package com.asg.shipping.demurrage;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferCreateDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferDto;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionTransferBillDetailDto;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.ProcessDataRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.UpdateFreeDaysRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferHdr;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDetnTransferHdrRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDetnTransferDtlRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDtnTransferBillDtlRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.service.DemurrageDetentionPayableTransferServiceImpl;
import com.asg.shipping.demurragedetentionpayabletransfer.util.DemurrageDetentionPayableTransferMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemurrageDetentionPayableTransferServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private ShipDemDetnTransferHdrRepository headerRepository;
    @Mock
    private ShipDemDetnTransferDtlRepository transferDtlRepository;
    @Mock
    private ShipDemDtnTransferBillDtlRepository billDtlRepository;
    @Mock
    private LovDataService lovService;
    @Mock
    private DemurrageDetentionPayableTransferMapper mapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DemurrageDetentionPayableTransferServiceImpl service;

    private DemurrageDetentionPayableTransferCreateDTO request;
    private ShipDemDetnTransferHdr hdrEntity;
    private DemurrageDetentionPayableTransferDto response;

    @BeforeEach
    void setup() {
        request = new DemurrageDetentionPayableTransferCreateDTO();
        request.setLinePoid(1001L);
        request.setTransactionDate(LocalDate.now());
        request.setBlType("IMPORT");
        request.setEmptyFromDate(LocalDate.now());
        request.setEmptyToDate(LocalDate.now().plusDays(30));
        request.setPayableGlPoid(12345L);
        request.setIncomeGlPoid(67890L);

        hdrEntity = new ShipDemDetnTransferHdr();
        hdrEntity.setTransactionPoid(1L);
        hdrEntity.setGroupPoid(100L);
        hdrEntity.setDeleted("N");

        response = new DemurrageDetentionPayableTransferDto();
        response.setTransactionPoid(1L);
    }

    @Test
    void testSearchRecords() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "DEM");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "DEM001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveDateFilters(eq(filterRequest), eq("TRANSACTION_DATE"), isNull(), isNull())).thenReturn(List.of(filter));
        when(documentService.search(
                anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()
        )).thenReturn(raw);

        Map<String, Object> result = service.searchDemurrageDetentionPayableTransfer("DOC-1", filterRequest, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetById_Success() {
        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                .thenReturn(Optional.of(hdrEntity));
        when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.mapToDto(any()))
                .thenReturn(response);

        // Mock UserContext
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            DemurrageDetentionPayableTransferDto result = service.getDemurrageDetentionPayableTransfer(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
        }
    }

    @Test
    void testGetById_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getDemurrageDetentionPayableTransfer(1L));
        }
    }

    @Test
    void testCreate_Success() {
        // GAP-5: billDetails required before save
        DemurrageDetentionTransferBillDetailDto billDetailDto = new DemurrageDetentionTransferBillDetailDto();
        billDetailDto.setGlPoid(1L);
        request.setBillDetails(List.of(billDetailDto));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-1");

            lenient().when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                    .thenReturn("12345");
            lenient().when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any()))
                    .thenReturn("67890");
            when(headerRepository.save(any())).thenReturn(hdrEntity);
            when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(mapper.mapToDto(any())).thenReturn(response);
            lenient().when(transferDtlRepository.getMaxDetRowId(any())).thenReturn(null);
            lenient().when(billDtlRepository.getMaxDetRowId(any())).thenReturn(null);
            // mapper and save mocks for bill detail persistence
            lenient().when(mapper.mapBillDtlFromDto(any(), any())).thenReturn(createMockBillDetail());
            lenient().when(billDtlRepository.save(any())).thenReturn(createMockBillDetail());

            DemurrageDetentionPayableTransferDto result =
                    service.createDemurrageDetentionPayableTransfer(request, 1L, 100L);

            assertNotNull(result);
            verify(headerRepository).save(any());
        }
    }

    @Test
    void testUpdate_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateDemurrageDetentionPayableTransfer(1L, null, 1L, 100L));
        }
    }

    @Test
    void testDelete_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));
            when(documentDeleteService.deleteDocument(any(), any(), any(), any(), any())).thenReturn(null);

            assertDoesNotThrow(() -> service.deleteDemurrageDetentionPayableTransfer(1L, 1L, 100L, null));

            verify(headerRepository).save(argThat(e -> "Y".equals(e.getDeleted())));
        }
    }

    @Test
    void testDelete_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteDemurrageDetentionPayableTransfer(1L, 1L, 100L, null));
        }
    }

    @Test
    void testProcessData_Success() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);
        processRequest.setBlType("IMPORT");
        processRequest.setEmptyFromDate(LocalDate.now());
        processRequest.setEmptyToDate(LocalDate.now().plusDays(30));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));
            lenient().when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());
            doNothing().when(transferDtlRepository).deleteByTransactionPoid(any());
            when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(mapper.mapToDto(any()))
                    .thenReturn(response);

            DemurrageDetentionPayableTransferDto result = service.processData(1L, processRequest);

            assertNotNull(result);
        }
    }

    @Test
    void testProcessData_NotFound() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.processData(1L, processRequest));
        }
    }

    @Test
    void testUpdateFreeDays_Success() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate containerUpdate =
                new UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate();
        containerUpdate.setDetRowId(1L);
        containerUpdate.setContainerNo("CONT001");
        containerUpdate.setMainfestTransactionPoid(1001L);
        containerUpdate.setExtraFreeDaysPrnpls(java.math.BigDecimal.valueOf(5));
        updateRequest.setContainerUpdates(List.of(containerUpdate));

        // Mock the JDBC template for stored procedure call
        lenient().when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> service.updatePrincipalDays(updateRequest));
    }

    @Test
    void testUpdateFreeDays_NotFound() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate containerUpdate =
                new UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate();
        // Missing required fields to trigger validation
        containerUpdate.setDetRowId(1L);
        updateRequest.setContainerUpdates(List.of(containerUpdate));
        
        assertThrows(com.asg.shipping.exceptions.ValidationException.class,
                () -> service.updatePrincipalDays(updateRequest));
    }

    @Test
    void testProcessDataBeforeCreate_Success() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);
        processRequest.setBlType("IMPORT");
        processRequest.setEmptyFromDate(LocalDate.now());
        processRequest.setEmptyToDate(LocalDate.now().plusDays(30));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of(Map.of("CONTAINER_NO", "CONT001")));

            Map<String, Object> result = service.processDataBeforeCreate(processRequest);

            assertNotNull(result);
            assertTrue(result.containsKey("containers"));
            assertTrue(result.containsKey("totalCount"));
        }
    }

    @Test
    void testLoadBillwiseDataBeforeCreate_Success() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        loadRequest.setBlType("IMPORT"); // Add missing BL Type
        
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer container = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer();
        loadRequest.setBlType("IMPORT");
        container.setMainfestTransactionPoid(1001L);
        container.setContainerNo("CONT001");
        container.setBlNumber("BL001");
        container.setIsSelect("Y"); // Add missing IsSelect field
        container.setTotalPayableAmount(java.math.BigDecimal.valueOf(1500));
        container.setTotalIncomeAmount(java.math.BigDecimal.valueOf(200));
        loadRequest.setSelectedContainers(List.of(container));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);

            // Mock billwise accounts query
            when(jdbcTemplate.queryForList(eq("SELECT * FROM VW_SHIP_BILLWISE_ACCOUNT_TRN WHERE GL_CODE = ? AND REMARKS LIKE ? AND COMPANY_POID = ?"), any(Object[].class)))
                    .thenReturn(List.of(Map.of(
                            "REMARKS", "Demurrage for BL001",
                            "BILL_REF", "BILL001",
                            "BALANCE", java.math.BigDecimal.valueOf(1000),
                            "GL_POID", 12345L
                    )));
            
            // Mock stored procedure call for default GL
            when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                    .thenReturn("12345");

            Map<String, Object> result = service.loadBillwiseDataBeforeCreate(loadRequest);

            assertNotNull(result);
            assertTrue(result.containsKey("billDetails"));
            assertTrue(result.containsKey("totalCount"));
            assertTrue(result.containsKey("blType"));
            assertTrue(result.containsKey("glCode"));
            assertEquals("IMPORT", result.get("blType"));
            assertEquals("LINE_DEM", result.get("glCode"));
        }
    }

    @Test
    void testLoadBillwiseDataBeforeCreate_PreservesBillMetadataOnMultipleRows() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest =
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        loadRequest.setBlType("IMPORT");

        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer container =
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer();
        container.setMainfestTransactionPoid(1001L);
        container.setContainerNo("CONT001");
        container.setBlNumber("BL001");
        container.setIsSelect("Y");
        container.setTotalPayableAmount(java.math.BigDecimal.valueOf(1500));
        container.setTotalIncomeAmount(java.math.BigDecimal.valueOf(200));
        loadRequest.setSelectedContainers(List.of(container));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);

            // Billwise query: no COMPANY_POID filter (matches legacy VwShipBillwiseAccountTrnView1)
            when(jdbcTemplate.queryForList(contains("VW_SHIP_BILLWISE_ACCOUNT_TRN"), any(Object[].class)))
                    .thenReturn(List.of(
                            Map.of(
                                    "REMARKS", "Demurrage for BL001",
                                    "BILL_REF", "BILL001",
                                    "BALANCE", java.math.BigDecimal.valueOf(1000),
                                    "GL_POID", 12345L,
                                    "GL_COMPANY_POID", 1L
                            ),
                            Map.of(
                                    "REMARKS", "Demurrage for BL001",
                                    "BILL_REF", "BILL001",
                                    "BALANCE", java.math.BigDecimal.valueOf(1000),
                                    "GL_POID", 12345L,
                                    "GL_COMPANY_POID", 1L
                            )
                    ));
            when(jdbcTemplate.queryForList(contains("VW_AR_SH_CONTAINER_DEMG_DTTN"), any(Object[].class)))
                    .thenReturn(List.of(
                            Map.of("CONTAINER_NO", "CONT001", "BL_NUMBER", "BL001", "DOC_REF", "DOC-1", "DM_CHARGE_AMT", java.math.BigDecimal.valueOf(120)),
                            Map.of("CONTAINER_NO", "CONT001", "BL_NUMBER", "BL001", "DOC_REF", "DOC-1", "DM_CHARGE_AMT", java.math.BigDecimal.valueOf(120))
                    ));

            Map<String, Object> result = service.loadBillwiseDataBeforeCreate(loadRequest);

            assertNotNull(result);
            List<?> billDetails = (List<?>) result.get("billDetails");
            assertEquals(2, billDetails.size());

            Map<?, ?> row1 = (Map<?, ?>) billDetails.get(0);
            Map<?, ?> row2 = (Map<?, ?>) billDetails.get(1);
            // Multi-row branch: billRefno comes from DOC_REF in the dynamic query, not BILL_REF
            assertEquals("DOC-1", row1.get("billRefno"));
            assertEquals("DOC-1", row2.get("billRefno"));
            assertEquals(12345L, row1.get("glPoid"));
            assertEquals(12345L, row2.get("glPoid"));
            assertNotNull(row1.get("billwiseBalance"));
            assertNotNull(row2.get("billwiseBalance"));
        }
    }

    @Test
    void testUpdatePrincipalDays_Success() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate containerUpdate =
                new UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate();
        containerUpdate.setMainfestTransactionPoid(1001L);
        containerUpdate.setContainerNo("CONT001");
        containerUpdate.setExtraFreeDaysPrnpls(java.math.BigDecimal.valueOf(5));
        updateRequest.setContainerUpdates(List.of(containerUpdate));

        assertDoesNotThrow(() -> service.updatePrincipalDays(updateRequest));
    }

    @Test
    void testGetAutoPopulatedGlAccounts_Success() {
        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                .thenReturn("12345");
        when(lovService.getDetailsByPoidAndLovName(any(), anyString()))
                .thenReturn(new com.asg.common.lib.dto.LovGetListDto());

        Map<String, Object> result = service.getAutoPopulatedGlAccounts(1001L, "IMPORT", 100L);

        assertNotNull(result);
        assertTrue(result.containsKey("payableGlPoid"));
        assertTrue(result.containsKey("incomeGlPoid"));
    }

    @Test
    void testGetGlAccountsDirectFromSp_Success() {
        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                .thenReturn("12345");

        Map<String, Object> result = service.getGlAccountsDirectFromSp(1001L, "IMPORT");

        assertNotNull(result);
        assertTrue(result.containsKey("payableGlPoid"));
    }

    @Test
    void testUpdate_Success() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferUpdateDTO updateDto = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferUpdateDTO();
        updateDto.setLinePoid(1001L);
        updateDto.setBlType("IMPORT");
        
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-1");

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));
            when(headerRepository.save(any()))
                    .thenReturn(hdrEntity);
            when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(mapper.mapToDto(any()))
                    .thenReturn(response);
            lenient().when(transferDtlRepository.getMaxDetRowId(any())).thenReturn(null);
            lenient().when(billDtlRepository.getMaxDetRowId(any())).thenReturn(null);
            DemurrageDetentionPayableTransferDto result = 
                    service.updateDemurrageDetentionPayableTransfer(1L, updateDto, 1L, 100L);

            assertNotNull(result);
            verify(headerRepository).save(any());
        }
    }

    @Test
    void testLoadBillwiseData_Success() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer container = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer();
        container.setDetRowId(1L);
        container.setMainfestTransactionPoid(1001L);
        container.setContainerNo("CONT001");
        container.setBlNumber("BL001");
        loadRequest.setSelectedContainers(List.of(container));

        // Set blType in the entity to avoid null validation error
        hdrEntity.setBlType("IMPORT");

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));
            doNothing().when(billDtlRepository).deleteByTransactionPoid(any());
            when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(mapper.mapToDto(any()))
                    .thenReturn(response);

            DemurrageDetentionPayableTransferDto result = service.loadBillwiseData(1L, loadRequest);

            assertNotNull(result);
        }
    }

    @Test
    void testLoadBillwiseData_NullBlType() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        
        // Entity has null blType - this should trigger our validation
        hdrEntity.setBlType(null);

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));

            com.asg.shipping.exceptions.ValidationException ex = assertThrows(
                    com.asg.shipping.exceptions.ValidationException.class,
                    () -> service.loadBillwiseData(1L, loadRequest));
            
            assertTrue(ex.getMessage().contains("BL Type is required for loading bill-wise data"));
        }
    }

    private com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDtnTransferBillDtl createMockBillDetail() {
        return com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDtnTransferBillDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .containerNo("CONT001")
                .glPoid(1L)
                .build();
    }

    private com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferDtl createMockTransferDetail() {
        return com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .containerNo("CONT001")
                .build();
    }

    // Validation and Edge Case Tests
    @Test
    void testCreate_InvalidBlType() {
        request.setBlType("INVALID");

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-1");

            assertThrows(com.asg.shipping.exceptions.ValidationException.class, 
                () -> service.createDemurrageDetentionPayableTransfer(request, 1L, 100L));
        }
    }

    @Test
    void testCreate_MissingLinePoid() {
        request.setLinePoid(null);

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-1");

            assertThrows(com.asg.shipping.exceptions.ValidationException.class, 
                () -> service.createDemurrageDetentionPayableTransfer(request, 1L, 100L));
        }
    }

    @Test
    void testCreate_InvalidDateRange() {
        request.setEmptyFromDate(LocalDate.now());
        request.setEmptyToDate(LocalDate.now().minusDays(1));
        // GAP-5: billDetails required before date check is reached
        DemurrageDetentionTransferBillDetailDto billDetailDto = new DemurrageDetentionTransferBillDetailDto();
        billDetailDto.setGlPoid(1L);
        request.setBillDetails(List.of(billDetailDto));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("DOC-1");

            // generateDocRef must succeed so date validation is actually reached
            lenient().when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any()))
                    .thenReturn("DOC-REF-001");

            // Mapper sets dates on the entity so date validation can fire
            doAnswer(invocation -> {
                ShipDemDetnTransferHdr entity = invocation.getArgument(1);
                entity.setEmptyFromDate(request.getEmptyFromDate());
                entity.setEmptyToDate(request.getEmptyToDate());
                return null;
            }).when(mapper).mapCreateDTOToEntity(any(), any(), any(), any());

            assertThrows(com.asg.shipping.exceptions.ValidationException.class,
                () -> service.createDemurrageDetentionPayableTransfer(request, 1L, 100L));
        }
    }

    @Test
    void testProcessDataBeforeCreate_MissingLinePoid() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setBlType("IMPORT");

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            assertThrows(com.asg.shipping.exceptions.ValidationException.class, 
                () -> service.processDataBeforeCreate(processRequest));
        }
    }

    @Test
    void testProcessDataBeforeCreate_MissingBlType() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            assertThrows(com.asg.shipping.exceptions.ValidationException.class, 
                () -> service.processDataBeforeCreate(processRequest));
        }
    }

    @Test
    void testUpdateFreeDays_NoBillDetails() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        updateRequest.setContainerUpdates(List.of());

        assertDoesNotThrow(() -> service.updatePrincipalDays(updateRequest));
    }

    @Test
    void testGetById_DeletedRecord() {
        hdrEntity.setDeleted("Y");

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                .thenReturn(Optional.of(hdrEntity));
            when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
            when(mapper.mapToDto(any())).thenReturn(response);

            DemurrageDetentionPayableTransferDto result = service.getDemurrageDetentionPayableTransfer(1L);
            assertNotNull(result);
        }
    }

    @Test
    void testProcessDataBeforeCreate_DatabaseError() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);
        processRequest.setBlType("IMPORT");

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Database error"));

            com.asg.shipping.exceptions.ValidationException ex = assertThrows(
                    com.asg.shipping.exceptions.ValidationException.class,
                    () -> service.processDataBeforeCreate(processRequest));
            assertTrue(ex.getMessage().contains("Failed to load available containers"));
        }
    }

    @Test
    void testLoadBillwiseDataBeforeCreate_DatabaseError() {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest = 
            new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        loadRequest.setBlType("IMPORT"); // Add missing BL Type
        
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer container = 
            new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer();
        container.setMainfestTransactionPoid(1001L);
        container.setContainerNo("CONT001");
        container.setBlNumber("BL001");
        container.setIsSelect("Y"); // Add missing IsSelect field
        container.setTotalPayableAmount(java.math.BigDecimal.valueOf(1500));
        container.setTotalIncomeAmount(java.math.BigDecimal.valueOf(200));
        loadRequest.setSelectedContainers(List.of(container));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);

            // Mock billwise accounts query to fail — queryBillwiseAccountView catches and returns empty list
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Database error"));

            var result = service.loadBillwiseDataBeforeCreate(loadRequest);

            assertNotNull(result);
            assertTrue(result.containsKey("billDetails"));
            // Legacy behaviour: no billwise rows → container skipped, no bill detail produced
            assertEquals(0, result.get("totalCount"));
        }
    }

    @Test
    void testGetAutoPopulatedGlAccounts_StoredProcedureError() {
        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
            .thenThrow(new RuntimeException("SP error"));

        var result = service.getAutoPopulatedGlAccounts(1001L, "IMPORT", 100L);

        assertNotNull(result);
        assertNull(result.get("payableGlPoid"));
    }

    @Test
    void testGetAutoPopulatedGlAccounts_NoData() {
        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
            .thenReturn("NO_DATA");

        var result = service.getAutoPopulatedGlAccounts(1001L, "IMPORT", 100L);

        assertNotNull(result);
        assertNull(result.get("payableGlPoid"));
    }
}
