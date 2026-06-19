package com.asg.shipping.linecommission;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import net.sf.jasperreports.engine.JasperReport;
import javax.sql.DataSource;
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
import jakarta.persistence.Query;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
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
    private DocumentDeleteService documentDeleteService;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
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
        request.setTransactionDate(LocalDate.now());
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
        userContextMock.when(UserContext::getGroupPoid).thenReturn(10L);
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
        when(documentService.resolveDateFilters(eq(filterRequest), eq("TRANSACTION_DATE"), isNull(), isNull())).thenReturn(List.of(filter));
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
                service.listLineCommissions("DOC-1", filterRequest, null, null, pageable);

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

    @Test
    void testGetById_TransactionPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.getById(null, 10L));
        assertTrue(ex.getMessage().contains("transactionPoid"));
    }

    @Test
    void testGetById_GroupPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.getById(1L, null));
        assertTrue(ex.getMessage().contains("groupPoid"));
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

    @Test
    void testCreate_InvalidPeriod() {
        request.setPeriodFrom(LocalDate.now().plusDays(3));
        request.setPeriodTo(LocalDate.now());
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("periodFrom"));
    }

    @Test
    void testCreate_DuplicateContainerTypeValidation() {
        ContainerRateDto a = new ContainerRateDto();
        a.setContainerTypePoid(10L);
        ContainerRateDto b = new ContainerRateDto();
        b.setContainerTypePoid(10L);
        request.setContainerRates(List.of(a, b));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("Duplicate container type"));
    }

    @Test
    void testCreate_ContainerTypeMissingValidation() {
        ContainerRateDto a = new ContainerRateDto();
        a.setContainerTypePoid(null);
        request.setContainerRates(List.of(a));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("Container Type is required"));
    }

    @Test
    void testCreate_SavesAllDetailCollections() {
        LineCommissionServiceImpl spy = spy(service);
        doNothing().when(spy).enrich(any(), any());

        ContainerRateDto c = new ContainerRateDto();
        c.setContainerTypePoid(10L);
        request.setContainerRates(List.of(c));
        request.setOtherRemunerations(List.of(new OtherRemunerationDto()));
        LocalShareDto localShare = new LocalShareDto();
        localShare.setChargePoid(99L);
        request.setLocalShares(List.of(localShare));

        ShipLineCommCntnrDtlEntity cnt = new ShipLineCommCntnrDtlEntity();
        cnt.setDetRowId(1L);
        ShipLineCommDtlEntity dtl = new ShipLineCommDtlEntity();
        dtl.setDetRowId(1L);
        ShipLineCommLocalDtlEntity local = new ShipLineCommLocalDtlEntity();
        local.setDetRowId(1L);

        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(mapper.toCreateHeaderEntity(request, 10L, 20L, "user1")).thenReturn(hdrEntity);
        when(hdrRepository.saveAndFlush(hdrEntity)).thenReturn(hdrEntity);
        doNothing().when(entityManager).refresh(hdrEntity);
        when(mapper.toContainerEntities(anyLong(), any(), any())).thenReturn(List.of(cnt));
        when(mapper.toOtherRemunerationEntities(anyLong(), any(), any())).thenReturn(List.of(dtl));
        when(mapper.toLocalShareEntities(anyLong(), any(), any())).thenReturn(List.of(local));
        when(cntnrRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dtlRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(localRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        LineCommissionResponse created = spy.create(request, 10L, "user1", "DOC-1");
        assertNotNull(created);
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
    void testUpdate_TransactionPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(null, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("transactionPoid"));
    }

    @Test
    void testUpdate_GroupPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, null, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("groupPoid"));
    }

    @Test
    void testUpdate_PeriodOverlap() {
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("overlaps"));
    }

    @Test
    void testUpdate_ContainerRateUpdateMissingDetRow() {
        ContainerRateDto dto = new ContainerRateDto();
        dto.setActionType("isUpdated");
        dto.setContainerTypePoid(10L);
        request.setContainerRates(List.of(dto));
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("detRowId is required"));
    }

    @Test
    void testUpdate_ContainerRateDeleteNotFound() {
        ContainerRateDto dto = new ContainerRateDto();
        dto.setActionType("isDeleted");
        dto.setDetRowId(99L);
        dto.setContainerTypePoid(10L);
        request.setContainerRates(List.of(dto));
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);
        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
    }

    @Test
    void testUpdate_OtherRemunerationDeleteMissingDetRow() {
        OtherRemunerationDto dto = new OtherRemunerationDto();
        dto.setActionType("isDeleted");
        request.setOtherRemunerations(List.of(dto));
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);
        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("detRowId is required"));
    }

    @Test
    void testUpdate_LocalShareUpdateNotFound() {
        LocalShareDto dto = new LocalShareDto();
        dto.setActionType("isUpdated");
        dto.setDetRowId(88L);
        dto.setChargePoid(11L);
        request.setLocalShares(List.of(dto));
        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);
        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(localRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request, 10L, "user1", "DOC-1"));
    }

    @Test
    void testUpdate_DetailUpdateAndDeleteSuccess() {
        LineCommissionServiceImpl spy = spy(service);
        doNothing().when(spy).enrich(any(), any());

        ContainerRateDto cUpd = new ContainerRateDto();
        cUpd.setActionType("isUpdated");
        cUpd.setDetRowId(1L);
        cUpd.setContainerTypePoid(10L);
        ContainerRateDto cDel = new ContainerRateDto();
        cDel.setActionType("isDeleted");
        cDel.setDetRowId(2L);
        cDel.setContainerTypePoid(20L);
        request.setContainerRates(List.of(cUpd, cDel));

        OtherRemunerationDto oUpd = new OtherRemunerationDto();
        oUpd.setActionType("isUpdated");
        oUpd.setDetRowId(1L);
        OtherRemunerationDto oDel = new OtherRemunerationDto();
        oDel.setActionType("isDeleted");
        oDel.setDetRowId(2L);
        request.setOtherRemunerations(List.of(oUpd, oDel));

        LocalShareDto lUpd = new LocalShareDto();
        lUpd.setActionType("isUpdated");
        lUpd.setDetRowId(1L);
        lUpd.setChargePoid(30L);
        LocalShareDto lDel = new LocalShareDto();
        lDel.setActionType("isDeleted");
        lDel.setDetRowId(2L);
        lDel.setChargePoid(31L);
        request.setLocalShares(List.of(lUpd, lDel));

        ShipLineCommCntnrDtlEntity cnt1 = new ShipLineCommCntnrDtlEntity();
        cnt1.setDetRowId(1L);
        ShipLineCommCntnrDtlEntity cnt2 = new ShipLineCommCntnrDtlEntity();
        cnt2.setDetRowId(2L);
        ShipLineCommDtlEntity dt1 = new ShipLineCommDtlEntity();
        dt1.setDetRowId(1L);
        ShipLineCommDtlEntity dt2 = new ShipLineCommDtlEntity();
        dt2.setDetRowId(2L);
        ShipLineCommLocalDtlEntity lc1 = new ShipLineCommLocalDtlEntity();
        lc1.setDetRowId(1L);
        ShipLineCommLocalDtlEntity lc2 = new ShipLineCommLocalDtlEntity();
        lc2.setDetRowId(2L);

        when(hdrRepository.countOverlapping(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(hdrEntity));
        when(hdrRepository.save(any())).thenReturn(hdrEntity);
        when(cntnrRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(cnt1, cnt2));
        when(dtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(dt1, dt2));
        when(localRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(lc1, lc2));
        when(cntnrRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dtlRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(localRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(cntnrRepository).deleteAll(any());
        doNothing().when(dtlRepository).deleteAll(any());
        doNothing().when(localRepository).deleteAll(any());
        doNothing().when(mapper).applyUpdateHeader(any(), any(), any(), any());
        doNothing().when(mapper).applyUpdateContainerEntity(any(), any(), any());
        doNothing().when(mapper).applyUpdateOtherRemunerationEntity(any(), any(), any());
        doNothing().when(mapper).applyUpdateLocalShareEntity(any(), any(), any());
        when(mapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        LineCommissionResponse out = spy.update(1L, request, 10L, "user1", "DOC-1");
        assertNotNull(out);
        verify(cntnrRepository).deleteAll(any());
        verify(dtlRepository).deleteAll(any());
        verify(localRepository).deleteAll(any());
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
                service.delete(1L, new DeleteReasonDto()));
        verify(documentDeleteService).deleteDocument(eq(1L), eq("SHIP_LINE_COMM_HDR"), eq("TRANSACTION_POID"), any(), any(LocalDate.class));
    }

    @Test
    void testDelete_TransactionMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.delete(null, new DeleteReasonDto()));
        assertTrue(ex.getMessage().contains("transactionPoid"));
    }

    @Test
    void testDelete_NotFound() {
        when(hdrRepository.findByTransactionPoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, new DeleteReasonDto()));
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

    @Test
    void testLoadContainerTypes_Success() {
        var master = new com.asg.shipping.containertypes.entity.ShipContainerTypeMaster();
        var dto = new com.asg.shipping.containertypes.dto.ContainerTypeDto();
        when(lineTypeRepository.findContainerTypeMastersByLine(1L)).thenReturn(List.of(master));
        when(mapper.toContainerTypeDtos(List.of(master))).thenReturn(List.of(dto));

        var result = service.loadContainerTypes(1L, 10L, "user1");

        assertEquals(1, result.size());
    }

    @Test
    void testEnrich_FillsLovDetails() {
        LineCommissionResponse r = new LineCommissionResponse();
        r.setLinePoid(10L);
        r.setCurrencyPoid(20L);
        ContainerRateDto cr = new ContainerRateDto();
        cr.setContainerTypePoid(30L);
        r.setContainerRates(List.of(cr));
        OtherRemunerationDto or = new OtherRemunerationDto();
        or.setRemunerationPoid(40L);
        or.setCurrencyPoid(20L);
        r.setOtherRemunerations(List.of(or));
        LocalShareDto ls = new LocalShareDto();
        ls.setChargePoid(50L);
        r.setLocalShares(List.of(ls));

        Query q1 = org.mockito.Mockito.mock(Query.class);
        Query q2 = org.mockito.Mockito.mock(Query.class);
        Query q3 = org.mockito.Mockito.mock(Query.class);
        Query q4 = org.mockito.Mockito.mock(Query.class);
        Query q5 = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(q1, q2, q3, q4, q5);
        when(q1.setParameter(anyString(), any())).thenReturn(q1);
        when(q2.setParameter(anyString(), any())).thenReturn(q2);
        when(q3.setParameter(anyString(), any())).thenReturn(q3);
        when(q4.setParameter(anyString(), any())).thenReturn(q4);
        when(q5.setParameter(anyString(), any())).thenReturn(q5);

        when(q1.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{10L, "L1", "Line One"}));
        when(q2.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{20L, "USD", "US Dollar"}));
        when(q3.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{30L, "20GP", "20GP-General"}));
        when(q4.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{50L, "CHG", "Charge"}));
        when(q5.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{40L, "REM", "Rem-IMP"}));

        service.enrich(r, 10L);

        assertNotNull(r.getLineDet());
        assertNotNull(r.getCurrencyDet());
        assertNotNull(r.getContainerRates().getFirst().getContainerTypeDet());
        assertNotNull(r.getOtherRemunerations().getFirst().getRemunerationDet());
        assertNotNull(r.getOtherRemunerations().getFirst().getCurrencyDet());
        assertNotNull(r.getLocalShares().getFirst().getChargeDet());
    }

    @Test
    void testEnrich_NullResponse_NoOp() {
        assertDoesNotThrow(() -> service.enrich(null, 10L));
    }
}

