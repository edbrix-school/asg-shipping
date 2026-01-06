package com.asg.shipping.regionmaster;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import com.asg.shipping.regionmaster.entity.ShipRegionMasterEntity;
import com.asg.shipping.regionmaster.repository.ShipRegionMasterRepository;
import com.asg.shipping.regionmaster.service.impl.RegionMasterServiceImpl;
import com.asg.shipping.regionmaster.util.RegionMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionMasterServiceImplTest {

    @Mock
    private ShipRegionMasterRepository repository;
    @Mock
    private RegionMasterMapper mapper;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private RegionMasterServiceImpl service;

    private ShipRegionMasterEntity entity;
    private RegionMasterRequest request;
    private RegionMasterResponse response;

    @BeforeEach
    void setup() {
        request = new RegionMasterRequest();
        request.setRegionCode("ME");
        request.setRegionName("Middle East");

        entity = ShipRegionMasterEntity.builder()
                .regionPoid(1L)
                .groupPoid(10L)
                .regionCode("ME")
                .regionName("Middle East")
                .active("Y")
                .deleted("N")
                .build();

        response = new RegionMasterResponse();
        response.setRegionPoid(1L);
        response.setRegionCode("ME");
        response.setRegionName("Middle East");
    }

    // ---------- LIST ----------
    @Test
    void testListRegionMasters() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "Middle");
        FilterRequestDto filterRequest =
                new FilterRequestDto("OR", "N", List.of(filter));

        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("REGION_CODE", "ME")),
                Map.of("REGION_CODE", "Region Code"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result =
                service.listRegionMasters("DOC-1", filterRequest, pageable);

        assertNotNull(result);
    }

    // ---------- GET BY ID ----------
    @Test
    void testGetById_Success() {
        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        RegionMasterResponse result = service.getById(1L, 10L);

        assertEquals("ME", result.getRegionCode());
    }

    @Test
    void testGetById_NotFound() {
        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(1L, 10L));
    }

    // ---------- CREATE ----------
    @Test
    void testCreate_Success() {
        when(repository.existsByRegionCodeAndGroupPoidAndDeletedNot(any(), any(), any()))
                .thenReturn(false);
        when(repository.existsByRegionNameAndGroupPoidAndDeletedNot(any(), any(), any()))
                .thenReturn(false);
        when(mapper.toEntity(any(), any(), any())).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        doNothing().when(loggingService)
                .createLogSummaryEntry(any(), any(), any());
        doNothing().when(loggingService)
                .logChanges(any(), any(), any(), any(), any(), any(), any());

        RegionMasterResponse result =
                service.create(request, 10L, "user1", "DOC-1");

        assertNotNull(result);
    }

    @Test
    void testCreate_DuplicateCode() {
        when(repository.existsByRegionCodeAndGroupPoidAndDeletedNot(any(), any(), any()))
                .thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.create(request, 10L, "user1", "DOC-1"));
    }

    // ---------- UPDATE ----------
    @Test
    void testUpdate_Success() {
        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        doNothing().when(loggingService)
                .createLogSummaryEntry(any(), any(), any());
        doNothing().when(loggingService)
                .logChanges(any(), any(), any(), any(), any(), any(), any());

        RegionMasterResponse result =
                service.update(1L, request, 10L, "user1", "DOC-1");

        assertNotNull(result);
    }

    // ---------- DELETE ----------
    @Test
    void testDelete() {
        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));

        assertDoesNotThrow(() ->
                service.delete(1L, 10L, "user1"));

        verify(repository).save(any());
    }

    // ---------- TOGGLE ACTIVE STATUS ----------
    @Test
    void testToggleActiveStatus_FromNtoY() {

        // GIVEN
        entity.setActive("N");

        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));

        // WHEN
        service.toggleActiveStatus(1L, 10L, "user1");

        // THEN
        verify(repository).save(argThat(saved ->
                "Y".equals(saved.getActive())
                        && "user1".equals(saved.getLastModifiedBy())
                        && saved.getLastModifiedDate() != null
        ));
    }

    @Test
    void testToggleActiveStatus_FromYtoN() {

        // GIVEN
        entity.setActive("Y");

        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));

        // WHEN
        service.toggleActiveStatus(1L, 10L, "user1");

        // THEN
        verify(repository).save(argThat(saved ->
                "N".equals(saved.getActive())
                        && "user1".equals(saved.getLastModifiedBy())
                        && saved.getLastModifiedDate() != null
        ));
    }

    @Test
    void testToggleActiveStatus_NotFound() {

        // GIVEN
        when(repository.findByRegionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        // THEN
        assertThrows(ResourceNotFoundException.class,
                () -> service.toggleActiveStatus(1L, 10L, "user1"));
    }


}

