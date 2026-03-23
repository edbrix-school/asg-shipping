package com.asg.shipping.mafitrailerdateupdateform.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.mafitrailerdateupdateform.dto.*;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtl;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtlId;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiHdr;
import com.asg.shipping.mafitrailerdateupdateform.repository.ShipBlMafiDtlRepository;
import com.asg.shipping.mafitrailerdateupdateform.repository.ShipBlMafiHdrRepository;
import com.asg.shipping.mafitrailerdateupdateform.repository.ShipReadOnlyRepository;
import com.asg.shipping.mafitrailerdateupdateform.util.MafiTrailerDateUpdateFormMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MafiTrailerDateUpdateFormServiceImplTest {

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private ShipBlMafiHdrRepository headerRepository;

    @Mock
    private ShipBlMafiDtlRepository detailRepository;

    @Mock
    private ShipReadOnlyRepository readOnlyRepository;

    @Mock
    private MafiTrailerDateUpdateFormMapper mapper;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private MafiTrailerDateUpdateFormServiceImpl service;

    private MockedStatic<UserContext> mockedUserContext;

    private ShipBlMafiHdr header;
    private ShipBlMafiDtl detail;

    @BeforeEach
    void setUp() {
        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
        mockedUserContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

        header = new ShipBlMafiHdr();
        header.setTransactionPoid(1L);
        header.setGroupPoid(10L);
        header.setCompanyPoid(20L);
        header.setDeleted("N");
        header.setVoyageTransactionPoid(99L);
        header.setAgentReference("AGENT1");
        header.setRemarks("OLD");

        ShipBlMafiDtlId id = new ShipBlMafiDtlId();
        id.setTransactionPoid(1L);
        id.setDetRowId(1L);

        detail = new ShipBlMafiDtl();
        detail.setId(id);
        detail.setBlPoid(100L);
        detail.setMafiRef("MAFI001");
        detail.setMafiSize(new java.math.BigDecimal("20"));
        detail.setMafiFreeDays(new java.math.BigDecimal("5"));
        detail.setRemarks("OLD");
        detail.setBackLoadDate(LocalDate.now());
        detail.setMafiEmptyDate(LocalDate.now());
    }

    @AfterEach
    void tearDown() {
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    /* ---------------- GET ALL ---------------- */

    @Test
    void getAll_success() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("TRANSACTION_POID", 1L, "JOB_NO", "JOB1")),
                Map.of("TRANSACTION_POID", "JOB_NO"),
                1L
        );

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("false");
        when(documentService.resolveFilters(filters)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                eq(pageable), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.getAll("DOC1", filters, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC1"), anyList(), eq("OR"), eq(pageable), eq("false"), eq("DOC_REF"), eq("JOB_NO"));
    }

    /* ---------------- GET BY ID ---------------- */

    @Test
    void getById_success() {
        VoyageProjection voyage = mock(VoyageProjection.class);
        MafiTrailerDateUpdateFormResponse response = new MafiTrailerDateUpdateFormResponse();

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.of(voyage));

        when(mapper.toMafiTrailerResponse(eq(header), eq(voyage), anyList()))
                .thenReturn(response);

        MafiTrailerDateUpdateFormResponse result = service.getById(1L, 10L, 20L);

        assertNotNull(result);
        verify(headerRepository).findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N");
        verify(detailRepository).findByIdTransactionPoidOrderByIdDetRowId(1L);
        verify(readOnlyRepository).findVoyageDetailsById(99L);
        verify(mapper).toMafiTrailerResponse(header, voyage, List.of(detail));
    }

    @Test
    void getById_headerNotFound_throwsException() {
        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L, 10L, 20L));

        assertTrue(exception.getMessage().contains("Mafi trailer entry not found"));
    }

    @Test
    void getById_voyageNotFound_throwsException() {
        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L, 10L, 20L));

        assertTrue(exception.getMessage().contains("Voyage not found"));
    }

    /* ---------------- UPDATE ---------------- */

    @Test
    void update_headerAndDetailChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(mapper).updateShipBlMafiHdr(any(ShipBlMafiHdr.class), eq(request), eq("admin"));
        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class), eq("admin"));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_onlyHeaderChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).updateByTransactionPoidAndDetRowId(anyLong(), anyLong(), anyString(), any(), any(), anyString());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_onlyDetailChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository, never()).updateByTransactionPoid(anyLong(), anyString(), anyString(), anyString());
        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class), eq("admin"));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_detailNotFoundInMap_skipped() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(999L); // Non-existent detail
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).updateByTransactionPoidAndDetRowId(eq(1L), eq(999L), anyString(), any(), any(), anyString());
    }

    @Test
    void update_onlyAgentReferenceChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
    }

    @Test
    void update_onlyRemarksChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("NEW");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
    }

    @Test
    void update_onlyBackLoadDateChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class), eq("admin"));
    }

    @Test
    void update_onlyMafiEmptyDateChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class), eq("admin"));
    }

    @Test
    void update_onlyDetailRemarksChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class), eq("admin"));
    }

    @Test
    void update_noChanges_throwsException() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.update(1L, request, 10L, 20L, "admin"));

        assertEquals("No data to update.", exception.getMessage());
        verify(headerRepository, never()).updateByTransactionPoid(anyLong(), anyString(), anyString(), anyString());
        verify(detailRepository, never()).updateByTransactionPoidAndDetRowId(anyLong(), anyLong(), anyString(), any(), any(), anyString());
    }

    @Test
    void update_headerNotFound_throwsException() {
        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(new MafitrailerHeaderDTO());
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.update(1L, request, 10L, 20L, "admin"));

        assertTrue(exception.getMessage().contains("Mafi trailer entry not found"));
    }

    /* ---------------- ADDITIONAL COVERAGE TESTS ---------------- */

    @Test
    void getAll_emptyResults_success() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult rawResult = new RawSearchResult(
                List.of(),
                Map.of(),
                0L
        );

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("false");
        when(documentService.resolveFilters(filters)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                eq(pageable), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.getAll("DOC1", filters, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC1"), anyList(), eq("OR"), eq(pageable), eq("false"), eq("DOC_REF"), eq("JOB_NO"));
    }

    @Test
    void update_multipleDetails_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDto detailDto1 = new MafiDetailDto();
        detailDto1.setDetRowId(1L);
        detailDto1.setRemarks("NEW_REMARKS_1");

        MafiDetailDto detailDto2 = new MafiDetailDto();
        detailDto2.setDetRowId(2L);
        detailDto2.setRemarks("NEW_REMARKS_2");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto1, detailDto2));

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        ShipBlMafiDtl detail2 = new ShipBlMafiDtl();
        ShipBlMafiDtlId id2 = new ShipBlMafiDtlId();
        id2.setTransactionPoid(1L);
        id2.setDetRowId(2L);
        detail2.setId(id2);
        detail2.setRemarks("OLD");

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail, detail2));

        service.update(1L, request, 10L, 20L, "admin");

        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(1L), eq("NEW_REMARKS_1"), any(), any(), eq("admin"));
        verify(detailRepository).updateByTransactionPoidAndDetRowId(eq(1L), eq(2L), eq("NEW_REMARKS_2"), any(), any(), eq("admin"));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_emptyDetails_onlyHeaderChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).updateByTransactionPoidAndDetRowId(anyLong(), anyLong(), anyString(), any(), any(), anyString());
    }

    @Test
    void update_noExistingDetails_onlyHeaderChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(
                1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of());

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(eq(1L), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).updateByTransactionPoidAndDetRowId(anyLong(), anyLong(), anyString(), any(), any(), anyString());
    }
}
