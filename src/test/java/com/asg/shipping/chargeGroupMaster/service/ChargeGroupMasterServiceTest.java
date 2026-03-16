package com.asg.shipping.chargeGroupMaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargeGroupMaster.entity.ShipChargeGroupMaster;
import com.asg.shipping.chargeGroupMaster.repository.ShipChargeGroupMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    private DocumentSearchService documentService;

    @InjectMocks
    private ChargeGroupMasterServiceImpl service;

    private ChargeGroupMasterRequestDto requestDto;
    private ShipChargeGroupMaster entity;
    private ChargeGroupMasterResponseDto responseDto;

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

        responseDto = ChargeGroupMasterResponseDto.builder()
                .chargeGroupPoid(1L)
                .groupPoid(100L)
                .chargeGroupCode("TEST001")
                .chargeGroupName("Test Charge Group")
                .chargeGroupName2("Test Charge Group 2")
                .chargeGlPayable(1001L)
                .chargeGlSale(1002L)
                .chargeGlCostSale(1003L)
                .linewisePayablePosting("Y")
                .active("Y")
                .seqNo(1L)
                .createdBy("testUser")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void create_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.empty());
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);

            ChargeGroupMasterResponseDto result = service.create(requestDto);

            assertNotNull(result);
            assertEquals("TEST001", result.getChargeGroupCode());
            assertEquals("TEST CHARGE GROUP", result.getChargeGroupName());
            verify(repository).findByChargeGroupCode("TEST001");
            verify(repository).save(any(ShipChargeGroupMaster.class));

        }
    }

    @Test
    void create_DuplicateCode() {
        when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.create(requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void update_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);

            ChargeGroupMasterResponseDto result = service.update(1L, requestDto);

            assertNotNull(result);
            assertEquals("TEST001", result.getChargeGroupCode());
            verify(repository).findById(1L);
            verify(repository).save(any(ShipChargeGroupMaster.class));

        }
    }

    @Test
    void update_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void findById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ChargeGroupMasterResponseDto result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getChargeGroupPoid());
        assertEquals("TEST001", result.getChargeGroupCode());
        verify(repository).findById(1L);
    }

    @Test
    void findById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void delete_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            lenient().doNothing().when(loggingService)
                    .createLogSummaryEntry(any(String.class), any(), any());

            when(repository.findById(1L)).thenReturn(Optional.of(entity));

            service.delete(1L);

            assertEquals("Y", entity.getDeleted());
            assertEquals("N", entity.getActive());
            verify(repository).findById(1L);
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
            verify(loggingService, times(2)).logSimpleFieldChange(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void delete_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));
    }

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
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listChargeGroupMaster("DOC001", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC001"), anyList(), eq("AND"),
                eq(pageable), eq("N"), eq("CHARGE_GROUP_CODE"), eq("CHARGE_GROUP_POID"));
    }

    @Test
    void getCurrentUser_WithUserId() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");

            String result = ChargeGroupMasterServiceImpl.getCurrentUser();

            assertEquals("123", result);
        }
    }

    @Test
    void getCurrentUser_WithoutUserId() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(null);

            String result = ChargeGroupMasterServiceImpl.getCurrentUser();

            assertEquals("SYSTEM", result);
        }
    }

    @Test
    void create_DuplicateName() {
        when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.empty());
        when(repository.findByChargeGroupName("Test Charge Group")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.create(requestDto));
        verify(repository, never()).save(any());
    }

    @Test
    void create_LoggingVerification() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findByChargeGroupCode("TEST001")).thenReturn(Optional.empty());
            when(repository.findByChargeGroupName("Test Charge Group")).thenReturn(Optional.empty());
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);

            service.create(requestDto);

            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("DOC001"), eq("1"));
            verify(loggingService).logChanges(any(), any(), eq(ShipChargeGroupMaster.class), eq("DOC001"), eq("1"), eq(LogDetailsEnum.CREATED), eq("CHARGE_GROUP_POID"));
        }
    }

    @Test
    void update_LoggingVerification() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(ShipChargeGroupMaster.class))).thenReturn(entity);

            service.update(1L, requestDto);

            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.MODIFIED), eq("DOC001"), eq("1"));
            verify(loggingService).logChanges(any(), any(), eq(ShipChargeGroupMaster.class), eq("DOC001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("CHARGE_GROUP_POID"));
        }
    }

    @Test
    void listChargeGroupMaster_WithNullFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("CHARGE_GROUP_CODE", "TEST001")),
                Map.of("CHARGE_GROUP_CODE", "Charge Group Code"),
                1L
        );

        when(documentService.resolveOperator(null)).thenReturn("OR");
        when(documentService.resolveIsDeleted(null)).thenReturn("N");
        when(documentService.resolveFilters(null)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listChargeGroupMaster("DOC001", null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC001"), anyList(), eq("OR"),
                eq(pageable), eq("N"), eq("CHARGE_GROUP_CODE"), eq("CHARGE_GROUP_POID"));
    }
}
