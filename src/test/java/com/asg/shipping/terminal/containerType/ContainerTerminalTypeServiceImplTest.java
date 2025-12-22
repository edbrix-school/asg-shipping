package com.asg.shipping.terminal.containerType;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import com.asg.shipping.terminal.containertype.entity.ContainerTerminalTypeEntity;
import com.asg.shipping.terminal.containertype.reposiory.ContainerTerminalTypeRepository;
import com.asg.shipping.terminal.containertype.service.impl.ContainerTerminalTypeServiceImpl;
import com.asg.shipping.terminal.containertype.util.ContainerTerminalTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerTerminalTypeServiceImplTest {

    @Mock
    private ContainerTerminalTypeRepository repository;

    @Mock
    private ContainerTerminalTypeMapper mapper;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ContainerTerminalTypeServiceImpl service;

    private ContainerTerminalTypeEntity entity;
    private ContainerTerminalTypeRequest request;
    private ContainerTerminalTypeResponse response;

    @BeforeEach
    void setup() {
        request = new ContainerTerminalTypeRequest();
        request.setContainerTerminalTypeCode("ISO20");
        request.setContainerTerminalTypeName("Twenty Feet");
        request.setContainerTerminalTypeSize(20L);
        request.setActive("Y");

        entity = ContainerTerminalTypeEntity.builder()
                .containerTerminalTypePoid(1L)
                .groupPoid(10L)
                .containerTerminalTypeCode("ISO20")
                .containerTerminalTypeName("Twenty Feet")
                .containerTerminalTypeSize(String.valueOf(20))
                .seqno(BigInteger.valueOf(10))
                .active("Y")
                .deleted("N")
                .createdBy("SYSTEM")
                .createdDate(LocalDateTime.now())
                .build();

        response = new ContainerTerminalTypeResponse();
        response.setContainerTerminalTypePoid(1L);
        response.setContainerTerminalTypeCode("ISO20");
        response.setContainerTerminalTypeName("Twenty Feet");
        response.setContainerTerminalTypeSize(20L);
        response.setActive("Y");
    }

    // ---------- LIST ----------

    @Test
    void testListContainerTerminalTypes() {

        // ----- GIVEN -----
        FilterDto filter = new FilterDto("GLOBALSEARCH", "ISO20");

        FilterRequestDto filterRequest =
                new FilterRequestDto("OR", "N", List.of(filter));

        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records =
                List.of(Map.of("CONTAINER_TMNL_TYPE_CODE", "ISO20"));

        Map<String, String> displayFields =
                Map.of("CONTAINER_TMNL_TYPE_CODE", "Container Code");

        RawSearchResult raw = new RawSearchResult(
                records,
                displayFields,
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(
                eq("DOC-1"),
                any(),
                eq("OR"),
                eq(pageable),
                eq("N"),
                eq("CONTAINER_TMNL_TYPE_NAME"),
                eq("CONTAINER_TMNL_TYPE_POID")
        )).thenReturn(raw);

        // ----- WHEN -----
        Map<String, Object> result =
                service.listContainerTerminalTypes("DOC-1", filterRequest, pageable);

        // ----- THEN -----
        assertNotNull(result);
        verify(documentService).search(
                eq("DOC-1"),
                any(),
                eq("OR"),
                eq(pageable),
                eq("N"),
                any(),
                any()
        );
    }


    // ---------- GET BY ID ----------
    @Test
    void testGetById_Success() {
        when(repository.findByContainerTerminalTypePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        ContainerTerminalTypeResponse result = service.getById(1L, 10L);

        assertNotNull(result);
        assertEquals("ISO20", result.getContainerTerminalTypeCode());
    }

    @Test
    void testGetById_NotFound() {
        when(repository.findByContainerTerminalTypePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(1L, 10L));
    }

    // ---------- CREATE ----------
    @Test
    void testCreate_Success() {
        when(repository.existsByContainerTerminalTypeCodeAndGroupPoid("ISO20", 10L))
                .thenReturn(false);
        when(repository.existsByContainerTerminalTypeNameAndGroupPoid("Twenty Feet", 10L))
                .thenReturn(false);
        when(mapper.toEntity(request, 10L, "user1")).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);
        // Mock logging service void methods
        doNothing().when(loggingService)
                .createLogSummaryEntry(
                        eq(LogDetailsEnum.CREATED),
                        eq("000-001"),
                        eq("1")
                );

        doNothing().when(loggingService)
                .logChanges(
                        isNull(),
                        any(ContainerTerminalTypeEntity.class),
                        eq(ContainerTerminalTypeEntity.class),
                        eq("000-001"),
                        eq("1"),
                        eq(LogDetailsEnum.CREATED),
                        eq("CONTAINER_TMNL_TYPE_POID")
                );

        ContainerTerminalTypeResponse result =
                service.create(request, 10L, "user1", "000-001");

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void testCreate_DuplicateCode() {
        when(repository.existsByContainerTerminalTypeCodeAndGroupPoid("ISO20", 10L))
                .thenReturn(true);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(request, 10L, "user1", "000-001")
        );

        assertTrue(ex.getMessage().contains("code already exists"));
    }

    @Test
    void testCreate_DuplicateName() {
        when(repository.existsByContainerTerminalTypeCodeAndGroupPoid("ISO20", 10L))
                .thenReturn(false);
        when(repository.existsByContainerTerminalTypeNameAndGroupPoid("Twenty Feet", 10L))
                .thenReturn(true);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(request, 10L, "user1", "000-001")
        );

        assertTrue(ex.getMessage().contains("name already exists"));
    }

    // ---------- UPDATE ----------
    @Test
    void testUpdate_Success() {
        when(repository.findByContainerTerminalTypePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);
        // Mock logging service void methods
        doNothing().when(loggingService)
                .createLogSummaryEntry(
                        eq(LogDetailsEnum.MODIFIED),
                        eq("000-001"),
                        eq("1")
                );

        doNothing().when(loggingService)
                .logChanges(
                        any(ContainerTerminalTypeEntity.class),
                        any(ContainerTerminalTypeEntity.class),
                        eq(ContainerTerminalTypeEntity.class),
                        eq("000-001"),
                        eq("1"),
                        eq(LogDetailsEnum.MODIFIED),
                        eq("CONTAINER_TMNL_TYPE_POID")
                );

        ContainerTerminalTypeResponse result =
                service.update(1L, request, 10L, "user1", "000-001");

        assertNotNull(result);
        verify(repository).save(entity);
    }

    @Test
    void testUpdate_DuplicateName() {
        // existing entity name
        entity.setContainerTerminalTypeName("Twenty Feet");

        // new request name (different)
        request.setContainerTerminalTypeName("Forty Feet");

        when(repository.findByContainerTerminalTypePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));

        when(repository.existsByContainerTerminalTypeNameAndGroupPoid("Forty Feet", 10L))
                .thenReturn(true);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.update(1L, request, 10L, "user1", "000-001")
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }



    // ---------- DELETE ----------
    @Test
    void testSoftDelete() {
        when(repository.findByContainerTerminalTypePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(entity));

        assertDoesNotThrow(() ->
                service.delete(1L, 10L, "user1"));

        verify(repository).save(argThat(e ->
                "Y".equals(e.getDeleted()) && "N".equals(e.getActive())));
    }


}

