package com.asg.shipping.containertypeportchargestariff.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesDtl;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesHdr;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesDtlRepository;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesHdrRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.portMaster.repository.PortMasterRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortChargesTariffServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ShipPortChargesHdrRepository hdrRepository;
    @Mock
    private ShipPortChargesDtlRepository dtlRepository;
    @Mock
    private PortMasterRepository portMasterRepository;
    @Mock
    private ShipLineMasterThirdPartyRepository lineMasterThirdPartyRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private com.asg.common.lib.service.DocumentDeleteService documentDeleteService;
    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private PortChargesTariffServiceImpl service;

    private ShipPortChargesHdr mockHdr;
    private ShipPortChargesDtl mockDtl;
    private PortChargesTariffCreateDto createDto;
    private CopyTariffRequestDto copyRequest;

    @BeforeEach
    void setUp() {
        mockHdr = new ShipPortChargesHdr();
        mockHdr.setTransactionPoid(1L);
        mockHdr.setGroupPoid(100L);
        mockHdr.setPortPoid(200L);
        mockHdr.setDescription("Test Tariff");
        mockHdr.setPeriodFrom(LocalDate.of(2024, 1, 1));
        mockHdr.setPeriodTo(LocalDate.of(2024, 12, 31));
        mockHdr.setChargeLinePoid(300L);
        mockHdr.setChargeDivision("DIV1");
        mockHdr.setDeleted("N");

        mockDtl = new ShipPortChargesDtl();
        mockDtl.setTransactionPoid(1L);
        mockDtl.setDetRowId(1L);
        mockDtl.setAmount20(BigDecimal.valueOf(100));

        createDto = new PortChargesTariffCreateDto();
        createDto.setPortPoid(200L);
        createDto.setDescription("New Tariff");
        createDto.setPeriodFrom(LocalDate.of(2024, 1, 1));
        createDto.setPeriodTo(LocalDate.of(2024, 12, 31));
        createDto.setChargeLinePoid(300L);
        createDto.setChargeDivision("DIV1");

        copyRequest = new CopyTariffRequestDto();
        copyRequest.setDescription("Copied Tariff");
        copyRequest.setUseStoredProcedure(false);
    }

    @Test
    void testGetPortChargesTariff_Success() {
        Long transactionPoid = 1L;
        Long groupPoid = 100L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.of(mockHdr));
            when(dtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(mockDtl));

            PortChargesTariffDto result = service.getPortChargesTariff(transactionPoid);

            assertNotNull(result);
            assertEquals(transactionPoid, result.getTransactionPoid());
            assertEquals("Test Tariff", result.getDescription());
        }
    }

    @Test
    void testGetPortChargesTariff_NotFound() {
        Long transactionPoid = 999L;
        Long groupPoid = 100L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                () -> service.getPortChargesTariff(transactionPoid));
        }
    }

    @Test
    void testCreatePortChargesTariff_Success() {
        Long groupPoid = 100L;
        Long userPoid = 50L;
        
        when(portMasterRepository.existsByPortPoidAndGroupPoid(200L, groupPoid)).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(300L)).thenReturn(true);
        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(hdrRepository.save(any(ShipPortChargesHdr.class))).thenReturn(mockHdr);
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            userContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            PortChargesTariffDto result = service.createPortChargesTariff(createDto, groupPoid, userPoid);

            assertNotNull(result);
            verify(hdrRepository).save(any(ShipPortChargesHdr.class));
        }
    }

    @Test
    void testListPortChargesTariff_Success() {
        String docId = "DOC123";
        FilterRequestDto request = new FilterRequestDto("AND", "N", Collections.emptyList());
        Pageable pageable = mock(Pageable.class);
        RawSearchResult raw = new RawSearchResult(List.of(Map.of("id", 1L)), Collections.emptyMap(), 1L);

        when(documentService.resolveOperator(request)).thenReturn("AND");
        when(documentService.resolveIsDeleted(request)).thenReturn("N");
        when(documentService.resolveFilters(request)).thenReturn(Collections.emptyList());
        when(documentService.search(eq(docId), anyList(), anyString(), any(), anyString(), anyString(), anyString())).thenReturn(raw);

        Map<String, Object> result = service.listPortChargesTariff(docId, request, pageable);

        assertNotNull(result);
        verify(documentService).search(eq(docId), anyList(), eq("AND"), eq(pageable), eq("N"), anyString(), anyString());
    }

    @Test
    void testGetPortChargesTariff_GroupMismatch() {
        Long transactionPoid = 1L;
        Long userGroupPoid = 999L; // Different from mockHdr's 100L

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(userGroupPoid);
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.of(mockHdr));

            assertThrows(ResourceNotFoundException.class, () -> service.getPortChargesTariff(transactionPoid));
        }
    }

    @Test
    void testCreatePortChargesTariff_InvalidPort() {
        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(false);
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            assertThrows(ResourceNotFoundException.class, () -> service.createPortChargesTariff(createDto, 100L, 50L));
        }
    }

    @Test
    void testCreatePortChargesTariff_InvalidLine() {
        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(anyLong())).thenReturn(false);
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            assertThrows(ResourceNotFoundException.class, () -> service.createPortChargesTariff(createDto, 100L, 50L));
        }
    }

    @Test
    void testCreatePortChargesTariff_InvalidPeriod() {
        createDto.setPeriodFrom(LocalDate.of(2024, 12, 31));
        createDto.setPeriodTo(LocalDate.of(2024, 1, 1)); // From > To
        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(anyLong())).thenReturn(true);
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            assertThrows(ValidationException.class, () -> service.createPortChargesTariff(createDto, 100L, 50L));
        }
    }

    @Test
    void testCreatePortChargesTariff_OverlapConflict() {
        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(anyLong())).thenReturn(true);
        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(mockHdr)); // Conflict found

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            assertThrows(ValidationException.class, () -> service.createPortChargesTariff(createDto, 100L, 50L));
        }
    }

    @Test
    void testCreatePortChargesTariff_WithDetails() {
        PortChargesDetailCreateDto detailDto = new PortChargesDetailCreateDto();
        detailDto.setChargeCodePoid(10L);
        createDto.setDetails(List.of(detailDto));

        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(anyLong())).thenReturn(true);
        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(hdrRepository.save(any(ShipPortChargesHdr.class))).thenReturn(mockHdr);
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");
            userContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.createPortChargesTariff(createDto, 100L, 50L);

            verify(dtlRepository).saveAll(anyList());
            verify(loggingService, atLeastOnce()).createLogSummaryEntry(anyString(), anyString(), anyString());
        }
    }

    @Test
    void testUpdatePortChargesTariff_NotFound() {
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updatePortChargesTariff(1L, new PortChargesTariffUpdateDto(), 100L, 50L));
    }

    @Test
    void testUpdatePortChargesTariff_GroupMismatch() {
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        assertThrows(ResourceNotFoundException.class, () -> service.updatePortChargesTariff(1L, new PortChargesTariffUpdateDto(), 999L, 50L));
    }

    @Test
    void testUpdatePortChargesTariff_Success() {
        PortChargesTariffUpdateDto updateDto = new PortChargesTariffUpdateDto();
        updateDto.setPortPoid(200L);
        updateDto.setPeriodFrom(LocalDate.of(2024, 1, 1));
        updateDto.setPeriodTo(LocalDate.of(2024, 12, 31));
        updateDto.setChargeLinePoid(300L);

        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(portMasterRepository.existsByPortPoidAndGroupPoid(anyLong(), anyLong())).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(anyLong())).thenReturn(true);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.updatePortChargesTariff(1L, updateDto, 100L, 50L);

            verify(hdrRepository).saveAndFlush(any(ShipPortChargesHdr.class));
            verify(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), eq(LogDetailsEnum.MODIFIED), anyString());
        }
    }

    @Test
    void testUpdatePortChargesTariff_PeriodChangedOverlap() {
        PortChargesTariffUpdateDto updateDto = new PortChargesTariffUpdateDto();
        updateDto.setPeriodFrom(LocalDate.of(2025, 1, 1)); // Changed
        updateDto.setPeriodTo(LocalDate.of(2025, 12, 31)); // Changed

        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(mockHdr)); // Conflict

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            assertThrows(ValidationException.class, () -> service.updatePortChargesTariff(1L, updateDto, 100L, 50L));
        }
    }

    @Test
    void testUpdatePortChargesTariff_ProcessDetails_AllActions() {
        PortChargesTariffUpdateDto updateDto = new PortChargesTariffUpdateDto();
        updateDto.setPeriodFrom(mockHdr.getPeriodFrom());
        updateDto.setPeriodTo(mockHdr.getPeriodTo());

        PortChargesDetailUpdateDto d1 = new PortChargesDetailUpdateDto();
        d1.setActionType("isdeleted");
        d1.setDetRowId(1L);

        PortChargesDetailUpdateDto d2 = new PortChargesDetailUpdateDto();
        d2.setActionType("iscreated"); // No detRowId -> New entity
        d2.setChargeCodePoid(10L);

        PortChargesDetailUpdateDto d3 = new PortChargesDetailUpdateDto();
        d3.setActionType("isupdated");
        d3.setDetRowId(2L); // Exists -> Update

        updateDto.setDetails(List.of(d1, d2, d3));

        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(dtlRepository.findByTransactionPoidAndDetRowId(1L, 1L)).thenReturn(Optional.of(mockDtl));
        when(dtlRepository.findByTransactionPoidAndDetRowId(1L, 2L)).thenReturn(Optional.of(new ShipPortChargesDtl()));
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(mockDtl)); // For getNextDetRowId

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.updatePortChargesTariff(1L, updateDto, 100L, 50L);

            verify(dtlRepository).deleteAll(anyList());
            verify(dtlRepository).saveAll(anyList());
            verify(loggingService).logChanges(any(), any(), any(), anyString(), anyString(), eq(LogDetailsEnum.MODIFIED), anyString());
        }
    }

    @Test
    void testDeletePortChargesTariff_NotFound() {
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deletePortChargesTariff(1L, new DeleteReasonDto()));
    }
 
    @Test
    void testDeletePortChargesTariff_Success() {
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        service.deletePortChargesTariff(1L, new DeleteReasonDto());
        verify(documentDeleteService).deleteDocument(anyLong(), anyString(), anyString(), any(DeleteReasonDto.class), any());
    }

    @Test
    void testValidateOverlap_InvalidDateRange() {
        ValidateOverlapRequestDto request = new ValidateOverlapRequestDto();
        request.setPeriodFrom(LocalDate.of(2024, 12, 31));
        request.setPeriodTo(LocalDate.of(2024, 1, 1)); // Invalid

        ValidateOverlapResponseDto result = service.validateOverlap(request);
        assertTrue(result.isOverlapping());
        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    void testValidateOverlap_WithConflicts() {
        ValidateOverlapRequestDto request = new ValidateOverlapRequestDto();
        request.setPeriodFrom(LocalDate.of(2024, 1, 1));
        request.setPeriodTo(LocalDate.of(2024, 12, 31));
        request.setPortPoid(200L);

        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(mockHdr));

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);

            ValidateOverlapResponseDto result = service.validateOverlap(request);
            assertTrue(result.isOverlapping());
            assertFalse(result.getConflicts().isEmpty());
            assertEquals(1L, result.getConflicts().get(0).getTransactionPoid());
        }
    }

    @Test
    void testGetNextDetRowId_NoExisting() {
        // This is indirectly tested via processDetails when existing details are empty
        PortChargesTariffUpdateDto updateDto = new PortChargesTariffUpdateDto();
        updateDto.setPeriodFrom(mockHdr.getPeriodFrom());
        updateDto.setPeriodTo(mockHdr.getPeriodTo());
        PortChargesDetailUpdateDto d = new PortChargesDetailUpdateDto();
        d.setActionType("iscreated");
        updateDto.setDetails(List.of(d));

        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            service.updatePortChargesTariff(1L, updateDto, 100L, 50L);
            verify(dtlRepository).saveAll(anyList());
        }
    }

    @Test
    void testMapToDto_NullDetails() {
        // success() uses getPortChargesTariff which uses mapToDto
        when(hdrRepository.findById(any(Long.class))).thenReturn(Optional.of(mockHdr));
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(null); // Force null details branch

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            PortChargesTariffDto result = service.getPortChargesTariff(1L);
            assertNull(result.getDetails());
        }
    }
}
