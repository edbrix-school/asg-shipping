package com.asg.shipping.tradelanemaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.entity.ShipTradelaneMaster;
import com.asg.shipping.tradelanemaster.repository.ShipTradeLaneMasterRepository;
import com.asg.shipping.tradelanemaster.service.impl.ShipTradeLaneServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.utility.ASGHelperUtils.getGroupId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipTradeLaneServiceImplTest {

    @Mock
    private ShipTradeLaneMasterRepository repository;

    @Mock
    private DocumentSearchService documentService;

    @InjectMocks
    private ShipTradeLaneServiceImpl service;

    private ShipTradelaneRequest request;
    private ShipTradelaneMaster entity;

    @BeforeEach
    void setUp() {
        request = new ShipTradelaneRequest();
        request.setTradeLaneCode("TL001");
        request.setTradeLaneName("Trade Lane 1");
        request.setTradeLaneName2("TL1");
        request.setRegionPoid(1L);
        request.setActive(true);
        request.setSeqNo(10);

        entity = new ShipTradelaneMaster();
        entity.setTradeLanePoid(1L);
        entity.setGroupPoid(2L);
        entity.setTradeLaneCode("TL001");
        entity.setTradeLaneName("Trade Lane 1");
        entity.setTradeLaneName2("TL1");
        entity.setRegionPoid(1L);
        entity.setActive("Y");
        entity.setSeqNo(10L);
        entity.setDeleted("N");
        entity.setCreatedBy("user1");
        entity.setCreatedDate(LocalDateTime.now());
    }

    @Test
    void testCreateSuccess() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            mockedStatic.when(ASGHelperUtils::getCurrentUser).thenReturn("user1");
            
            when(repository.existsByTradeLaneCode(anyString())).thenReturn(false);
            when(repository.existsByTradeLaneName(anyString())).thenReturn(false);
            when(repository.save(any(ShipTradelaneMaster.class))).thenReturn(entity);

            ShipTradelaneResponse response = service.create(request);

            assertNotNull(response);
            assertEquals("TL001", response.getTradeLaneCode());
            assertEquals("Trade Lane 1", response.getTradeLaneName());
            assertTrue(response.getActive());
            verify(repository).save(any(ShipTradelaneMaster.class));
        }
    }

    @Test
    void testCreateDuplicateCode() {
        when(repository.existsByTradeLaneCode("TL001")).thenReturn(true);

        assertThrows(DuplicateKeyException.class, () -> service.create(request));
    }

    @Test
    void testCreateDuplicateName() {
        when(repository.existsByTradeLaneCode("TL001")).thenReturn(false);
        when(repository.existsByTradeLaneName("Trade Lane 1")).thenReturn(true);

        assertThrows(DuplicateKeyException.class, () -> service.create(request));
    }

    @Test
    void testUpdateSuccess() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            mockedStatic.when(ASGHelperUtils::getCurrentUser).thenReturn("user2");
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(ShipTradelaneMaster.class))).thenReturn(entity);

            ShipTradelaneResponse response = service.update(1L, request);

            assertNotNull(response);
            assertEquals("TL001", response.getTradeLaneCode());
            verify(repository).save(any(ShipTradelaneMaster.class));
        }
    }

    @Test
    void testUpdateNullId() {
        assertThrows(ValidationException.class, () -> service.update(null, request));
    }

    @Test
    void testUpdateNotFound() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testUpdateDeletedRecord() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            entity.setDeleted("Y");
            when(repository.findById(1L)).thenReturn(Optional.of(entity));

            assertThrows(IllegalStateException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testGetByIdSuccess() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ShipTradelaneResponse response = service.getById(1L);

        assertNotNull(response);
        assertEquals("TL001", response.getTradeLaneCode());
    }

    @Test
    void testGetByIdNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void testDeleteSuccess() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(() -> getCurrentUser()).thenReturn("user2");
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(repository.save(any(ShipTradelaneMaster.class))).thenReturn(entity);

            service.delete(1L);

            verify(repository).save(any(ShipTradelaneMaster.class));
        }
    }

    @Test
    void testList() {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult rawResult = new RawSearchResult(
                Collections.emptyList(),
                Collections.emptyMap(),
                0L
        );

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());

        when(documentService.search(
                anyString(),
                anyList(),
                anyString(),
                any(Pageable.class),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(rawResult);

        Map<String, Object> result = service.list("100-012", filters, pageable);

        assertNotNull(result);
        verify(documentService).search(
                anyString(),
                anyList(),
                anyString(),
                any(Pageable.class),
                anyString(),
                anyString(),
                anyString()
        );
    }


    @Test
    void testValidateRequestEmptyCode() {
        request.setTradeLaneCode("");
        
        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void testValidateRequestEmptyName() {
        request.setTradeLaneName("");
        
        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void testValidateRequestInvalidRegion() {
        request.setRegionPoid(0L);
        
        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void testUpdateEmptyCode() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            request.setTradeLaneCode("");

            assertThrows(ValidationException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testUpdateEmptyName() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            request.setTradeLaneName("");

            assertThrows(ValidationException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testUpdateInvalidRegion() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            request.setRegionPoid(0L);

            assertThrows(ValidationException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testUpdateDuplicateCode() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            request.setTradeLaneCode("TL002");
            when(repository.existsByTradeLaneCodeExcluding("TL002", 1L)).thenReturn(true);

            assertThrows(DuplicateKeyException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testUpdateDuplicateName() {
        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(ASGHelperUtils::getGroupId).thenReturn(2L);
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            request.setTradeLaneName("Trade Lane 2");
            when(repository.existsByTradeLaneNameExcluding("Trade Lane 2", 1L)).thenReturn(true);

            assertThrows(DuplicateKeyException.class, () -> service.update(1L, request));
        }
    }

    @Test
    void testMapToResponseWithNullSeqNo() {
        entity.setSeqNo(null);
        when(repository.existsByTradeLaneCode(anyString())).thenReturn(false);
        when(repository.existsByTradeLaneName(anyString())).thenReturn(false);
        when(repository.save(any(ShipTradelaneMaster.class))).thenReturn(entity);

        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(() -> getGroupId()).thenReturn(2L);
            mockedStatic.when(() -> getCurrentUser()).thenReturn("user1");
            
            ShipTradelaneResponse response = service.create(request);
            
            assertNull(response.getSeqNo());
        }
    }

    @Test
    void testMapToResponseWithInactiveFlag() {
        entity.setActive("N");
        when(repository.existsByTradeLaneCode(anyString())).thenReturn(false);
        when(repository.existsByTradeLaneName(anyString())).thenReturn(false);
        when(repository.save(any(ShipTradelaneMaster.class))).thenReturn(entity);

        try (MockedStatic<ASGHelperUtils> mockedStatic = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedStatic.when(() -> getGroupId()).thenReturn(2L);
            mockedStatic.when(() -> getCurrentUser()).thenReturn("user1");
            
            request.setActive(false);
            ShipTradelaneResponse response = service.create(request);
            
            assertFalse(response.getActive());
        }
    }
}