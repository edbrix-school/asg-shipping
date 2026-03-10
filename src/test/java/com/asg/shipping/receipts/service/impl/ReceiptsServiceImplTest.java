package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.entity.ArShReceiptHdr;
import com.asg.shipping.receipts.enums.ButtonType;
import com.asg.shipping.receipts.repository.*;
import com.asg.shipping.receipts.util.ReceiptsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiptsServiceImplTest {

    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private ReceiptHdrRepository hdrRepository;
    @Mock
    private ArShReceiptContainerDtlRepository containerRepository;
    @Mock
    private ArShReceiptChargesDtlRepository chargesRepository;
    @Mock
    private ArShReceiptPymtDetailsRepository paymentRepository;
    @Mock
    private ReceiptsMapper mapper;
    @Mock
    private ShippingReceiptValidationService validationService;

    @Mock
    private ShipReceiptProcRepository procRepository;
    @Mock
    private ReceiptAutoPopulateRepository autoPopulateRepository;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ReceiptsServiceImpl receiptsService;

    private ArShReceiptHdr hdr;
    private ReceiptsBlDetailsDto detailsDto;
    private ReceiptsCreateDto createDto;

    @BeforeEach
    void setUp() {
        hdr = new ArShReceiptHdr();
        hdr.setTransactionPoid(1L);
        hdr.setDocRef("RCP-001");
        hdr.setCompanyPoid(100L);
        hdr.setGroupPoid(1L);

        detailsDto = ReceiptsBlDetailsDto.builder()
                .docRef("RCP-001")
                .build();

        createDto = ReceiptsCreateDto.builder()
                .docRef("RCP-001")
                .blPoid(1001L)
                .companyPoid(100L)
                .build();
    }

    @Test
    void getReceipt_Success() {
        when(hdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
        when(containerRepository.findByIdTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(chargesRepository.findByIdTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByIdTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(mapper.mapBlDetailsEntityToDto(any(), any(), any(), any())).thenReturn(detailsDto);

        ReceiptsBlDetailsDto result = receiptsService.getReceipt(1L);

        assertNotNull(result);
        assertEquals("RCP-001", result.getDocRef());
        verify(hdrRepository).findById(1L);
    }

    @Test
    void getReceipt_NotFound() {
        when(hdrRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> receiptsService.getReceipt(1L));
    }

    @Test
    void deleteReceipt_Success() {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        when(hdrRepository.findById(1L)).thenReturn(Optional.of(hdr));

        receiptsService.deleteReceipt(1L, deleteReason);

        verify(documentDeleteService).deleteDocument(eq(1L), eq("AR_SH_RECEIPT_HDR"), eq("TRANSACTION_POID"),
                eq(deleteReason), any());
    }

    @Test
    void list_Success() {

        FilterRequestDto filterRequest = new FilterRequestDto(
                "AND",
                "N",
                Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawSearchResult = new RawSearchResult(Collections.emptyList(), Map.of(), 1L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getDocumentId)
                    .thenReturn("DOC-001");
            when(documentSearchService.resolveOperator(any())).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
            when(documentSearchService.resolveFilters(any())).thenReturn(Collections.emptyList());
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(),
                    anyString())).thenReturn(rawSearchResult);
            Map<String, Object> result = receiptsService.list(filterRequest, pageable);
            assertNotNull(result);
            verify(documentSearchService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(),
                    anyString());
        }
    }

    @Test
    void autoPopulateFields_Success() {
        ReceiptBlAutoPopulateDto blDto = new ReceiptBlAutoPopulateDto();
        when(procRepository.autoPopulateFields(1001L)).thenReturn(blDto);
        when(autoPopulateRepository.findAvailableContainersForBl(1001L, 1L)).thenReturn(Collections.emptyList());
        when(autoPopulateRepository.findAvailableChargesForBl(1001L, 1L)).thenReturn(Collections.emptyList());

        ReceiptAutoPopulateDto result = receiptsService.autoPopulateFields(1001L, 1L);

        assertNotNull(result);
        assertEquals(blDto, result.getBlDetails());
    }

    @Test
    void updateReceipt_AllActions_Success() {
        ReceiptsUpdateDto updateDto = ReceiptsUpdateDto.builder()
                .container(java.util.Arrays.asList(
                        ReceiptContainerDto.builder().actionType("ISCREATED").containerNo("NEW").build(),
                        ReceiptContainerDto.builder().actionType("ISUPDATED").detRowId(1L).build(),
                        ReceiptContainerDto.builder().actionType("ISDELETED").detRowId(2L).build()))
                .charges(java.util.Arrays.asList(
                        ReceiptCharges.builder().actionType("ISCREATED").chargePoid(10L).build(),
                        ReceiptCharges.builder().actionType("ISUPDATED").detRowId(1L).build(),
                        ReceiptCharges.builder().actionType("ISDELETED").detRowId(2L).build()))
                .paymentDetail(java.util.Arrays.asList(
                        ReceiptPaymentDetailDto.builder().actionType("ISCREATED").amount(BigDecimal.ONE).build(),
                        ReceiptPaymentDetailDto.builder().actionType("ISUPDATED").detRowId(1L).build(),
                        ReceiptPaymentDetailDto.builder().actionType("ISDELETED").detRowId(2L).build()))
                .build();

        com.asg.shipping.receipts.entity.ArShReceiptContainerDtl contEntity = new com.asg.shipping.receipts.entity.ArShReceiptContainerDtl();
        contEntity.setId(new com.asg.shipping.receipts.entity.TransactionDtlId(1L, 100L));

        com.asg.shipping.receipts.entity.ArShReceiptChargesDtl chargesEntity = new com.asg.shipping.receipts.entity.ArShReceiptChargesDtl();
        chargesEntity.setId(new com.asg.shipping.receipts.entity.TransactionDtlId(1L, 101L));

        com.asg.shipping.receipts.entity.ArShReceiptPymtDetails pymtEntity = new com.asg.shipping.receipts.entity.ArShReceiptPymtDetails();
        pymtEntity.setId(new com.asg.shipping.receipts.entity.TransactionDtlId(1L, 102L));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(UserContext::getUserName).thenReturn("testuser");

            when(hdrRepository.findById(anyLong())).thenReturn(Optional.of(hdr));
            when(mapper.mapBlDetailsDtoToEntity(any())).thenReturn(hdr);
            when(hdrRepository.save(any())).thenReturn(hdr);

            // Mock mapper for creations
            when(mapper.mapContainerDtoToEntity(any(), anyLong(), anyLong())).thenReturn(contEntity);
            when(mapper.mapChargesDtoToEntity(any(), anyLong(), anyLong())).thenReturn(chargesEntity);
            when(mapper.mapPaymentDtoToEntity(any(), anyLong(), anyLong())).thenReturn(pymtEntity);

            // Mock findById for updates
            when(containerRepository.findById(any())).thenReturn(Optional.of(contEntity));
            when(chargesRepository.findById(any())).thenReturn(Optional.of(chargesEntity));
            when(paymentRepository.findById(any())).thenReturn(Optional.of(pymtEntity));

            // For getReceipt call at the end
            when(containerRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(chargesRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(paymentRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(mapper.mapBlDetailsEntityToDto(any(), any(), any(), any())).thenReturn(detailsDto);

            receiptsService.updateReceipt(1L, updateDto);

            verify(validationService).validateReceiptUpdate(eq(updateDto), any());
            verify(hdrRepository).save(any());
            verify(procRepository).afterSave(anyLong(), anyLong(), anyLong(), anyLong(), eq("UPDATE"), any());

            verify(containerRepository).save(any());
            verify(chargesRepository).save(any());
            verify(paymentRepository).save(any());

        }
    }

    @Test
    void calculateDemurrage_Success() {
        ReceiptCalculateDemurrageRequestDto requestDto = ReceiptCalculateDemurrageRequestDto.builder()
                .blPoid(1001L)
                .containerNo("CONT123")
                .containerIsoType("20")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(5))
                .build();

        TaxConfig taxConfig = TaxConfig.builder()
                .taxApplicable("Y")
                .percentage(new java.math.BigDecimal("10"))
                .taxPoid(1L)
                .build();

        ChargeDto chargeDto = new ChargeDto();
        chargeDto.setChargeApplicable("PERBL");
        chargeDto.setAmountOther(new java.math.BigDecimal("50"));
        chargeDto.setTaxApplicable("Y");
        chargeDto.setTaxPercentage(new java.math.BigDecimal("5"));
        chargeDto.setChargeTypeApplicable("LATECOLLECTION");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            when(autoPopulateRepository.findLinePoidByBlPoid(1001L)).thenReturn(new java.math.BigDecimal("50"));
            when(procRepository.calculateDemurrageAmount(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new java.math.BigDecimal("500"));
            when(procRepository.getDemurrageTaxInfo(anyLong())).thenReturn(taxConfig);
            when(procRepository.getContainerSize(anyString())).thenReturn("20");
            when(procRepository.getCombinedCharges(anyLong(), anyLong()))
                    .thenReturn(Collections.singletonList(chargeDto));

            ReceiptCalculateDemurrageResponseDto result = receiptsService.calculateDemurrage(requestDto);

            assertNotNull(result);
            assertEquals(new java.math.BigDecimal("500"), result.getDemurrageAmount());
            assertEquals(new java.math.BigDecimal("50.00"), result.getDemurrageTaxAmount());
            assertEquals(new java.math.BigDecimal("602.50"), result.getTotalAmount());
        }
    }

    @Test
    void calculateDemurrage_InvalidDates() {
        ReceiptCalculateDemurrageRequestDto requestDto = ReceiptCalculateDemurrageRequestDto.builder()
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().minusDays(1))
                .build();

        assertThrows(com.asg.common.lib.exception.ValidationException.class,
                () -> receiptsService.calculateDemurrage(requestDto));
    }

    @Test
    void receiptAndInvoicePrint_Success() throws Exception {
        when(printService.buildBaseParams(anyLong(), anyString())).thenReturn(new java.util.HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(net.sf.jasperreports.engine.JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        byte[] result = receiptsService.receiptAndInvoicePrint(1L, 1001L, ButtonType.Receipts);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void calculateDemurrage_NoTaxInfo() {
        ReceiptCalculateDemurrageRequestDto requestDto = ReceiptCalculateDemurrageRequestDto.builder()
                .blPoid(1001L)
                .containerNo("CONT123")
                .containerIsoType("20")
                .fromDate(LocalDateTime.now())
                .toDate(LocalDateTime.now().plusDays(5))
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            when(autoPopulateRepository.findLinePoidByBlPoid(1001L)).thenReturn(new java.math.BigDecimal("50"));
            when(procRepository.calculateDemurrageAmount(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new java.math.BigDecimal("500"));
            when(procRepository.getDemurrageTaxInfo(anyLong())).thenReturn(null);
            when(procRepository.getContainerSize(anyString())).thenReturn("20");
            when(procRepository.getCombinedCharges(anyLong(), anyLong())).thenReturn(null);

            ReceiptCalculateDemurrageResponseDto result = receiptsService.calculateDemurrage(requestDto);

            assertNotNull(result);
            assertEquals(java.math.BigDecimal.ZERO, result.getDemurrageTaxAmount());
            assertEquals(new java.math.BigDecimal("500"), result.getTotalAmount());
        }
    }

    @Test
    void updateReceipt_ActionTypes() {
        ReceiptsUpdateDto updateDto = ReceiptsUpdateDto.builder()
                .container(java.util.Arrays.asList(
                        ReceiptContainerDto.builder().actionType("CREATED").containerNo("C1").build(),
                        ReceiptContainerDto.builder().actionType("UPDATED").detRowId(1L).build(),
                        ReceiptContainerDto.builder().actionType("DELETED").detRowId(2L).build(),
                        ReceiptContainerDto.builder().actionType("").build() // NOCHANGES
                ))
                .build();

        com.asg.shipping.receipts.entity.ArShReceiptContainerDtl contEntity = new com.asg.shipping.receipts.entity.ArShReceiptContainerDtl();
        contEntity.setId(new com.asg.shipping.receipts.entity.TransactionDtlId(1L, 100L));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(UserContext::getUserName).thenReturn("testuser");

            when(hdrRepository.findById(anyLong())).thenReturn(Optional.of(hdr));
            when(mapper.mapBlDetailsDtoToEntity(any())).thenReturn(hdr);
            when(mapper.mapContainerDtoToEntity(any(), anyLong(), anyLong())).thenReturn(contEntity);
            when(containerRepository.findById(any())).thenReturn(Optional.of(contEntity));

            // For getReceipt
            when(containerRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(chargesRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(paymentRepository.findByIdTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
            when(mapper.mapBlDetailsEntityToDto(any(), any(), any(), any())).thenReturn(detailsDto);

            receiptsService.updateReceipt(1L, updateDto);

            verify(containerRepository, times(1)).save(any()); // One CREATED

        }
    }

    @Test
    void updateReceipt_ResourceNotFound() {
        ReceiptsUpdateDto updateDto = ReceiptsUpdateDto.builder()
                .container(Collections.singletonList(
                        ReceiptContainerDto.builder().actionType("UPDATED").detRowId(999L).build()))
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(hdrRepository.findById(anyLong())).thenReturn(Optional.of(hdr));
            when(mapper.mapBlDetailsDtoToEntity(any())).thenReturn(hdr);
            when(containerRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> receiptsService.updateReceipt(1L, updateDto));
        }
    }
}
