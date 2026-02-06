package com.asg.shipping.linecommission;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.entity.ShipLineCommHdrEntity;
import com.asg.shipping.linecommission.repository.ShipLineCommCntnrDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommHdrRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommLocalDtlRepository;
import com.asg.shipping.linecommission.service.LineCommissionServiceImpl;
import com.asg.shipping.linecommission.util.LineCommissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineCommissionServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ShipLineCommHdrRepository hdrRepository;
    @Mock
    private ShipLineCommCntnrDtlRepository cntnrRepository;
    @Mock
    private ShipLineCommDtlRepository dtlRepository;
    @Mock
    private ShipLineCommLocalDtlRepository localRepository;
    @Mock
    private ShipLineMasterTypeRepository lineTypeRepository;
    @Mock
    private LineCommissionMapper mapper;
    @Mock
    private LoggingService loggingService;


    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;


    @InjectMocks
    private LineCommissionServiceImpl service;

    private LineCommissionRequest request;
    private ShipLineCommHdrEntity hdrEntity;
    private LineCommissionResponse response;

    @BeforeEach
    void setup() {
        request = new LineCommissionRequest();
        request.setLinePoid(1L);
        request.setPeriodFrom(LocalDate.now());
        request.setPeriodTo(LocalDate.now().plusDays(10));

        hdrEntity = new ShipLineCommHdrEntity();
        hdrEntity.setTransactionPoid(1L);
        hdrEntity.setGroupPoid(10L);
        hdrEntity.setDeleted("N");

        response = new LineCommissionResponse();
        response.setTransactionPoid(1L);
    }

    // ---------------- LIST ----------------

    @Test
    void testListLineCommissions() {

        FilterDto filter = new FilterDto("GLOBALSEARCH", "MAERSK");
        FilterRequestDto filterRequest =
                new FilterRequestDto("OR", "N", List.of(filter));

        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("LINE_NAME", "MAERSK")),
                Map.of("LINE_NAME", "Line Name"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(
                anyString(),
                any(),
                eq("OR"),
                eq(pageable),
                eq("N"),
                eq("LINE_NAME"),
                eq("TRANSACTION_POID")
        )).thenReturn(raw);

        Map<String, Object> result =
                service.listLineCommissions("DOC-1", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    // ---------------- GET BY ID ----------------

    @Test
    void testGetById_Success() {

        LineCommissionServiceImpl spy = spy(service);

        doNothing().when(spy).enrich(any(), any());

        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(hdrEntity));

        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());

        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());

        when(localRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());

        when(mapper.toResponse(any(), any(), any(), any()))
                .thenReturn(response);

        LineCommissionResponse result = spy.getById(1L, 10L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
    }


    @Test
    void testGetById_NotFound() {

        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(1L, 10L));
    }

    // ---------------- CREATE ----------------

    @Test
    void testCreate_GroupPoidMissing() {

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.create(request, null, "user1", "DOC-1")
        );

        assertTrue(ex.getMessage().contains("groupPoid"));
    }

    // ---------------- UPDATE ----------------

    @Test
    void testUpdate_NotFound() {

        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
    }

    // ---------------- DELETE ----------------

    @Test
    void testSoftDelete_Success() {

        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(hdrEntity));

        assertDoesNotThrow(() ->
                service.delete(1L, 10L, "user1"));

        verify(hdrRepository).save(argThat(e ->
                "Y".equals(e.getDeleted())
        ));
    }

    // ---------------- LOAD CONTAINER TYPES ----------------

    @Test
    void testLoadContainerTypes_Empty() {

        when(lineTypeRepository.findContainerTypeMastersByLine(1L))
                .thenReturn(List.of());

        assertTrue(service.loadContainerTypes(1L, 10L, "user1").isEmpty());
    }

    @Test
    void testLoadContainerTypes_LineMissing() {

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.loadContainerTypes(null, 10L, "user1")
        );

        assertTrue(ex.getMessage().contains("Line"));
    }
}

