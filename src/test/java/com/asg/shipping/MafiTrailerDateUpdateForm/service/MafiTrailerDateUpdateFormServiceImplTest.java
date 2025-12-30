package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiDetailDto;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormResponse;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafitrailerHeaderDTO;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.VoyageProjection;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtl;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtlId;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiDtlRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiHdrRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipReadOnlyRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.util.MafiTrailerDateUpdateFormMapper;

import jakarta.persistence.EntityNotFoundException;

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

    @InjectMocks
    private MafiTrailerDateUpdateFormServiceImpl service;

    private ShipBlMafiHdr header;
    private ShipBlMafiDtl detail;

    @BeforeEach
    void setUp() {
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
        detail.setRemarks("OLD");
        detail.setBackLoadDate(LocalDate.now());
        detail.setMafiEmptyDate(LocalDate.now());
    }

    @Test
    void getAll_Success() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("TRANSACTION_POID", 1L, "JOB_NO", "JOB1")),
                Map.of("TRANSACTION_POID", "Transaction Id", "JOB_NO", "Job No"),
                1L
        );

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("false");
        when(documentService.resolveFilters(filters)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                eq(pageable), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        service.getAll("DOC1", filters, pageable);
    }

    @Test
    void getById_Success() {

        VoyageProjection voyage = mock(VoyageProjection.class);

        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.of(voyage));

        when(mapper.toMafiTrailerResponse(eq(header), eq(voyage), anyList()))
                .thenReturn(new MafiTrailerDateUpdateFormResponse());

        service.getById(1L, 10L, 20L);
    }

    @Test
    void getById_HeaderNotFound_Throws() {
        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L, 10L, 20L));
    }

    @Test
    void getById_VoyageNotFound_Throws() {
        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        when(readOnlyRepository.findVoyageDetailsById(99L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.getById(1L, 10L, 20L));
    }

    @Test
    void update_HeaderAndDetailUpdated_Success() {
        MafitrailerHeaderDTO headerDto = new MafitrailerHeaderDTO();
        headerDto.setAgentReference("NEW_AGENT");
        headerDto.setRemarks("NEW");

        MafiDetailDto detailDto = new MafiDetailDto();
        detailDto.setDetRowId(1L);
        detailDto.setRemarks("NEW");
        detailDto.setBackLoadDate(LocalDate.now().plusDays(1));
        detailDto.setMafiEmptyDate(LocalDate.now().plusDays(1));

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDto);
        request.setMafiDetails(List.of(detailDto));

        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        service.update(1L, request, 10L, 20L, "admin");

        verify(headerRepository).updateByTransactionPoid(
                eq(1L), eq("NEW_AGENT"), eq("NEW"), eq("admin"));

        verify(detailRepository).updateByTransactionPoidAndDetRowId(
                eq(1L), eq(1L), any(), any(), any(), eq("admin"));
    }

    @Test
    void update_NoChanges_Throws() {
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

        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.of(header));

        when(detailRepository.findByIdTransactionPoidOrderByIdDetRowId(1L))
                .thenReturn(List.of(detail));

        assertThrows(IllegalStateException.class,
                () -> service.update(1L, request, 10L, 20L, "admin"));
    }

    @Test
    void update_HeaderNotFound_Throws() {
        when(headerRepository
                .findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(1L, 10L, 20L, "N"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.update(1L, new MafiTrailerDateUpdateFormRequest(), 10L, 20L, "admin"));
    }
}
