package com.asg.shipping.shipcommisiontransferTest.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shipcommisiontransfer.dto.*;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionDtlRepository;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionHdrRepository;
import com.asg.shipping.shipcommisiontransfer.service.ShipCommissionTransferServiceImpl;
import com.asg.shipping.shipcommisiontransfer.util.ShipCommissionTransferMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipCommissionTransferServiceImplTest {

    @Mock
    private ShipBlCommissionHdrRepository headerRepository;
    @Mock
    private ShipBlCommissionDtlRepository detailRepository;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private ShipCommissionTransferMapper mapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EntityManager entityManager;
    @Mock
    private StoredProcedureQuery storedProcedureQuery;
    @Mock
    private com.asg.shipping.common.service.LovService lovService;

    @InjectMocks
    private ShipCommissionTransferServiceImpl service;

    private ShipBlCommissionHdr hdrEntity;
    private ShipCommissionTransferDto dto;
    private ShipCommissionTransferCreateDTO createDTO;
    private ShipCommissionTransferUpdateDTO updateDTO;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        hdrEntity = ShipBlCommissionHdr.builder()
                .transactionPoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .docRef("COM-001")
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        dto = ShipCommissionTransferDto.builder()
                .transactionPoid(1L)
                .docRef("COM-001")
                .build();

        createDTO = ShipCommissionTransferCreateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .commissionDetails(List.of())
                .build();

        updateDTO = ShipCommissionTransferUpdateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .commissionDetails(List.of())
                .build();
    }

    @Test
    void testSearchShipCommissionTransfer() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "COM-001");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "COM-001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveDateFilters(eq(filterRequest), eq("TRANSACTION_DATE"), isNull(), isNull())).thenReturn(List.of(filter));
        when(documentService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), eq("DOC_REF"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.searchShipCommissionTransfer("DOC-1", filterRequest, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getShipCommissionTransfer(1L));
        }
    }

    @Test
    void testGetShipCommissionTransfer_Deleted() {
        hdrEntity.setDeleted("Y");
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::isLogEnabled).thenReturn(true);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(mapper.mapToDto(hdrEntity)).thenReturn(dto);
            when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

            ShipCommissionTransferDto result = service.getShipCommissionTransfer(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
        }
    }

    @Test
    void testGetShipCommissionTransfer_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::isLogEnabled).thenReturn(true);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(mapper.mapToDto(hdrEntity)).thenReturn(dto);
            when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

            ShipCommissionTransferDto result = service.getShipCommissionTransfer(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            verifyNoInteractions(loggingService);
        }
    }

    @Test
    void testCreateShipCommissionTransfer_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(1L);

            when(headerRepository.save(any(ShipBlCommissionHdr.class))).thenReturn(hdrEntity);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn("COM-001");
            
            // Mock the service call to getShipCommissionTransfer for the return
            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(mapper.mapToDto(hdrEntity)).thenReturn(dto);
            when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

            ShipCommissionTransferDto result = service.createShipCommissionTransfer(createDTO);

            assertNotNull(result);
            verify(headerRepository).save(any(ShipBlCommissionHdr.class));
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("DOC-001"), eq("1"));
        }
    }

    @Test
    void testUpdateShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateShipCommissionTransfer(1L, updateDTO));
        }
    }

    @Test
    void testUpdateShipCommissionTransfer_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-001");
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::isLogEnabled).thenReturn(true);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(headerRepository.save(any(ShipBlCommissionHdr.class))).thenReturn(hdrEntity);
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(mapper.mapToDto(hdrEntity)).thenReturn(dto);
            when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

            ShipCommissionTransferDto result = service.updateShipCommissionTransfer(1L, updateDTO);

            assertNotNull(result);
            verify(headerRepository).save(any(ShipBlCommissionHdr.class));
            verify(loggingService).logChanges(any(), any(), eq(ShipBlCommissionHdr.class), eq("DOC-001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void testDeleteShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteShipCommissionTransfer(1L, null));
        }
    }

    @Test
    void testDeleteShipCommissionTransfer_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));

            service.deleteShipCommissionTransfer(1L, null);

            verify(documentDeleteService).deleteDocument(
                    eq(1L),
                    eq("SHIP_BL_COMMISSION_HDR"),
                    eq("TRANSACTION_POID"),
                    isNull(),
                    eq(hdrEntity.getTransactionDate())
            );
        }
    }

    @Test
    void testCalculateCommission_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(1L);

            hdrEntity.setVoyageTransactionPoid(100L);
            hdrEntity.setCurrencyExchange(BigDecimal.ONE);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());

            CalculateCommissionRequestDTO request = CalculateCommissionRequestDTO.builder()
                    .recalculateAll(true)
                    .build();

            Map<String, Object> result = service.calculateCommission(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.get("transactionPoid"));
            assertEquals(0, result.get("calculatedDetails"));
            assertEquals(BigDecimal.ZERO, result.get("totalCommission"));
        }
    }

    @Test
    void testLoadFromVoyage_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class);
             var mockedHelperUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedHelperUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            hdrEntity.setDocRef("COM-001");
            hdrEntity.setVoyageTransactionPoid(100L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class))).thenReturn("Success");

            Map<String, Object> result = service.loadFromVoyage(1L);

            assertNotNull(result);
            assertEquals(1L, result.get("transactionPoid"));
            assertEquals(0, result.get("loadedDetails"));
        }
    }

    @Test
    void testLoadFromVoyage_ValidationError_NoDocRef() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            hdrEntity.setDocRef(null);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));

            assertThrows(com.asg.common.lib.exception.ValidationException.class, 
                () -> service.loadFromVoyage(1L));
        }
    }

    @Test
    void testLoadFromVoyage_ValidationError_NoVoyageTransactionPoid() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            hdrEntity.setDocRef("COM-001");
            hdrEntity.setVoyageTransactionPoid(null);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));

            assertThrows(com.asg.common.lib.exception.ValidationException.class, 
                () -> service.loadFromVoyage(1L));
        }
    }

    @Test
    void testInsertPdaCommission_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class);
             var mockedHelperUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {

            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);
            mockedHelperUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            hdrEntity.setFdaTransactionPoid(200L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));
            when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                    .thenReturn("SUCESS commission posted to FDA, ");

            Map<String, Object> result = service.insertPdaCommission(1L);

            assertNotNull(result);
            assertEquals(1L, result.get("transactionPoid"));
            assertTrue(result.get("message").toString().startsWith("Records imported..."));
        }
    }

    @Test
    void testInsertPdaCommission_ValidationError_NoFdaTransactionPoid() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            hdrEntity.setFdaTransactionPoid(null);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));

            Map<String, Object> result = service.insertPdaCommission(1L);

            assertNotNull(result);
            assertEquals(1L, result.get("transactionPoid"));
            assertEquals("Select Fda number ...", result.get("message"));
        }
    }

    @Test
    void testGetCommissionPending_Success() {

        CommissionPendingRequestDTO request = new CommissionPendingRequestDTO();
        request.setExchangeRate(1.5);
        request.setBlPoid(50L);
        request.setFrtBuyActual(200.0);
        request.setShortLegSelected("Y");
        request.setRecordType("ALL");

        // 18 columns matching indices [0]..[17] used by the service mapper
        Object[] row1 = {"TARGET_DOC_ID=100-102,DOC_KEY_POID=604763", 604763L, "BHD", 1, "IMPORT", 1, 0, "APPROVED", 121.93, 16, 16, 184, 184, 0, 16, 20.32, 0, "P"};
        Object[] row2 = {"TARGET_DOC_ID=100-102,DOC_KEY_POID=604764", 604764L, "USD", 1, "EXPORT", 2, 1, "PENDING",  200.00, 10, 10, 100, 100, 5, 10, 15.00, 0, "N"};
        List<Object[]> expectedRows = List.of(row1, row2);

        when(entityManager.createStoredProcedureQuery("PROC_SHIP_COMMISSION_RECORD_FETCH"))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getResultList()).thenReturn(expectedRows);
        when(lovService.getLovItemByPoid(anyLong(), eq("ALLBLNUMBER"), any(), any(), any()))
                .thenReturn(new com.asg.shipping.common.dto.LovItem(604763L, "BL-001", "BL Description", null, null, null));

        List<CommissionPendingResponseDTO> result = service.getCommissionPending(100L, request);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(604763L, result.get(0).getBlPoid());
        assertNotNull(result.get(0).getBlDet());

        verify(storedProcedureQuery).setParameter("P_LOGIN_GROUP_POID", null);
        verify(storedProcedureQuery).setParameter("P_COMPANY_POID", null);
        verify(storedProcedureQuery).setParameter("P_LOGIN_USER_POID", null);
        verify(storedProcedureQuery).setParameter("P_DOC_ID", null);
        verify(storedProcedureQuery).setParameter("P_BL_POID", 50L);
        verify(storedProcedureQuery).setParameter("P_VOYAGE_TRANSACTION_POID", 100L);
        verify(storedProcedureQuery).setParameter("P_EXCHANGE", 1.5d);
        verify(storedProcedureQuery).setParameter("P_RECORD_TYPE", "ALL");
        verify(storedProcedureQuery).setParameter("P_FRT_BUY_ACTUAL", 200.0d);
        verify(storedProcedureQuery).setParameter("P_SHORT_LEG_SELECTED", "Y");
        verify(storedProcedureQuery).execute();
    }

    @Test
    void testGetCommissionPending_WithNullRequestFields_UsesDefaults() {
        CommissionPendingRequestDTO request = new CommissionPendingRequestDTO();

        when(entityManager.createStoredProcedureQuery("PROC_SHIP_COMMISSION_RECORD_FETCH"))
                .thenReturn(storedProcedureQuery);

        when(storedProcedureQuery.getResultList())
                .thenReturn(List.of());

        List<CommissionPendingResponseDTO> result = service.getCommissionPending(100L, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(storedProcedureQuery).setParameter("P_LOGIN_GROUP_POID", null);
        verify(storedProcedureQuery).setParameter("P_COMPANY_POID", null);
        verify(storedProcedureQuery).setParameter("P_LOGIN_USER_POID", null);
        verify(storedProcedureQuery).setParameter("P_DOC_ID", null);

        verify(storedProcedureQuery).setParameter("P_BL_POID", 0L);
        verify(storedProcedureQuery).setParameter("P_VOYAGE_TRANSACTION_POID", 100L);
        verify(storedProcedureQuery).setParameter("P_EXCHANGE", 1.0d);
        verify(storedProcedureQuery).setParameter("P_RECORD_TYPE", "ALL");
        verify(storedProcedureQuery).setParameter("P_FRT_BUY_ACTUAL", 0.0d);
        verify(storedProcedureQuery).setParameter("P_SHORT_LEG_SELECTED", "N");
    }

    @Test
    void testGetCommissionPending_ReturnsEmptyList() {
        CommissionPendingRequestDTO request = new CommissionPendingRequestDTO();
        request.setExchangeRate(1.0);
        request.setBlPoid(0L);
        request.setFrtBuyActual(0.0);
        request.setShortLegSelected("N");
        request.setRecordType("ALL");

        when(entityManager.createStoredProcedureQuery("PROC_SHIP_COMMISSION_RECORD_FETCH")).thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getResultList()).thenReturn(List.of());

        List<CommissionPendingResponseDTO> result = service.getCommissionPending( 100L, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(storedProcedureQuery).execute();
    }
}