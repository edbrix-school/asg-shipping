package com.asg.shipping.commoditymaster.service;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import com.asg.shipping.commoditymaster.mapper.CommodityMapper;
import com.asg.shipping.commoditymaster.repository.CommodityMasterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommodityMasterServiceTest {

    @Mock
    private CommodityMasterRepository commodityMasterRepository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private CommodityMapper mapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private CommodityMasterService commodityMasterService;

    private CommodityMaster testCommodity;
    private CommodityMasterRequest testRequest;
    private CommodityMasterResponse testResponse;

    @BeforeEach
    void setUp() {
        testCommodity = new CommodityMaster();
        testCommodity.setCommodityPoid(1L);
        testCommodity.setGroupPoid(1L);
        testCommodity.setCommodityCode("TEST001");
        testCommodity.setCommodityName("Test Commodity");
        testCommodity.setActive("Y");

        testRequest = CommodityMasterRequest.builder()
                .commodityName("Test Commodity")
                .active("Y")
                .build();

        testResponse = CommodityMasterResponse.builder()
                .commodityPoid(1L)
                .commodityName("Test Commodity")
                .active("Y")
                .build();
    }

    @Test
    void getCommodityById_Success() {
        when(commodityMasterRepository.findActiveByCommodityPoid(1L))
                .thenReturn(Optional.of(testCommodity));
        when(mapper.mapToDto(testCommodity)).thenReturn(testResponse);

        CommodityMasterResponse result = commodityMasterService.getCommodityById(1L);

        assertNotNull(result);
        assertEquals(testResponse.getCommodityPoid(), result.getCommodityPoid());
        verify(commodityMasterRepository).findActiveByCommodityPoid(1L);
        verify(mapper).mapToDto(testCommodity);
        // VIEWED logging is now handled in controller, not service
        verify(loggingService, never()).createLogSummaryEntry((LogDetailsEnum) any(), any(), any());
    }

    @Test
    void getCommodityById_NotFound() {
        when(commodityMasterRepository.findActiveByCommodityPoid(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> commodityMasterService.getCommodityById(1L));
    }


    @Test
    void createCommodity_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserName).thenReturn("testuser");

            when(commodityMasterRepository.save(any(CommodityMaster.class))).thenReturn(testCommodity);
            when(mapper.mapToDto(testCommodity)).thenReturn(testResponse);

            CommodityMasterResponse result = commodityMasterService.createCommodity(testRequest);

            assertNotNull(result);
            verify(mapper).mapCreateDTOToEntity(eq(testRequest), any(CommodityMaster.class), eq(1L), eq("testuser"));
            verify(commodityMasterRepository).save(any(CommodityMaster.class));
        }
    }

    @Test
    void updateCommodity_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserName).thenReturn("testuser");
            when(commodityMasterRepository.findByCommodityPoid(1L))
                    .thenReturn(Optional.of(testCommodity));
            when(commodityMasterRepository.save(testCommodity)).thenReturn(testCommodity);
            when(mapper.mapToDto(testCommodity)).thenReturn(testResponse);

            CommodityMasterResponse result = commodityMasterService.updateCommodity(1L, testRequest);

            assertNotNull(result);
            verify(mapper).mapUpdateDTOToEntity(testRequest, testCommodity, "testuser");
            verify(commodityMasterRepository).save(testCommodity);
        }
    }

    @Test
    void updateCommodity_NotFound() {
        when(commodityMasterRepository.findByCommodityPoid(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commodityMasterService.updateCommodity(1L, testRequest));
    }

    @Test
    void listCommodities_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-001");

            com.asg.common.lib.dto.FilterRequestDto filterRequest = 
                new com.asg.common.lib.dto.FilterRequestDto("AND", "N", java.util.Collections.emptyList());
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 20);

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveFilters(filterRequest)).thenReturn(java.util.Collections.emptyList());
            when(documentSearchService.search(anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new com.asg.common.lib.dto.RawSearchResult(
                            java.util.Collections.emptyList(), 
                            java.util.Collections.emptyMap(), 
                            0L));

            Map<String, Object> result = commodityMasterService.listCommodities("100-001", filterRequest, pageable);

            assertNotNull(result);
            verify(documentSearchService).search(eq("100-001"), any(), eq("AND"), 
                    eq(pageable), eq("N"), eq("COMODITY_NAME"), eq("COMODITY_POID"));
        }
    }

    @Test
    void listCommodities_WithFilters() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-001");

            com.asg.common.lib.dto.FilterDto filter = new com.asg.common.lib.dto.FilterDto("COMODITY_NAME", "Test");
            com.asg.common.lib.dto.FilterRequestDto filterRequest = 
                new com.asg.common.lib.dto.FilterRequestDto("OR", "N", java.util.List.of(filter));
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 20);

            when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
            when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
            when(documentSearchService.resolveFilters(filterRequest)).thenReturn(java.util.List.of(filter));
            when(documentSearchService.search(anyString(), any(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new com.asg.common.lib.dto.RawSearchResult(
                            java.util.Collections.emptyList(), 
                            java.util.Collections.emptyMap(), 
                            0L));

            Map<String, Object> result = commodityMasterService.listCommodities("100-001", filterRequest, pageable);

            assertNotNull(result);
            verify(documentSearchService).search(eq("100-001"), any(), eq("OR"), 
                    eq(pageable), eq("N"), eq("COMODITY_NAME"), eq("COMODITY_POID"));
        }
    }

    @Test
    void softDeleteCommodity_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");
        
        when(commodityMasterRepository.findByCommodityPoid(1L))
                .thenReturn(Optional.of(testCommodity));
        // Mock the deleteDocument method - it might return something or be void
        // Using lenient to avoid strict stubbing issues
        lenient().when(documentDeleteService.deleteDocument(1L, "SHIP_COMODITY_MASTER", "COMODITY_POID", deleteReasonDto, null))
                .thenReturn(null); // or whatever the method returns

        commodityMasterService.softDeleteCommodity(1L, deleteReasonDto);

        verify(documentDeleteService).deleteDocument(1L, "SHIP_COMODITY_MASTER", "COMODITY_POID", deleteReasonDto, null);
    }

    @Test
    void softDeleteCommodity_WithNullDeleteReason() {
        when(commodityMasterRepository.findByCommodityPoid(1L))
                .thenReturn(Optional.of(testCommodity));
        lenient().when(documentDeleteService.deleteDocument(1L, "SHIP_COMODITY_MASTER", "COMODITY_POID", null, null))
                .thenReturn(null);

        commodityMasterService.softDeleteCommodity(1L, null);

        verify(documentDeleteService).deleteDocument(1L, "SHIP_COMODITY_MASTER", "COMODITY_POID", null, null);
    }

    @Test
    void softDeleteCommodity_NotFound() {
        when(commodityMasterRepository.findByCommodityPoid(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commodityMasterService.softDeleteCommodity(1L, null));
    }

    @Test
    void getRefreshedCommodity_Success() {
        when(commodityMasterRepository.findById(1L))
                .thenReturn(Optional.of(testCommodity));
        when(mapper.mapToDto(testCommodity)).thenReturn(testResponse);

        CommodityMasterResponse result = commodityMasterService.getRefreshedCommodity(1L);

        assertNotNull(result);
        assertEquals(testResponse.getCommodityPoid(), result.getCommodityPoid());
        verify(commodityMasterRepository).findById(1L);
    }

    @Test
    void getRefreshedCommodity_NotFound() {
        when(commodityMasterRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commodityMasterService.getRefreshedCommodity(1L));
    }
}