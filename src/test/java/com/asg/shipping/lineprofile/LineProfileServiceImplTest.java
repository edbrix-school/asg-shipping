package com.asg.shipping.lineprofile;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileContactDto;
import com.asg.shipping.lineprofile.dto.LineProfileLineDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import com.asg.shipping.lineprofile.entity.ShipLineProfileContactDtlEntity;
import com.asg.shipping.lineprofile.entity.ShipLineProfileMasterEntity;
import com.asg.shipping.lineprofile.repository.ShipLineProfileContactDtlRepository;
import com.asg.shipping.lineprofile.repository.ShipLineProfileMasterRepository;
import com.asg.shipping.lineprofile.service.LineProfileServiceImpl;
import com.asg.shipping.lineprofile.util.LineProfileMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_CREATED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_DELETED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_UPDATED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_NO_CHANGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineProfileServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ShipLineProfileMasterRepository masterRepository;
    @Mock
    private ShipLineProfileContactDtlRepository contactRepository;
    @Mock
    private LineProfileMapper mapper;
    @Mock
    private EntityManager entityManager;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService deleteService;

    @InjectMocks
    private LineProfileServiceImpl service;

    private LineProfileRequest request;
    private ShipLineProfileMasterEntity masterEntity;
    private LineProfileResponse response;

    @BeforeEach
    void setup() {
        request = new LineProfileRequest();
        request.setLinePoid(10L);
        request.setRegionPoids(List.of());
        request.setRemarks("Remarks");
        request.setAgreementPoid(null);
        request.setActive("Y");

        masterEntity = new ShipLineProfileMasterEntity();
        masterEntity.setLineProfilePoid(1L);
        masterEntity.setGroupPoid(10L);
        masterEntity.setLinePoid(null);
        masterEntity.setAgreementPoid(null);
        masterEntity.setRegion(null);
        masterEntity.setActive("Y");
        masterEntity.setDeleted("N");

        response = new LineProfileResponse();
        response.setLineProfilePoid(1L);
        response.setRegionPoids(new ArrayList<>());
        response.setContactDetails(new ArrayList<>());
    }

    @Test
    void testListLineProfiles() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "MAERSK");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
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
                eq("LINE_PROFILE_POID")
        )).thenReturn(raw);

        Map<String, Object> result = service.listLineProfiles("DOC-1", filterRequest, pageable);
        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetById_Success() {
        stubEmptyRegionQuery();
        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.toResponse(masterEntity, List.of()))
                .thenReturn(response);

        LineProfileResponse result = service.getById(1L, 10L);
        assertNotNull(result);
        assertEquals(1L, result.getLineProfilePoid());
    }

    @Test
    void testGetById_NotFound() {
        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L, 10L));
    }

    @Test
    void testGetById_LineProfilePoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.getById(null, 10L));
        assertTrue(ex.getMessage().contains("lineProfilePoid"));
    }

    @Test
    void testGetById_GroupPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.getById(1L, null));
        assertTrue(ex.getMessage().contains("groupPoid"));
    }

    @Test
    void testCreate_Success() {
        stubEmptyRegionQuery();
        when(mapper.toCreateEntity(request, 10L, "user1"))
                .thenReturn(masterEntity);
        when(masterRepository.saveAndFlush(masterEntity))
                .thenReturn(masterEntity);
        doNothing().when(entityManager).refresh(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.toResponse(masterEntity, List.of()))
                .thenReturn(response);

        LineProfileResponse result = service.create(request, 10L, "user1", "DOC-1");
        assertNotNull(result);
        verify(masterRepository).saveAndFlush(masterEntity);
    }

    @Test
    void testCreate_GroupPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, null, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("groupPoid"));
    }

    @Test
    void testCreate_UserIdMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(request, 10L, null, "DOC-1"));
        assertTrue(ex.getMessage().contains("userId"));
    }

    @Test
    void testUpdate_Success() {
        stubEmptyRegionQuery();
        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        doNothing().when(mapper).applyUpdate(masterEntity, request, "user1");
        when(masterRepository.save(masterEntity))
                .thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.toResponse(masterEntity, List.of()))
                .thenReturn(response);

        LineProfileResponse result = service.update(1L, request, 10L, "user1", "DOC-1");
        assertNotNull(result);
        verify(masterRepository).save(masterEntity);
    }

    @Test
    void testUpdate_LineProfilePoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(null, request, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("lineProfilePoid"));
    }

    @Test
    void testUpdate_GroupPoidMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, null, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("groupPoid"));
    }

    @Test
    void testUpdate_UserMissing() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, request, 10L, null, "DOC-1"));
        assertTrue(ex.getMessage().contains("userId"));
    }

    @Test
    void testUpdate_WithContactActions() {
        stubEmptyRegionQuery();
        LineProfileContactDto createDto = new LineProfileContactDto(null, ACTION_IS_CREATED,
                "Contact1", "Mgr", "123", "111", "a@b.com");
        LineProfileContactDto updateDto = new LineProfileContactDto(1L, ACTION_IS_UPDATED,
                "Contact2", "Lead", "234", "222", "c@d.com");
        LineProfileContactDto deleteDto = new LineProfileContactDto(2L, ACTION_IS_DELETED,
                "Contact3", "Dir", "345", "333", "e@f.com");

        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(createDto, updateDto, deleteDto));

        ShipLineProfileContactDtlEntity existing1 = new ShipLineProfileContactDtlEntity();
        existing1.setLineProfilePoid(1L);
        existing1.setDetRowId(1L);
        ShipLineProfileContactDtlEntity existing2 = new ShipLineProfileContactDtlEntity();
        existing2.setLineProfilePoid(1L);
        existing2.setDetRowId(2L);

        ShipLineProfileContactDtlEntity created = new ShipLineProfileContactDtlEntity();
        created.setLineProfilePoid(1L);
        created.setDetRowId(3L);

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        doNothing().when(mapper).applyUpdate(masterEntity, updateRequest, "user1");
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of(existing1, existing2));
        when(mapper.toNewContactEntity(eq(1L), eq(3L), eq(createDto), eq("user1")))
                .thenReturn(created);
        doNothing().when(mapper).applyUpdateContactEntity(eq(existing1), eq(updateDto), eq("user1"));
        when(contactRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(contactRepository).deleteAll(any());
        when(mapper.toResponse(masterEntity, List.of(existing1, existing2)))
                .thenReturn(response);

        LineProfileResponse result = service.update(1L, updateRequest, 10L, "user1", "DOC-1");
        assertNotNull(result);
        verify(contactRepository).deleteAll(any());
    }

    @Test
    void testUpdate_WithNoChangeContactAction() {
        stubEmptyRegionQuery();
        LineProfileContactDto noChangeDto = new LineProfileContactDto(1L, ACTION_NO_CHANGE,
                "Contact1", "Mgr", "123", "111", "a@b.com");
        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(noChangeDto));

        ShipLineProfileContactDtlEntity existing = new ShipLineProfileContactDtlEntity();
        existing.setLineProfilePoid(1L);
        existing.setDetRowId(1L);

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        doNothing().when(mapper).applyUpdate(masterEntity, updateRequest, "user1");
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of(existing));
        when(mapper.toResponse(masterEntity, List.of(existing))).thenReturn(response);

        LineProfileResponse result = service.update(1L, updateRequest, 10L, "user1", "DOC-1");
        assertNotNull(result);
    }

    @Test
    void testUpdate_ContactUpdateMissingDetRow() {
        LineProfileContactDto updateDto = new LineProfileContactDto(null, ACTION_IS_UPDATED,
                "Contact2", "Lead", "234", "222", "c@d.com");
        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(updateDto));

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L)).thenReturn(List.of());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, updateRequest, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("detRowId is required"));
    }

    @Test
    void testUpdate_ContactDeleteMissingDetRow() {
        LineProfileContactDto deleteDto = new LineProfileContactDto(null, ACTION_IS_DELETED,
                "Contact3", "Dir", "345", "333", "e@f.com");
        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(deleteDto));

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L)).thenReturn(List.of());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(1L, updateRequest, 10L, "user1", "DOC-1"));
        assertTrue(ex.getMessage().contains("detRowId is required"));
    }

    @Test
    void testUpdate_ContactUpdateNotFound() {
        LineProfileContactDto updateDto = new LineProfileContactDto(99L, ACTION_IS_UPDATED,
                "Contact2", "Lead", "234", "222", "c@d.com");
        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(updateDto));

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, updateRequest, 10L, "user1", "DOC-1"));
    }

    @Test
    void testUpdate_ContactDeleteNotFound() {
        LineProfileContactDto deleteDto = new LineProfileContactDto(99L, ACTION_IS_DELETED,
                "Contact3", "Dir", "345", "333", "e@f.com");
        LineProfileRequest updateRequest = new LineProfileRequest();
        updateRequest.setLinePoid(10L);
        updateRequest.setContactDetails(List.of(deleteDto));

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));
        when(masterRepository.save(masterEntity)).thenReturn(masterEntity);
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, updateRequest, 10L, "user1", "DOC-1"));
    }

    @Test
    void testDelete_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");

        when(masterRepository.findByLineProfilePoidAndGroupPoid(eq(1L), any()))
                .thenReturn(Optional.of(masterEntity));

        when(deleteService.deleteDocument(
                eq(1L),
                eq("SH_LINE_PROFILE_MASTER"),
                eq("LINE_PROFILE_POID"),
                eq(deleteReasonDto),
                isNull()
        )).thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.delete(1L, deleteReasonDto));

        verify(deleteService).deleteDocument(
                eq(1L),
                eq("SH_LINE_PROFILE_MASTER"),
                eq("LINE_PROFILE_POID"),
                eq(deleteReasonDto),
                isNull()
        );
    }

    @Test
    void testDelete_NotFound() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(masterRepository.findByLineProfilePoidAndGroupPoid(eq(1L), any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, deleteReasonDto));
    }

    @Test
    void testFetchLineDetails_Success() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getObject("LINE_POID")).thenReturn(10L);
        when(rs.getString("LINE_CODE")).thenReturn("LINE01");
        when(rs.getString("LINE_NAME")).thenReturn("MAERSK");
        when(rs.getObject("COUNTRY_POID")).thenReturn(100L);
        when(rs.getString("COUNTRY_CODE")).thenReturn("US");
        when(rs.getString("COUNTRY_NAME")).thenReturn("United States");
        when(rs.getString("MIS_LINE_CATEGORY")).thenReturn("MLO");

        LineProfileLineDetailsResponse result =
                service.fetchLineDetails(10L, 1L, 2L, 3L);

        assertNotNull(result);
        assertEquals(10L, result.getLinePoid());
        assertEquals(100L, result.getCountryPoid());
        assertNotNull(result.getCountryDet());
        assertEquals(1L, result.getAgencyPoid());
        assertNotNull(result.getAgencyTypeDet());
    }

    @Test
    void testFetchLineDetails_NvocAgencyType() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getObject("LINE_POID")).thenReturn(10L);
        when(rs.getString("LINE_CODE")).thenReturn("LINE01");
        when(rs.getString("LINE_NAME")).thenReturn("MAERSK");
        when(rs.getObject("COUNTRY_POID")).thenReturn(100L);
        when(rs.getString("COUNTRY_CODE")).thenReturn("US");
        when(rs.getString("COUNTRY_NAME")).thenReturn("United States");
        when(rs.getString("MIS_LINE_CATEGORY")).thenReturn("NVOC");

        LineProfileLineDetailsResponse result = service.fetchLineDetails(10L, 1L, 2L, 3L);

        assertNotNull(result);
        assertEquals(2L, result.getAgencyPoid());
    }

    @Test
    void testFetchLineDetails_BlankAgencyType_NoAgencySet() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getObject("LINE_POID")).thenReturn(10L);
        when(rs.getString("LINE_CODE")).thenReturn("LINE01");
        when(rs.getString("LINE_NAME")).thenReturn("MAERSK");
        when(rs.getObject("COUNTRY_POID")).thenReturn(null);
        when(rs.getString("MIS_LINE_CATEGORY")).thenReturn(" ");

        LineProfileLineDetailsResponse result = service.fetchLineDetails(10L, 1L, 2L, 3L);

        assertNotNull(result);
        assertNull(result.getAgencyPoid());
    }

    @Test
    void testGetById_EnrichesRegionLineAndAgreementDetails() throws Exception {
        ShipLineProfileMasterEntity master = new ShipLineProfileMasterEntity();
        master.setLineProfilePoid(1L);
        master.setGroupPoid(10L);

        LineProfileResponse mapped = new LineProfileResponse();
        mapped.setLineProfilePoid(1L);
        mapped.setRegionPoids(List.of(1L));
        mapped.setLinePoid(10L);
        mapped.setAgreementPoid(200L);

        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(master));
        when(contactRepository.findByLineProfilePoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.toResponse(master, List.of())).thenReturn(mapped);

        Query q = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(q);
        when(q.setParameter(eq("poids"), any())).thenReturn(q);
        when(q.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{1L, "RG", "Region One"}));

        StoredProcedureQuery spLine = mock(StoredProcedureQuery.class);
        ResultSet rsLine = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE")).thenReturn(spLine);
        when(spLine.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spLine);
        when(spLine.setParameter(anyString(), any())).thenReturn(spLine);
        when(spLine.execute()).thenReturn(true);
        when(spLine.getOutputParameterValue("OUTDATA")).thenReturn(rsLine);
        when(rsLine.next()).thenReturn(true);
        when(rsLine.getObject("LINE_POID")).thenReturn(10L);
        when(rsLine.getString("LINE_CODE")).thenReturn("L-1");
        when(rsLine.getString("LINE_NAME")).thenReturn("Line One");
        when(rsLine.getObject("COUNTRY_POID")).thenReturn(5L);
        when(rsLine.getString("COUNTRY_CODE")).thenReturn("AE");
        when(rsLine.getString("COUNTRY_NAME")).thenReturn("UAE");
        when(rsLine.getString("MIS_LINE_CATEGORY")).thenReturn("MLO");

        StoredProcedureQuery spAgr = mock(StoredProcedureQuery.class);
        ResultSet rsAgr = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_CONTRACTS_AGREEMENT")).thenReturn(spAgr);
        when(spAgr.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spAgr);
        when(spAgr.setParameter(anyString(), any())).thenReturn(spAgr);
        when(spAgr.execute()).thenReturn(true);
        when(spAgr.getOutputParameterValue("OUTDATA")).thenReturn(rsAgr);
        when(rsAgr.next()).thenReturn(true);
        when(rsAgr.getObject("TRANSACTION_POID")).thenReturn(200L);
        when(rsAgr.getString("AGREEMENT_ID")).thenReturn("AGR-200");
        when(rsAgr.getString("AGREEMENT_TYPE")).thenReturn("MASTER");
        when(rsAgr.getString("AGREEMENT_STATUS")).thenReturn("ACTIVE");
        when(rsAgr.getObject("EFFECTIVE_DATE")).thenReturn(Date.valueOf(LocalDate.now()));
        when(rsAgr.getObject("EXPIRY_DATE")).thenReturn(Date.valueOf(LocalDate.now().plusDays(1)));
        when(rsAgr.getString("RENEWAL_CYCLE")).thenReturn("YEARLY");

        LineProfileResponse out = service.getById(1L, 10L);

        assertNotNull(out);
        assertNotNull(out.getRegionDet());
        assertEquals(1, out.getRegionDet().size());
        assertNotNull(out.getLineDetails());
        assertNotNull(out.getAgreementDetails());
    }

    @Test
    void testFetchLineDetails_NotFound() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        LineProfileLineDetailsResponse result =
                service.fetchLineDetails(10L, 1L, 2L, 3L);
        assertNull(result);
    }

    @Test
    void testFetchAgreementDetails_Success() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_CONTRACTS_AGREEMENT"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getObject("TRANSACTION_POID")).thenReturn(200L);
        when(rs.getString("AGREEMENT_ID")).thenReturn("AGR-1");
        when(rs.getString("AGREEMENT_TYPE")).thenReturn("MASTER");
        when(rs.getString("AGREEMENT_STATUS")).thenReturn("ACTIVE");
        when(rs.getObject("EFFECTIVE_DATE")).thenReturn(Date.valueOf(LocalDate.now()));
        when(rs.getObject("EXPIRY_DATE")).thenReturn(Date.valueOf(LocalDate.now().plusDays(30)));
        when(rs.getString("RENEWAL_CYCLE")).thenReturn("YEARLY");

        LineProfileAgreementDetailsResponse result =
                service.fetchAgreementDetails(200L, 1L, 2L, 3L);

        assertNotNull(result);
        assertEquals(200L, result.getAgreementPoid());
        assertEquals("AGR-1", result.getAgreementId());
    }

    @Test
    void testFetchAgreementDetails_NotFound() throws Exception {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        ResultSet rs = mock(ResultSet.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_CONTRACTS_AGREEMENT"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenReturn(true);
        when(spQuery.getOutputParameterValue("OUTDATA")).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        LineProfileAgreementDetailsResponse result =
                service.fetchAgreementDetails(200L, 1L, 2L, 3L);
        assertNull(result);
    }

    @Test
    void testFetchLineDetails_Error() {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenThrow(new RuntimeException("proc error"));

        assertThrows(ValidationException.class,
                () -> service.fetchLineDetails(10L, 1L, 2L, 3L));
    }

    @Test
    void testFetchAgreementDetails_Error() {
        StoredProcedureQuery spQuery = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("PROC_SH_CONTRACTS_AGREEMENT"))
                .thenReturn(spQuery);
        when(spQuery.registerStoredProcedureParameter(anyString(), any(), any()))
                .thenReturn(spQuery);
        when(spQuery.setParameter(anyString(), any()))
                .thenReturn(spQuery);
        when(spQuery.execute()).thenThrow(new RuntimeException("proc error"));

        assertThrows(ValidationException.class,
                () -> service.fetchAgreementDetails(200L, 1L, 2L, 3L));
    }

    private void stubEmptyRegionQuery() {
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("poids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
    }
}

