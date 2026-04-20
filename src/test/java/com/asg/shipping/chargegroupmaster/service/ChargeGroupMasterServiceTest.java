package com.asg.shipping.chargegroupmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargegroupmaster.entity.ShipChargeGroupMaster;
import com.asg.shipping.chargegroupmaster.repository.ShipChargeGroupMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChargeGroupMasterServiceTest {

    @Mock
    private ShipChargeGroupMasterRepository repository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private LovDataService lovService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private DocumentSearchService documentService;

    @InjectMocks
    private ChargeGroupMasterServiceImpl service;

    private ChargeGroupMasterRequestDto requestDto;
    private ShipChargeGroupMaster entity;
    private LovGetListDto lovDto;

    @BeforeEach
    void setUp() {
        requestDto = ChargeGroupMasterRequestDto.builder()
                .chargeGroupCode("TEST001")
                .chargeGroupName("Test Charge Group")
                .chargeGroupName2("Test Charge Group 2")
                .chargeGlPayable(1001L)
                .chargeGlSale(1002L)
                .chargeGlCostSale(1003L)
                .linewisePayablePosting("Y")
                .active("Y")
                .seqNo(1L)
                .build();

        entity = ShipChargeGroupMaster.builder()
                .chargeGroupPoid(1L)
                .groupPoid(100L)
                .chargeGroupCode("TEST001")
                .chargeGroupName("Test Charge Group")
                .chargeGroupName2("Test Charge Group 2")
                .chargeGlPayable(1001L)
                .chargeGlSale(1002L)
                .chargeGlCostSale(1003L)
                .linewisePayablePosting("Y")
                .glPrefix("TG")
                .active("Y")
                .deleted("N")
                .seqNo(1L)
                .build();

        lovDto = new LovGetListDto();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(100L);
            mocked.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.empty());
            when(repository.findByChargeGroupName("Test Charge Group")).thenReturn(Optional.empty());
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);

            ChargeGroupMasterResponseDto result = service.create(requestDto);

            assertNotNull(result);
            assertEquals("TEST001", result.getChargeGroupCode());
            assertEquals("TEST CHARGE GROUP", result.getChargeGroupName());
            verify(repository).save(any(ShipChargeGroupMaster.class));
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC001", "1");
        }
    }

    @Test
    void create_DuplicateCode() {
        when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.create(requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void create_DuplicateName() {
        when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.empty());
        when(repository.findByChargeGroupName("Test Charge Group")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.create(requestDto));
        verify(repository, never()).save(any());
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(100L);
            mocked.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(repository.existsByChargeGroupNameAndChargeGroupPoidNot("Test Charge Group", 1L)).thenReturn(false);
            when(repository.existsByChargeGroupCodeAndChargeGroupPoidNot("TEST001", 1L)).thenReturn(false);
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);

            ChargeGroupMasterResponseDto result = service.update(1L, requestDto);

            assertNotNull(result);
            assertEquals("TEST001", result.getChargeGroupCode());
            verify(repository).save(any(ShipChargeGroupMaster.class));
            verify(loggingService).logChanges(any(), any(), eq(ShipChargeGroupMaster.class),
                    eq("DOC001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("CHARGE_GROUP_POID"));
        }
    }

    @Test
    void update_NotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(999L, requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void update_DuplicateName() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByChargeGroupNameAndChargeGroupPoidNot("Test Charge Group", 1L)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(1L, requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void update_DuplicateCode() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByChargeGroupNameAndChargeGroupPoidNot("Test Charge Group", 1L)).thenReturn(false);
        when(repository.existsByChargeGroupCodeAndChargeGroupPoidNot("TEST001", 1L)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(1L, requestDto));
        verify(repository, never()).save(any());
    }

    // ─── FIND BY ID ───────────────────────────────────────────────────────────

    @Test
    void findById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(lovDto);

        ChargeGroupMasterResponseDto result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getChargeGroupPoid());
        assertEquals("TEST001", result.getChargeGroupCode());
        assertNotNull(result.getGroupDet());
        assertNotNull(result.getChargeGlPaybeDet());
        assertNotNull(result.getChargeGlSaleDet());
        assertNotNull(result.getChargeGlCostSaleDet());
    }

    @Test
    void findById_NotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(999L));
    }

    @Test
    void findById_NullLovFields_ReturnsNullDets() {
        ShipChargeGroupMaster entityNullFields = ShipChargeGroupMaster.builder()
                .chargeGroupPoid(2L)
                .groupPoid(null)
                .chargeGroupCode("TEST002")
                .chargeGroupName("No LOV Group")
                .chargeGlPayable(null)
                .chargeGlSale(null)
                .chargeGlCostSale(null)
                .build();

        when(repository.findById(2L)).thenReturn(Optional.of(entityNullFields));

        ChargeGroupMasterResponseDto result = service.findById(2L);

        assertNotNull(result);
        assertNull(result.getGroupDet());
        assertNull(result.getChargeGlPaybeDet());
        assertNull(result.getChargeGlSaleDet());
        assertNull(result.getChargeGlCostSaleDet());
        verify(lovService, never()).getDetailsByPoidAndLovName(any(), any());
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void delete_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L, deleteReasonDto);

        verify(repository).findById(1L);
        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq("SHIP_CHARGE_GROUP_MASTER"),
                eq("CHARGE_GROUP_POID"),
                eq(deleteReasonDto),
                isNull()
        );
    }

    @Test
    void delete_NotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(999L, new DeleteReasonDto()));
        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Test
    void listChargeGroupMaster_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("CHARGE_GROUP_CODE", "TEST001")),
                Map.of("CHARGE_GROUP_CODE", "Charge Group Code"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveDateFilters(eq(filterRequest), eq("TRANSACTION_DATE"), any(), any()))
                .thenReturn(List.of());
        when(documentService.search(eq("DOC001"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("CHARGE_GROUP_CODE"), eq("CHARGE_GROUP_POID")))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listChargeGroupMaster("DOC001", filterRequest, null, null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC001"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("CHARGE_GROUP_CODE"), eq("CHARGE_GROUP_POID"));
    }

    @Test
    void listChargeGroupMaster_WithDateParams() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        FilterDto dateFilter = new FilterDto("TRANSACTION_DATE", "2024-01-01");
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(null)).thenReturn("OR");
        when(documentService.resolveIsDeleted(null)).thenReturn("N");
        when(documentService.resolveDateFilters(null, "TRANSACTION_DATE", start, end))
                .thenReturn(List.of(dateFilter));
        when(documentService.search(any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listChargeGroupMaster("DOC001", null, start, end, pageable);

        assertNotNull(result);
        verify(documentService).resolveDateFilters(null, "TRANSACTION_DATE", start, end);
    }

    @Test
    void listChargeGroupMaster_WithNullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(null)).thenReturn("OR");
        when(documentService.resolveIsDeleted(null)).thenReturn("N");
        when(documentService.resolveDateFilters(isNull(), anyString(), any(), any())).thenReturn(List.of());
        when(documentService.search(any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listChargeGroupMaster("DOC001", null, null, null, pageable);

        assertNotNull(result);
    }

    // ─── GET CURRENT USER ─────────────────────────────────────────────────────

    @Test
    void getCurrentUser_WithUserId() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getUserId).thenReturn("123");

            assertEquals("123", ChargeGroupMasterServiceImpl.getCurrentUser());
        }
    }

    @Test
    void getCurrentUser_WithoutUserId() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getUserId).thenReturn(null);

            assertEquals("SYSTEM", ChargeGroupMasterServiceImpl.getCurrentUser());
        }
    }
}
