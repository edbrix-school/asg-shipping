package com.asg.shipping.commoditymaster.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
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
}