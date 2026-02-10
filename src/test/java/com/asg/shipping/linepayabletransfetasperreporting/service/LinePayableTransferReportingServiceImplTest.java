package com.asg.shipping.linepayabletransfetasperreporting.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtl;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferHdr;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferDtlRepository;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferHdrRepository;
import com.asg.shipping.linepayabletransfetasperreporting.util.LinePayableTransferReportingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinePayableTransferReportingServiceImplTest {

    @Mock
    private ShipLineReportTransferHdrRepository hdrRepository;

    @Mock
    private ShipLineReportTransferDtlRepository dtlRepository;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LinePayableTransferReportingMapper mapper;

    @InjectMocks
    private LinePayableTransferReportingServiceImpl service;

    private ShipLineReportTransferHdr testEntity;
    private LinePayableTransferReportingDto testDto;
    private LinePayableTransferReportingCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        testEntity = ShipLineReportTransferHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .companyPoid(1L)
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .docRef("LPT-2024-001")
                .deleted("N")
                .build();

        testDto = LinePayableTransferReportingDto.builder()
                .transactionPoid(1L)
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .docRef("LPT-2024-001")
                .build();

        createDTO = LinePayableTransferReportingCreateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .docRef("LPT-2024-001")
                .build();
    }

    @Test
    void getLinePayableTransferById_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(mapper.mapToDto(testEntity)).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());

        LinePayableTransferReportingDto result = service.getLinePayableTransferById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        verify(hdrRepository).findActiveByTransactionPoid(1L);
    }

    @Test
    void getLinePayableTransferById_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getLinePayableTransferById(1L));
    }

    @Test
    void createLinePayableTransfer_Success() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.existsByDocRef(anyString(), any())).thenReturn(false);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), anyLong(), anyLong());

        LinePayableTransferReportingDto result = service.createLinePayableTransfer(createDTO);

        assertNotNull(result);
        verify(hdrRepository).save(any());
    }

    @Test
    void createLinePayableTransfer_InvalidBlType() {
        LinePayableTransferReportingCreateDTO invalidDTO = LinePayableTransferReportingCreateDTO.builder()
                .linePoid(1123L)
                .blType("INVALID")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);

        assertThrows(ValidationException.class, () -> service.createLinePayableTransfer(invalidDTO));
    }

    @Test
    void createLinePayableTransfer_InvalidDateRange() {
        LinePayableTransferReportingCreateDTO invalidDTO = LinePayableTransferReportingCreateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);

        assertThrows(ValidationException.class, () -> service.createLinePayableTransfer(invalidDTO));
    }

    @Test
    void updateLinePayableTransfer_Success() {
        LinePayableTransferReportingUpdateDTO updateDTO = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        LinePayableTransferReportingDto result = service.updateLinePayableTransfer(1L, updateDTO);

        assertNotNull(result);
        verify(hdrRepository).save(any());
    }

    @Test
    void updateLinePayableTransfer_NotFound() {
        LinePayableTransferReportingUpdateDTO updateDTO = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateLinePayableTransfer(1L, updateDTO));
    }

    @Test
    void deleteLinePayableTransfer_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(documentDeleteService.deleteDocument(any(), any(), any(), any(), any())).thenReturn(null);

        service.deleteLinePayableTransfer(1L, null);

        verify(hdrRepository).save(argThat(entity -> "Y".equals(entity.getDeleted())));
    }

    @Test
    void deleteLinePayableTransfer_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteLinePayableTransfer(1L, null));
    }

    @Test
    void loadDataByDateRange_InvalidDateRange() {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> service.loadDataByDateRange(1L, request));
    }

    @Test
    void loadDataByDateRange_LineNotFound() {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        assertThrows(ValidationException.class, () -> service.loadDataByDateRange(1L, request));
    }
}
