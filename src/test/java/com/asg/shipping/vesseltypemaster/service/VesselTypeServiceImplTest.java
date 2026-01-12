package com.asg.shipping.vesseltypemaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;
import com.asg.shipping.vesseltypemaster.entity.ShipVesselTypeMaster;
import com.asg.shipping.vesseltypemaster.repository.ShipVesselTypeMasterRepository;
import com.asg.shipping.vesseltypemaster.util.VesselTypeMapper;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselTypeServiceImplTest {

    @Mock
    private ShipVesselTypeMasterRepository vesselTypeRepository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private VesselTypeMapper mapper;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private VesselTypeServiceImpl vesselTypeService;

    private ShipVesselTypeMaster testEntity;
    private VesselTypeDto testDto;
    private VesselTypeCreateDTO createDto;
    private VesselTypeUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        testEntity = ShipVesselTypeMaster.builder()
                .vesselTypePoid(1L)
                .vesselTypeCode("VT001")
                .vesselTypeName("Container Ship")
                .vesselTypeName2("Container")
                .active("Y")
                .deleted("N")
                .groupPoid(1L)
                .createdBy("testuser")
                .createdDate(LocalDateTime.now())
                .build();

        testDto = VesselTypeDto.builder()
                .vesselTypePoid(1L)
                .vesselTypeCode("VT001")
                .vesselTypeName("Container Ship")
                .active("Y")
                .build();

        createDto = VesselTypeCreateDTO.builder()
                .vesselTypeCode("VT001")
                .vesselTypeName("Container Ship")
                .active("Y")
                .build();

        updateDto = VesselTypeUpdateDTO.builder()
                .vesselTypeName("Updated Ship")
                .active("Y")
                .build();
    }

    @Test
    void searchVesselTypes_Success() {
        FilterRequestDto request = new FilterRequestDto("OR", "N", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 20);
        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentSearchService.resolveOperator(request)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(Collections.emptyList());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = vesselTypeService.searchVesselTypes("100-003", request, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(eq("100-003"), anyList(), eq("OR"), eq(pageable), eq("N"), eq("VESSEL_TYPE_NAME"), eq("VESSEL_TYPE_POID"));
    }

    @Test
    void searchVesselTypes_WithFilters() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of(new FilterDto("vesselTypeName", "Container")));
        Pageable pageable = PageRequest.of(0, 20);
        RawSearchResult rawResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentSearchService.resolveOperator(request)).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
        when(documentSearchService.resolveFilters(request)).thenReturn(List.of(new FilterDto("vesselTypeName", "Container")));
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = vesselTypeService.searchVesselTypes("100-003", request, pageable);

        assertNotNull(result);
    }

    @Test
    void getVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            lenient().doNothing().when(loggingService)
                    .createLogSummaryEntry(any(String.class), any(), any());

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(mapper.mapToDto(testEntity)).thenReturn(testDto);

            VesselTypeDto result = vesselTypeService.getVesselType(1L);

            assertNotNull(result);
            assertEquals("VT001", result.getVesselTypeCode());
            verify(vesselTypeRepository).findByVesselTypePoidAndGroupPoid(1L, 1L);
        }
    }

    @Test
    void getVesselType_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselTypeService.getVesselType(1L));
        }
    }

    @Test
    void createVesselType_Success() {
        when(vesselTypeRepository.existsByVesselTypeCode(createDto.getVesselTypeCode())).thenReturn(false);
        when(vesselTypeRepository.existsByVesselTypeName(createDto.getVesselTypeName())).thenReturn(false);
        when(vesselTypeRepository.save(any(ShipVesselTypeMaster.class))).thenReturn(testEntity);
        when(mapper.mapToDto(testEntity)).thenReturn(testDto);
        lenient().doNothing().when(loggingService)
                .createLogSummaryEntry(any(String.class), any(), any());

        VesselTypeDto result = vesselTypeService.createVesselType(createDto, 1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getVesselTypePoid());
        verify(vesselTypeRepository).save(any(ShipVesselTypeMaster.class));
        verify(mapper).mapCreateDTOToEntity(eq(createDto), any(ShipVesselTypeMaster.class), eq(1L), eq(1L));
    }

    @Test
    void createVesselType_DuplicateCode() {
        when(vesselTypeRepository.existsByVesselTypeCode(createDto.getVesselTypeCode())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> vesselTypeService.createVesselType(createDto, 1L, 1L));
    }

    @Test
    void createVesselType_DuplicateName() {
        when(vesselTypeRepository.existsByVesselTypeCode(createDto.getVesselTypeCode())).thenReturn(false);
        when(vesselTypeRepository.existsByVesselTypeName(createDto.getVesselTypeName())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> vesselTypeService.createVesselType(createDto, 1L, 1L));
    }

    @Test
    void updateVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(vesselTypeRepository.existsByVesselTypeNameExcludingPoid(updateDto.getVesselTypeName(), 1L)).thenReturn(false);
            when(vesselTypeRepository.save(any(ShipVesselTypeMaster.class))).thenReturn(testEntity);
            when(mapper.mapToDto(testEntity)).thenReturn(testDto);

            VesselTypeDto result = vesselTypeService.updateVesselType(1L, updateDto, 1L, 1L);

            assertNotNull(result);
            verify(vesselTypeRepository).save(any(ShipVesselTypeMaster.class));
            verify(mapper).mapUpdateDTOToEntity(eq(updateDto), eq(testEntity), eq(1L), eq(1L));
        }
    }

    @Test
    void updateVesselType_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselTypeService.updateVesselType(1L, updateDto, 1L, 1L));
        }
    }

    @Test
    void updateVesselType_DuplicateName() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(vesselTypeRepository.existsByVesselTypeNameExcludingPoid(updateDto.getVesselTypeName(), 1L)).thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, () -> vesselTypeService.updateVesselType(1L, updateDto, 1L, 1L));
        }
    }

    @Test
    void toggleActive_Success_FromYToN() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            lenient().doNothing().when(loggingService)
                    .createLogSummaryEntry(any(String.class), any(), any());

            testEntity.setActive("Y");
            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(vesselTypeRepository.save(any(ShipVesselTypeMaster.class))).thenReturn(testEntity);

            vesselTypeService.toggleActive(1L);

            verify(vesselTypeRepository).save(argThat(saved -> "N".equals(saved.getActive())));
        }
    }

    @Test
    void toggleActive_Success_FromNToY() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            lenient().doNothing().when(loggingService)
                    .createLogSummaryEntry(any(String.class), any(), any());

            testEntity.setActive("N");
            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(vesselTypeRepository.save(any(ShipVesselTypeMaster.class))).thenReturn(testEntity);

            vesselTypeService.toggleActive(1L);

            verify(vesselTypeRepository).save(argThat(saved -> "Y".equals(saved.getActive())));
        }
    }

    @Test
    void toggleActive_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselTypeService.toggleActive(1L));
        }
    }

    @Test
    void deleteVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            lenient().doNothing().when(loggingService)
                    .createLogSummaryEntry(any(String.class), any(), any());

            testEntity.setDeleted("N");
            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));
            when(vesselTypeRepository.save(any(ShipVesselTypeMaster.class))).thenReturn(testEntity);

            vesselTypeService.deleteVesselType(1L);

            verify(vesselTypeRepository).save(argThat(saved -> 
                "Y".equals(saved.getDeleted()) && "N".equals(saved.getActive())
            ));
        }
    }

    @Test
    void deleteVesselType_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testEntity.setDeleted("Y");
            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testEntity));

            vesselTypeService.deleteVesselType(1L);

            verify(vesselTypeRepository, never()).save(any(ShipVesselTypeMaster.class));
        }
    }

    @Test
    void deleteVesselType_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(vesselTypeRepository.findByVesselTypePoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselTypeService.deleteVesselType(1L));
        }
    }
}
