package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorCreateDTO;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorBlAutoPopulateDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorBlAutoPopulateRequest;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorUpdateDTO;
import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintHdr;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintChargeDtlRepository;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintContainerDtlRepository;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintHdrRepository;
import com.asg.shipping.shippingmanifestcorrector.util.ManifestCorrectorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.security.util.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManifestCorrectorServiceImplTest {

    @Mock
    private ShipBlReprintHdrRepository hdrRepository;
    @Mock
    private ShipBlReprintChargeDtlRepository chargeDtlRepository;
    @Mock
    private ShipBlReprintContainerDtlRepository containerDtlRepository;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private LovDataService lovService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ManifestCorrectorMapper mapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ManifestCorrectorServiceImpl service;

    private ShipBlReprintHdr testEntity;
    private ManifestCorrectorCreateDTO createDTO;
    private ManifestCorrectorUpdateDTO updateDTO;
    private ManifestCorrectorDto responseDTO;

    @BeforeEach
    void setup() {
        testEntity = new ShipBlReprintHdr();
        testEntity.setTransactionPoid(1L);
        testEntity.setDeleted("N");
        testEntity.setBlNumber("12345");

        createDTO = new ManifestCorrectorCreateDTO();
        createDTO.setBlNumber("12345");
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlReprint("Y");

        updateDTO = new ManifestCorrectorUpdateDTO();
        updateDTO.setBlNumber("12345");

        responseDTO = new ManifestCorrectorDto();
        responseDTO.setTransactionPoid(1L);
    }

    @Test
    void searchManifestCorrector_Success() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "TEST");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "MC001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null))
                .thenReturn(List.of(filter));
        when(documentSearchService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result = service.searchManifestCorrector("100-143", filterRequest, null, null, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getManifestCorrectorById_Success() {
        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(lovService.getDetailsByPoidAndLovName(12345L, "SHIP_BL_REPRINT"))
                .thenReturn(new LovGetListDto(12345L, "240988", "240988", 12345L, "240988", null, null));

        responseDTO.setBlNumber("12345");

        ManifestCorrectorDto result = service.getManifestCorrectorById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        assertNotNull(result.getBlNumberDet());
    }

    @Test
    void getManifestCorrectorById_NotFound() {
        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getManifestCorrectorById(1L));
    }

    @Test
    @Disabled
    void createManifestCorrector_Success() {
        when(hdrRepository.saveAndFlush(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn("COMP");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);

        ManifestCorrectorDto result = service.createManifestCorrector(createDTO);

        assertNotNull(result);
        verify(hdrRepository).saveAndFlush(any());
    }

    @Test
    void updateManifestCorrector_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateManifestCorrector(1L, updateDTO));
    }

    @Test
    void deleteManifestCorrector_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(documentDeleteService.deleteDocument(any(), any(), any(), any(), any())).thenReturn(null);

        assertDoesNotThrow(() -> service.deleteManifestCorrector(1L, null));

        verify(hdrRepository).saveAndFlush(any());
        verifyNoInteractions(chargeDtlRepository);
        verifyNoInteractions(containerDtlRepository);
    }

    @Test
    void deleteManifestCorrector_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteManifestCorrector(1L, null));
    }

    @Test
    void updateManifestCorrector_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(hdrRepository.saveAndFlush(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);

        ManifestCorrectorDto result = service.updateManifestCorrector(1L, updateDTO);

        assertNotNull(result);
        verify(hdrRepository).saveAndFlush(any());
    }

    @Test
    void validateRefundAmounts_Success() {
        var request = com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest.builder()
                .blPoid(1L)
                .containerNumber("CONT123")
                .revPayable(java.math.BigDecimal.valueOf(100))
                .revIncome(java.math.BigDecimal.valueOf(50))
                .perQuantityAmount(java.math.BigDecimal.valueOf(200))
                .build();

        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.CallableStatementCallback<?> callback = invocation.getArgument(1);
                    return null;
                });

        var result = service.validateRefundAmounts(request);

        assertNotNull(result);
    }

    @Test
    void validateRefundAmounts_InvalidAmount() {
        var request = com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest.builder()
                .blPoid(1L)
                .containerNumber("CONT123")
                .revPayable(java.math.BigDecimal.valueOf(150))
                .revIncome(java.math.BigDecimal.valueOf(100))
                .perQuantityAmount(java.math.BigDecimal.valueOf(200))
                .build();

        var result = service.validateRefundAmounts(request);

        assertNotNull(result);
    }

    @Test
    void searchManifestCorrector_EmptyResult() {
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(),
                Map.of(),
                0L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null))
                .thenReturn(List.of());
        when(documentSearchService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result = service.searchManifestCorrector("100-143", filterRequest, null, null, pageable);

        assertNotNull(result);
    }

    @Test
    void getManifestCorrectorById_WithDetails() {
        var chargeDtl = new com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintChargeDtl();
        var containerDtl = new com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintContainerDtl();

        when(hdrRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(chargeDtl));
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(containerDtl));
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(mapper.mapChargeDtlListToDto(any())).thenReturn(List.of());
        when(mapper.mapContainerDtlListToDto(any())).thenReturn(List.of());

        ManifestCorrectorDto result = service.getManifestCorrectorById(1L);

        assertNotNull(result);
        verify(chargeDtlRepository).findByTransactionPoid(1L);
        verify(containerDtlRepository).findByTransactionPoid(1L);
    }

    @Test
    void autoPopulateFromBlBrowse_Success() throws Exception {
        LovGetListDto blDet = new LovGetListDto(12345L, "12345", "12345", 12345L, "12345", null, null);
        LovGetListDto consigneeDet = new LovGetListDto(101L, "C101", "Consignee", 101L, "Consignee", null, null);
        LovGetListDto notifyDet = new LovGetListDto(202L, "N202", "Notify", 202L, "Notify", null, null);
        LovGetListDto payableGlDet = new LovGetListDto(303L, "GL303", "Payable GL", 303L, "Payable GL", null, null);
        LovGetListDto incomeGlDet = new LovGetListDto(404L, "GL404", "Income GL", 404L, "Income GL", null, null);
        LovGetListDto deliveryDet = new LovGetListDto(505L, "P505", "Delivery Port", 505L, "Delivery Port", null, null);
        LovGetListDto receiptDet = new LovGetListDto(606L, "P606", "Receipt Port", 606L, "Receipt Port", null, null);
        LovGetListDto loadingDet = new LovGetListDto(707L, "P707", "Loading Port", 707L, "Loading Port", null, null);
        LovGetListDto dischargeDet = new LovGetListDto(808L, "P808", "Discharge Port", 808L, "Discharge Port", null, null);
        LovGetListDto voyageDet = new LovGetListDto(909L, "V909", "Voyage", 909L, "Voyage", null, null);
        LovGetListDto issueTypeDet = new LovGetListDto(1L, "ORIGINAL", "Original", 1L, "Original", null, null);
        LovGetListDto blTypeDet = new LovGetListDto(1L, "IMPORT", "Import", 1L, "Import", null, null);
        LovGetListDto holdReasonDet = new LovGetListDto(1L, "NONE", "None", 1L, "None", null, null);

        when(lovService.getDetailsByPoidsAndLovName(anyList(), eq("SHIP_BL_REPRINT")))
                .thenReturn(Map.of(12345L, blDet));
        when(lovService.getDetailsByPoidsAndLovName(anyList(), eq("ADDRESS_MASTER")))
                .thenReturn(Map.of(101L, consigneeDet, 202L, notifyDet));
        when(lovService.getDetailsByPoidsAndLovName(anyList(), eq("GL_MASTER_LEDGERS")))
                .thenReturn(Map.of(303L, payableGlDet, 404L, incomeGlDet));
        when(lovService.getDetailsByPoidsAndLovName(anyList(), eq("PORT_MASTER")))
                .thenReturn(Map.of(505L, deliveryDet, 606L, receiptDet, 707L, loadingDet, 808L, dischargeDet));
        when(lovService.getDetailsByPoidsAndLovName(anyList(), eq("VESSAL_VOYAGE")))
                .thenReturn(Map.of(909L, voyageDet));
        when(lovService.getDetailsByCodesAndLovName(anyList(), eq("BL_ISSUE_TYPE")))
                .thenReturn(Map.of("ORIGINAL", issueTypeDet));
        when(lovService.getDetailsByCodesAndLovName(anyList(), eq("BL_TYPE")))
                .thenReturn(Map.of("IMPORT", blTypeDet));
        when(lovService.getDetailsByCodesAndLovName(anyList(), eq("SHIP_DO_ANOTICE_HOLD")))
                .thenReturn(Map.of("NONE", holdReasonDet));

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(10L);
            userContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            userContext.when(UserContext::getUserPoid).thenReturn(30L);
            userContext.when(UserContext::getDocumentId).thenReturn("100-143");

            when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                    .thenAnswer(invocation -> {
                        org.springframework.jdbc.core.CallableStatementCallback<?> callback = invocation.getArgument(1);
                        CallableStatement cs = mock(CallableStatement.class);
                        ResultSet rs = mock(ResultSet.class);

                        when(cs.getObject(8)).thenReturn(rs);
                        when(rs.next()).thenReturn(true);
                        when(rs.getObject("CONSIGNEE_POID")).thenReturn(101L);
                        when(rs.getString("ISSUE_TYPE")).thenReturn("ORIGINAL");
                        when(rs.getObject("NOTIFY_POID")).thenReturn(202L);
                        when(rs.getString("SHIPPER_EDI_NAME")).thenReturn("TEST SHIPPER");
                        when(rs.getString("BL_TYPE")).thenReturn("IMPORT");
                        when(rs.getString("HOLD_CAN_DO")).thenReturn("N");
                        when(rs.getString("HOLD_REASON")).thenReturn("NONE");
                        when(rs.getObject("PAYABLE_GL_POID")).thenReturn(303L);
                        when(rs.getObject("INCOME_GL_POID")).thenReturn(404L);
                        when(rs.getObject("PLACE_OF_DELIEVERY_POID")).thenReturn(505L);
                        when(rs.getObject("PLACE_OF_RECIEPT_POID")).thenReturn(606L);
                        when(rs.getString("BL_PLACE_RECEIPT")).thenReturn("RECEIPT");
                        when(rs.getString("BL_PLACE_LOAD")).thenReturn("LOAD");
                        when(rs.getString("BL_FINAL_DESTINATION")).thenReturn("DEST");
                        when(rs.getString("BL_PLACE_DISCHARE_DESC")).thenReturn("DISCHARGE");
                        when(rs.getObject("PORT_OF_LOADING_POID")).thenReturn(707L);
                        when(rs.getObject("PORT_OF_DISCHARGE_POID")).thenReturn(808L);
                        when(rs.getObject("VOYAGE_TRANSACTION_POID")).thenReturn(909L);

                        return callback.doInCallableStatement(cs);
                    });

            ManifestCorrectorBlAutoPopulateDto result = service.autoPopulateFromBlBrowse(
                    "12345",
                    ManifestCorrectorBlAutoPopulateRequest.builder().transactionPoid(1L).build());

            assertNotNull(result);
            assertEquals(12345L, result.getBlPoid());
            assertEquals(101L, result.getConsigneePoid());
            assertEquals("ORIGINAL", result.getIssueType());
            assertEquals(909L, result.getVoyageTransactionPoid());
            assertNotNull(result.getBlDet());
            assertNotNull(result.getConsigneeDet());
            assertNotNull(result.getIssueTypeDet());
            assertNotNull(result.getVoyageTransactionDet());
        }
    }
}
