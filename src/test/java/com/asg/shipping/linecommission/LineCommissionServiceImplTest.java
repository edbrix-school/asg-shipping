package com.asg.shipping.linecommission;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.linecommission.dto.ContainerRateDto;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.dto.LocalShareDto;
import com.asg.shipping.linecommission.dto.OtherRemunerationDto;
import com.asg.shipping.linecommission.entity.ShipLineCommCntnrDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommHdrEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommLocalDtlEntity;
import com.asg.shipping.linecommission.repository.ShipLineCommCntnrDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommHdrRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommLocalDtlRepository;
import com.asg.shipping.linecommission.service.LineCommissionServiceImpl;
import com.asg.shipping.linecommission.util.LineCommissionMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private EntityManager entityManager;

    @InjectMocks
    private LineCommissionServiceImpl service;

    private LineCommissionRequest request;
    private ShipLineCommHdrEntity hdrEntity;
    private LineCommissionResponse response;
    private MockedStatic<UserContext> userContextMock;

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

        userContextMock = org.mockito.Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(20L);
        userContextMock.when(UserContext::getDocumentId).thenReturn("DOC-1");
        userContextMock.when(UserContext::getUserId).thenReturn("user1");
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
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

    @Test
    void testCreate_Success() {
        LineCommissionServiceImpl spy = spy(service);
        doNothing().when(spy).enrich(any(), any());

        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(mapper.toCreateHeaderEntity(request, 10L, 20L, "user1")).thenReturn(hdrEntity);
        when(hdrRepository.saveAndFlush(hdrEntity)).thenReturn(hdrEntity);
        doNothing().when(entityManager).refresh(hdrEntity);
        when(mapper.toContainerEntities(anyLong(), any(), any())).thenReturn(List.of());
        when(mapper.toOtherRemunerationEntities(anyLong(), any(), any())).thenReturn(List.of());
        when(mapper.toLocalShareEntities(anyLong(), any(), any())).thenReturn(List.of());
        when(mapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        LineCommissionResponse created = spy.create(request, 10L, "user1", "DOC-1");
        assertNotNull(created);
        verify(hdrRepository).saveAndFlush(hdrEntity);
    }

    @Test
    void testCreate_PeriodOverlap() {
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("overlaps"));
    }

    // ---------------- UPDATE ----------------

    @Test
    void testUpdate_NotFound() {

        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
    }

    @Test
    void testUpdate_DuplicateContainerTypeValidation() {
        ContainerRateDto r1 = new ContainerRateDto();
        r1.setContainerTypePoid(100L);
        ContainerRateDto r2 = new ContainerRateDto();
        r2.setContainerTypePoid(100L);
        request.setContainerRates(List.of(r1, r2));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("Duplicate container type"));
    }

    @Test
    void testUpdate_SuccessWithDetailActions() {
        LineCommissionServiceImpl spy = spy(service);
        doNothing().when(spy).enrich(any(), any());

        ContainerRateDto rate = new ContainerRateDto();
        rate.setContainerTypePoid(99L);
        request.setContainerRates(List.of(rate));
        request.setOtherRemunerations(List.of(new OtherRemunerationDto()));
        request.setLocalShares(List.of(new LocalShareDto()));

        ShipLineCommCntnrDtlEntity existingCnt = new ShipLineCommCntnrDtlEntity();
        existingCnt.setDetRowId(1L);
        ShipLineCommDtlEntity existingDtl = new ShipLineCommDtlEntity();
        existingDtl.setDetRowId(1L);
        ShipLineCommLocalDtlEntity existingLocal = new ShipLineCommLocalDtlEntity();
        existingLocal.setDetRowId(1L);

        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);

        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(existingCnt));
        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(existingDtl));
        when(localRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(existingLocal));
        when(cntnrRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dtlRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(localRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(mapper.toNewContainerEntity(anyLong(), anyLong(), any(), any())).thenReturn(existingCnt);
        when(mapper.toNewOtherRemunerationEntity(anyLong(), anyLong(), any(), any())).thenReturn(existingDtl);
        when(mapper.toNewLocalShareEntity(anyLong(), anyLong(), any(), any())).thenReturn(existingLocal);
        doNothing().when(mapper).applyUpdateHeader(any(), any(), any(), any());
        when(mapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        LineCommissionResponse updated = spy.update(1L, request, 10L, "user1", "DOC-1");
        assertNotNull(updated);
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

    @Test
    void testLoadContainerTypes_GroupMissing() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.loadContainerTypes(1L, null, "user1")
        );
        assertTrue(ex.getMessage().contains("groupPoid"));
    }
}

