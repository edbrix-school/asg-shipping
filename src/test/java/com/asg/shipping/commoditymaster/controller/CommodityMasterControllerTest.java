package com.asg.shipping.commoditymaster.controller;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.mapper.CommodityMapper;
import com.asg.shipping.commoditymaster.service.CommodityMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommodityMasterControllerTest {

    @Mock
    private CommodityMasterService commodityService;

    @Mock
    private CommodityMapper mapper;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private CommodityMasterController controller;

    private CommodityMasterRequest testRequest;
    private CommodityMasterResponse testResponse;

    @BeforeEach
    void setUp() {
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
        when(commodityService.getCommodityById(1L)).thenReturn(testResponse);

        ResponseEntity<?> response = controller.getCommodityById(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(commodityService).getCommodityById(1L);
    }

    @Test
    void getCommodityById_NotFound() {
        when(commodityService.getCommodityById(1L))
                .thenThrow(new ResourceNotFoundException("Commodity", "commodityPoid", "1"));

        ResponseEntity<?> response = controller.getCommodityById(1L);
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value()); // or 400 depending on your @ExceptionHandler
    }

    @Test
    void createCommodity_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserName).thenReturn("testuser");

            when(commodityService.createCommodity(any())).thenReturn(testResponse);

            ResponseEntity<?> response = controller.createCommodity(testRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(commodityService).createCommodity(any());
        }
    }

    @Test
    void createCommodity_WithCommodityPoid_BadRequest() {
        testRequest.setCommodityPoid(1L);

        ResponseEntity<?> response = controller.createCommodity(testRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        verify(commodityService, never()).createCommodity(any());
    }

    @Test
    void updateCommodity_Success() {
        when(commodityService.updateCommodity(eq(1L), any())).thenReturn(testResponse);

        ResponseEntity<?> response = controller.updateCommodity(1L, testRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(commodityService).updateCommodity(eq(1L), any());
    }

    @Test
    void softDeleteCommodity_Success() {
        doNothing().when(commodityService).softDeleteCommodity(1L);

        ResponseEntity<?> response = controller.softDeleteCommodity(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(commodityService).softDeleteCommodity(1L);
    }

    @Test
    void updateCommodity_NotFound() {
        when(commodityService.updateCommodity(eq(1L), any()))
                .thenThrow(new ResourceNotFoundException("Commodity", "commodityPoid", "1"));

        ResponseEntity<?> response = controller.updateCommodity(1L, testRequest);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void softDeleteCommodity_NotFound() {
        doThrow(new ResourceNotFoundException("Commodity", "commodityPoid", "1"))
                .when(commodityService).softDeleteCommodity(1L);

        ResponseEntity<?> response = controller.softDeleteCommodity(1L);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void getCommodities_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-001");

            com.asg.common.lib.dto.FilterRequestDto filterRequest = 
                new com.asg.common.lib.dto.FilterRequestDto("AND", "N", null);
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 20);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", java.util.Collections.emptyList());
            result.put("totalElements", 0);

            when(commodityService.listCommodities(eq("100-001"), any(), any()))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.getCommodities(pageable, filterRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(commodityService).listCommodities(eq("100-001"), any(), any());
        }
    }

    @Test
    void getCommodities_WithNullFilters() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-001");

            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 20);
            java.util.Map<String, Object> result = new java.util.HashMap<>();

            when(commodityService.listCommodities(eq("100-001"), any(), any()))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.getCommodities(pageable, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void getCommodities_Exception() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-001");

            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 20);

            when(commodityService.listCommodities(eq("100-001"), any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            ResponseEntity<?> response = controller.getCommodities(pageable, null);

            assertNotNull(response);
            assertEquals(500, response.getStatusCode().value());
        }
    }
}