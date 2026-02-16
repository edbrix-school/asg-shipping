package com.asg.shipping.lineprofile;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void testDelete_Success() {
        when(masterRepository.findByLineProfilePoidAndGroupPoid(1L, 10L))
                .thenReturn(Optional.of(masterEntity));

        assertDoesNotThrow(() -> service.delete(1L, 10L, "user1"));
        verify(masterRepository).save(any());
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

