package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.*;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtl;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiDtlRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiHdrRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipReadOnlyRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.util.MafiTrailerDateUpdateFormMapper;
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
import java.util.ArrayList;
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
        mockedUserContext.when(UserContext::getUserId).thenReturn("admin");

        header = new ShipBlMafiHdr();
        header.setTransactionPoid(1L);
        header.setGroupPoid(10L);
        header.setCompanyPoid(20L);
        header.setDeleted("N");
        header.setVoyageTransactionPoid(99L);
        header.setAgentReference("AGENT1");
        header.setRemarks("OLD");

        detail = new ShipBlMafiDtl();
        detail.setTransactionPoid(1L);
        detail.setDetRowId(1L);
        detail.setBlPoid(100L);
        detail.setMafiRef("MAFI001");
        detail.setMafiSize(new java.math.BigDecimal("20"));
        detail.setMafiFreeDays(new java.math.BigDecimal("5"));
        detail.setRemarks("OLD");
        detail.setBackLoadDate(LocalDate.now());
        detail.setMafiEmptyDate(LocalDate.now());

        VoyageProjection voyage = mock(VoyageProjection.class);
        lenient().when(readOnlyRepository.findVoyageDetailsById(99L)).thenReturn(Optional.of(voyage));
        lenient().when(mapper.toMafiTrailerResponse(any(), any(), any())).thenReturn(new MafiTrailerDateUpdateFormResponse());
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
        when(documentService.resolveDateFilters(any(), anyString(), isNull(), isNull())).thenReturn(new ArrayList<>());
        when(documentService.search(
                anyString(), anyList(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = service.getAll("DOC1", filters, pageable,null,null);

        assertNotNull(result);
        verify(documentService).search(eq("DOC1"), anyList(), eq("OR"), eq(pageable), eq("false"), eq("DOC_REF"), eq("JOB_NO"));
    }

    /* ---------------- GET BY ID ---------------- */

    @Test
    void getById_success() {
        VoyageProjection voyage = mock(VoyageProjection.class);
        MafiTrailerDateUpdateFormResponse response = new MafiTrailerDateUpdateFormResponse();

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.of(voyage));

        when(mapper.toMafiTrailerResponse(eq(header), eq(voyage), anyList()))
                .thenReturn(response);

        MafiTrailerDateUpdateFormResponse result = service.getById(1L);

        assertNotNull(result);
        verify(headerRepository).findByTransactionPoidAndDeleted(1L,  "N");
        verify(detailRepository).findByTransactionPoidOrderByDetRowId(1L);
        verify(readOnlyRepository).findVoyageDetailsById(99L);
        verify(mapper).toMafiTrailerResponse(header, voyage, List.of(detail));
    }

    @Test
    void getById_headerNotFound_throwsException() {
        when(headerRepository.findByTransactionPoidAndDeleted(
                1L, "N"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L));

        assertTrue(exception.getMessage().contains("Mafi trailer entry not found"));
    }

    @Test
    void getById_voyageNotFound_throwsException() {
        when(headerRepository.findByTransactionPoidAndDeleted(
                1L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L));

        assertTrue(exception.getMessage().contains("Voyage not found"));
    }

    /* ---------------- UPDATE ---------------- */

    @Test
    void update_headerAndDetailChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));
        detailDto.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(mapper).updateShipBlMafiHdr(any(ShipBlMafiHdr.class), eq(request), eq("admin"));
        verify(headerRepository).updateByTransactionPoid(eq(1L), any(LocalDate.class),anyString(), anyString(), eq("admin"));
        verify(detailRepository).save(any(ShipBlMafiDtl.class));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_onlyHeaderChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L), any(LocalDate.class),anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).save(any(ShipBlMafiDtl.class));
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_onlyDetailChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
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

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository, never()).updateByTransactionPoid(anyLong(),any(LocalDate.class), anyString(), anyString(), anyString());
        verify(detailRepository).save(any(ShipBlMafiDtl.class));
        verify(loggingService).createLogBatch(anyList());
        verify(loggingService).logChanges(any(), any(), eq(ShipBlMafiHdr.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
    }

    @Test
    void update_detailNotFoundInMap_skipped() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(999L); // Non-existent detail
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));
        detailDto.setActionType("NOACTION");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L),any(LocalDate.class), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_onlyAgentReferenceChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());
        detailDto.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L),any(LocalDate.class), anyString(), anyString(), eq("admin"));
    }

    @Test
    void update_onlyRemarksChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("NEW");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());
        detailDto.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L),any(LocalDate.class), anyString(), anyString(), eq("admin"));
    }

    @Test
    void update_onlyBackLoadDateChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
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

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(detailRepository).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_onlyMafiEmptyDateChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("OLD");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));
        detailDto.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(detailRepository).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_onlyDetailRemarksChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto = new MafiDetailDtoRequest();
        detailDto.setDetRowId(1L);
        detailDto.setBlPoid(100L);
        detailDto.setMafiRef("MAFI001");
        detailDto.setMafiSize(new java.math.BigDecimal("20"));
        detailDto.setMafiFreeDays(new java.math.BigDecimal("5"));
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(detail.getBackLoadDate());
        detailDto.setMafiEmptyDate(detail.getMafiEmptyDate());
        detailDto.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(detailRepository).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_noChanges_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndDeleted(1L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository, never()).updateByTransactionPoid(anyLong(),any(LocalDate.class), anyString(), anyString(), anyString());
        verify(detailRepository, never()).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_headerNotFound_throwsException() {
        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(new MafitrailerHeaderDTO());
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> service.update(1L, request));

        assertTrue(exception.getMessage().contains("Mafi trailer entry not found"));
    }

    /* ---------------- ADDITIONAL COVERAGE TESTS ---------------- */

    @Test
    void update_multipleDetails_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("AGENT1");
        headerDto.setRemarks("OLD");

        MafiDetailDtoRequest detailDto1 = new MafiDetailDtoRequest();
        detailDto1.setDetRowId(1L);
        detailDto1.setRemarks("NEW_REMARKS_1");
        detailDto1.setActionType("isCreated");

        MafiDetailDtoRequest detailDto2 = new MafiDetailDtoRequest();
        detailDto2.setDetRowId(2L);
        detailDto2.setRemarks("NEW_REMARKS_2");
        detailDto2.setActionType("isCreated");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto1, detailDto2));

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        ShipBlMafiDtl detail2 = new ShipBlMafiDtl();
        detail2.setTransactionPoid(1L);
        detail2.setDetRowId(2L);
        detail2.setRemarks("OLD");

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail, detail2));

        service.update(1L, request);

        verify(detailRepository, times(2)).save(any(ShipBlMafiDtl.class));
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

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L),any(LocalDate.class), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).save(any(ShipBlMafiDtl.class));
    }

    @Test
    void update_noExistingDetails_onlyHeaderChanged_success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of());

        when(headerRepository.findByTransactionPoidAndDeleted(
                1L,  "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());

        service.update(1L, request);

        verify(headerRepository).updateByTransactionPoid(eq(1L),any(LocalDate.class), anyString(), anyString(), eq("admin"));
        verify(detailRepository, never()).save(any(ShipBlMafiDtl.class));
    }
}
