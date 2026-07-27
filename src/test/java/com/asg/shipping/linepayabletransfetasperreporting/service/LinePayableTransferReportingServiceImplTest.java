package com.asg.shipping.linepayabletransfetasperreporting.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtl;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferHdr;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferDtlRepository;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferHdrRepository;
import com.asg.shipping.linepayabletransfetasperreporting.util.LinePayableTransferReportingMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LinePayableTransferReportingMapper mapper;

    @Mock
    private LovDataService lovDataService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private LinePayableTransferReportingServiceImpl service;

    private ShipLineReportTransferHdr testEntity;
    private LinePayableTransferReportingDto testDto;
    private LinePayableTransferReportingCreateDTO createDTO;
    private LinePayableTransferReportingUpdateDTO updateDTO;
    private ShipLineReportTransferDtl testDetailEntity;
    private LinePayableTransferReportingDtlDto testDetailDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        when(lovDataService.getDetailsByPoidsAndLovName(any(), any())).thenReturn(Collections.emptyMap());
        when(lovDataService.getDetailsByCodesAndLovName(any(), any())).thenReturn(Collections.emptyMap());

        // BL type is validated against the REPORT_CNT_BL_TYPE list of values
        LovGetListDto importType = new LovGetListDto();
        importType.setCode("IMPORT");
        LovGetListDto exportType = new LovGetListDto();
        exportType.setCode("EXPORT");
        when(lovDataService.getLovList(anyString(), any(), any(), any(), eq("REPORT_CNT_BL_TYPE"),
                anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Map.of("data", List.of(importType, exportType)));

        // Lines resolve and documents are found unless a test says otherwise
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);

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

        updateDTO = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .reportStartDate(LocalDate.of(2024, 2, 1))
                .reportEndDate(LocalDate.of(2024, 2, 28))
                .build();

        testDetailEntity = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .mainfestTransactionPoid(100L)
                .blNumber("BL123")
                .acutalAmount(BigDecimal.valueOf(1000))
                .totalAmountTransfer(BigDecimal.valueOf(1000))
                .isSelect("Y")
                .build();

        testDetailDto = LinePayableTransferReportingDtlDto.builder()
                .detRowId(1L)
                .mainfestTransactionPoid(100L)
                .blNumber("BL123")
                .acutalAmount(BigDecimal.valueOf(1000))
                .totalAmountTransfer(BigDecimal.valueOf(1000))
                .isSelect("Y")
                .build();

        // Create and update read the header back after PROC_SHIP_BL_PAGE_SAVE_AFTER has run
        when(hdrRepository.findActiveByTransactionPoid(anyLong())).thenReturn(Optional.of(testEntity));
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
        // VIEWED logging is done by the controller, not the service
        verifyNoInteractions(loggingService);
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
        doNothing().when(hdrRepository).flush();
        doNothing().when(entityManager).refresh(any());
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), anyLong(), anyLong());

        LinePayableTransferReportingDto result = service.createLinePayableTransfer(createDTO);

        assertNotNull(result);
        verify(hdrRepository).save(any());
        verify(hdrRepository).flush();
        verify(entityManager).refresh(any());
        verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq(LogDetailsEnum.CREATED.getDescription() + " LPT-2024-001"));
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
        verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq(LogDetailsEnum.MODIFIED.getDescription() + " LPT-2024-001"));
    }

    @Test
    void updateLinePayableTransfer_DetachesDeletedDetailsBeforeReinserting() {
        ShipLineReportTransferDtl existing = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .build();

        LinePayableTransferReportingUpdateDTO updateDTO = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .details(Collections.singletonList(testDetailDto))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Collections.singletonList(existing));
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, updateDTO);

        // The stale managed row must be detached after the bulk delete, otherwise re-saving
        // DET_ROW_ID 1 is flushed as an UPDATE of a deleted row
        InOrder inOrder = inOrder(dtlRepository, entityManager);
        inOrder.verify(dtlRepository).deleteByTransactionPoid(1L);
        inOrder.verify(entityManager).detach(existing);
        inOrder.verify(dtlRepository).save(any());
    }

    @Test
    void updateLinePayableTransfer_LogsHeaderBeforeDetails() {
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .details(Collections.singletonList(testDetailDto))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Collections.emptyList());
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, dto);

        InOrder inOrder = inOrder(loggingService, dtlRepository);
        inOrder.verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq(LogDetailsEnum.MODIFIED.getDescription() + " LPT-2024-001"));
        inOrder.verify(loggingService).logDetails(any(), any(), eq(ShipLineReportTransferHdr.class),
                any(), eq("1"), eq("TRANSACTION_POID"));
        inOrder.verify(dtlRepository).save(any());
        inOrder.verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("Row Created on Line Payable Transfer Detail with detRowId: 1"));
    }

    @Test
    void createLinePayableTransfer_LogsHeaderBeforeDetails() {
        createDTO.setDetails(Collections.singletonList(testDetailDto));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.existsByDocRef(anyString(), any())).thenReturn(false);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        doNothing().when(hdrRepository).flush();
        doNothing().when(entityManager).refresh(any());
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(dtlRepository.findMaxDetRowIdByTransactionPoid(anyLong())).thenReturn(0L);
        when(dtlRepository.save(any())).thenReturn(testDetailEntity);
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), anyLong(), anyLong());

        service.createLinePayableTransfer(createDTO);

        InOrder inOrder = inOrder(loggingService, dtlRepository);
        inOrder.verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq(LogDetailsEnum.CREATED.getDescription() + " LPT-2024-001"));
        inOrder.verify(dtlRepository).save(any());
        inOrder.verify(loggingService).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("Row Created on Line Payable Transfer Detail with detRowId: 1"));
    }

    @Test
    void updateLinePayableTransfer_FilterUnchanged_LogsFieldDifferencesOfExistingRows() {
        ShipLineReportTransferDtl stored = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .blNumber("BL123")
                .acutalAmount(BigDecimal.valueOf(1000))
                .totalAmountTransfer(BigDecimal.valueOf(1000))
                .isSelect("Y")
                .build();

        ShipLineReportTransferDtl incoming = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .blNumber("BL123")
                .acutalAmount(BigDecimal.valueOf(1000))
                .totalAmountTransfer(BigDecimal.valueOf(750))
                .isSelect("N")
                .build();

        // Same line, BL type and report dates as the stored header
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .details(Collections.singletonList(testDetailDto))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Collections.singletonList(stored));
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(incoming);
        when(dtlRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, dto);

        // Rows are edited in place, not wiped and reinserted
        verify(dtlRepository, never()).deleteByTransactionPoid(anyLong());
        verify(loggingService, never()).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("Row Created on Line Payable Transfer Detail with detRowId: 1"));

        ArgumentCaptor<ShipLineReportTransferDtl> oldRow = ArgumentCaptor.forClass(ShipLineReportTransferDtl.class);
        ArgumentCaptor<ShipLineReportTransferDtl> newRow = ArgumentCaptor.forClass(ShipLineReportTransferDtl.class);
        verify(loggingService).createLog(oldRow.capture(), newRow.capture(), eq(ShipLineReportTransferDtl.class),
                eq("100-432"), eq("1"), eq("KeyId = DET_ROW_ID:1"));

        // The snapshot must hold the pre-update values, the saved row the payload values
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(oldRow.getValue().getTotalAmountTransfer()));
        assertEquals("Y", oldRow.getValue().getIsSelect());
        assertEquals(0, BigDecimal.valueOf(750).compareTo(newRow.getValue().getTotalAmountTransfer()));
        assertEquals("N", newRow.getValue().getIsSelect());
    }

    @Test
    void updateLinePayableTransfer_FilterUnchanged_DeletesRowsMissingFromPayload() {
        ShipLineReportTransferDtl kept = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L).detRowId(1L).isSelect("Y").build();
        ShipLineReportTransferDtl dropped = ShipLineReportTransferDtl.builder()
                .transactionPoid(1L).detRowId(2L).isSelect("Y").build();

        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .details(Collections.singletonList(testDetailDto))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(Arrays.asList(kept, dropped));
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(dtlRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, dto);

        verify(dtlRepository).delete(dropped);
        verify(dtlRepository, never()).delete(kept);
        verify(loggingService).logDelete(dropped, "100-432", "1");
    }

    @Test
    void createLinePayableTransfer_LogsEachDetailRowIndividually() {
        createDTO.setDetails(Arrays.asList(testDetailDto, testDetailDto, testDetailDto));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.existsByDocRef(anyString(), any())).thenReturn(false);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        doNothing().when(hdrRepository).flush();
        doNothing().when(entityManager).refresh(any());
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(dtlRepository.findMaxDetRowIdByTransactionPoid(anyLong())).thenReturn(0L);
        when(dtlRepository.save(any())).thenReturn(testDetailEntity);
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), anyLong(), anyLong());

        service.createLinePayableTransfer(createDTO);

        verify(dtlRepository, times(3)).save(any());
        verify(loggingService, times(3)).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("Row Created on Line Payable Transfer Detail with detRowId: 1"));
    }

    @Test
    void updateLinePayableTransfer_LogsChangedHeaderFields() {
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        // Mimic the real mapper so the entity actually changes between snapshot and save
        doAnswer(invocation -> {
            LinePayableTransferReportingUpdateDTO source = invocation.getArgument(0);
            ShipLineReportTransferHdr target = invocation.getArgument(1);
            target.setBlType(source.getBlType());
            return null;
        }).when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, dto);

        ArgumentCaptor<ShipLineReportTransferHdr> oldCaptor = ArgumentCaptor.forClass(ShipLineReportTransferHdr.class);
        verify(loggingService).logDetails(oldCaptor.capture(), eq(testEntity), eq(ShipLineReportTransferHdr.class),
                eq("100-432"), eq("1"), eq("TRANSACTION_POID"));

        // The snapshot must hold the pre-update value, otherwise the diff would always be empty
        assertEquals("IMPORT", oldCaptor.getValue().getBlType());
        assertEquals("EXPORT", testEntity.getBlType());
    }

    @Test
    void updateLinePayableTransfer_LogsEachDetailRowIndividually() {
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .details(Arrays.asList(testDetailDto, testDetailDto))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(dtlRepository.save(any())).thenReturn(testDetailEntity);
        when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());
        doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

        service.updateLinePayableTransfer(1L, dto);

        verify(dtlRepository, times(2)).save(any());
        verify(loggingService, times(2)).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("Row Created on Line Payable Transfer Detail with detRowId: 1"));
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

    @Test
    void loadDataByDateRange_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .chargeFilter("FRTTHC")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
            doNothing().when(dtlRepository).deleteByTransactionPoid(1L);
            when(dtlRepository.findMaxDetRowIdByTransactionPoid(1L)).thenReturn(0L);
            when(dtlRepository.save(any())).thenReturn(testDetailEntity);
            when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);

            CallableStatement cs = mock(CallableStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true).thenReturn(false);
            when(rs.getObject("MAINFEST_TRANSACTION_POID")).thenReturn(100L);
            when(rs.getString("BL_NUMBER")).thenReturn("BL123");
            when(rs.getObject("ACUTAL_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
            when(rs.getObject("CHARGE_POID")).thenReturn(1L);
            when(rs.getString("FREIGHT_TYPE")).thenReturn("PREPAID");
            when(rs.getString("CURRENCY_CODE")).thenReturn("USD");
            when(rs.getObject("CURRENCY_EXCHANGE")).thenReturn(BigDecimal.ONE);
            when(rs.getObject("CURRENCY_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
            when(cs.getObject(8)).thenReturn(rs);

            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
                CallableStatementCallback<?> callback = invocation.getArgument(1);
                return callback.doInCallableStatement(cs);
            });

            List<LinePayableTransferReportingDtlDto> result = service.loadDataByDateRange(1L, request);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(dtlRepository).deleteByTransactionPoid(1L);
            verify(cs).setString(7, "FRTTHC");
        }
    }

    @Test
    void createLinePayableTransfer_LineNotFound() {
        LinePayableTransferReportingCreateDTO dto = LinePayableTransferReportingCreateDTO.builder()
                .linePoid(9999L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        assertThrows(ValidationException.class, () -> service.createLinePayableTransfer(dto));
    }

    @Test
    void createLinePayableTransfer_MissingLinePoid() {
        LinePayableTransferReportingCreateDTO dto = LinePayableTransferReportingCreateDTO.builder()
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        assertThrows(ValidationException.class, () -> service.createLinePayableTransfer(dto));
    }

    @Test
    void updateLinePayableTransfer_InvalidBlType() {
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .blType("INVALID")
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));

        assertThrows(ValidationException.class, () -> service.updateLinePayableTransfer(1L, dto));
    }

    @Test
    void updateLinePayableTransfer_InvalidDateRange() {
        LinePayableTransferReportingUpdateDTO dto = LinePayableTransferReportingUpdateDTO.builder()
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));

        assertThrows(ValidationException.class, () -> service.updateLinePayableTransfer(1L, dto));
    }

    @Test
    void searchLinePayableTransfer_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                Collections.emptyList(),
                Collections.emptyMap(),
                0L
        );

        when(documentSearchService.resolveOperator(any())).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
        when(documentSearchService.resolveDateFilters(any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.searchLinePayableTransfer("100-432", filterRequest, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), pageable);

        assertNotNull(result);
        verify(documentSearchService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void processWeeklyBlReport_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(dtlRepository).deleteByTransactionPoid(1L);
            when(dtlRepository.save(any())).thenReturn(testDetailEntity);
            when(mapper.mapDtlFromDto(any(), anyLong(), anyLong())).thenReturn(testDetailEntity);

            CallableStatement cs = mock(CallableStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true).thenReturn(false);
            when(rs.getObject("BL_POID")).thenReturn(100L);
            when(rs.getString("BL_NUMBER")).thenReturn("BL123");
            when(rs.getObject("manifest_amount")).thenReturn(BigDecimal.valueOf(1000));
            when(rs.getObject("chargeamount_local")).thenReturn(BigDecimal.valueOf(1000));
            when(rs.getObject("WKYRPT_INCLUDE_POID")).thenReturn(1L);
            when(rs.getString("BL_TYPE")).thenReturn("IMPORT");
            when(rs.getString("CURRENCY_CODE")).thenReturn("USD");
            when(rs.getObject("CURRENCY_EXCHANGE")).thenReturn(BigDecimal.ONE);
            when(rs.getObject("THC_AMOUNT")).thenReturn(BigDecimal.valueOf(100));
            when(cs.getObject(5)).thenReturn(rs);

            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
                CallableStatementCallback<?> callback = invocation.getArgument(1);
                return callback.doInCallableStatement(cs);
            });

            List<LinePayableTransferReportingDtlDto> result = service.processWeeklyBlReport(1L, request);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(dtlRepository).deleteByTransactionPoid(1L);
        }
    }

    @Test
    void processWeeklyBlReport_InvalidDateRange() {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> service.processWeeklyBlReport(1L, request));
    }

    @Test
    void loadDataBeforeCreate_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .chargeFilter("OTHERS")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            CallableStatement cs = mock(CallableStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true).thenReturn(false);
            when(rs.getObject("MAINFEST_TRANSACTION_POID")).thenReturn(100L);
            when(rs.getString("BL_NUMBER")).thenReturn("BL123");
            when(rs.getObject("ACUTAL_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
            when(rs.getObject("CHARGE_POID")).thenReturn(1L);
            when(rs.getString("FREIGHT_TYPE")).thenReturn("PREPAID");
            when(rs.getString("CURRENCY_CODE")).thenReturn("USD");
            when(rs.getObject("CURRENCY_EXCHANGE")).thenReturn(BigDecimal.ONE);
            when(rs.getObject("CURRENCY_AMOUNT")).thenReturn(BigDecimal.valueOf(1000));
            when(cs.getObject(8)).thenReturn(rs);

            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
                CallableStatementCallback<?> callback = invocation.getArgument(1);
                return callback.doInCallableStatement(cs);
            });

            List<LinePayableTransferReportingDtlDto> result = service.loadDataBeforeCreate(request);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(cs).setString(7, "OTHERS");
        }
    }

    @Test
    void loadDataBeforeCreate_InvalidDateRange() {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> service.loadDataBeforeCreate(request));
    }

    @Test
    void processWeeklyBeforeCreate_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getObject("BL_POID")).thenReturn(100L);
        when(rs.getString("BL_NUMBER")).thenReturn("BL123");
        when(rs.getObject("manifest_amount")).thenReturn(BigDecimal.valueOf(1000));
        when(rs.getObject("chargeamount_local")).thenReturn(BigDecimal.valueOf(1000));
        when(rs.getObject("WKYRPT_INCLUDE_POID")).thenReturn(1L);
        when(rs.getString("BL_TYPE")).thenReturn("IMPORT");
        when(rs.getString("CURRENCY_CODE")).thenReturn("USD");
        when(rs.getObject("CURRENCY_EXCHANGE")).thenReturn(BigDecimal.ONE);
        when(rs.getObject("THC_AMOUNT")).thenReturn(BigDecimal.valueOf(100));
        when(cs.getObject(5)).thenReturn(rs);

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
            CallableStatementCallback<?> callback = invocation.getArgument(1);
            return callback.doInCallableStatement(cs);
        });

        List<LinePayableTransferReportingDtlDto> result = service.processWeeklyBeforeCreate(request);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void processWeeklyBeforeCreate_InvalidDateRange() {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .reportStartDate(LocalDate.of(2024, 1, 31))
                .reportEndDate(LocalDate.of(2024, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> service.processWeeklyBeforeCreate(request));
    }

    @Test
    void deleteLinePayableTransfer_WithDeleteReason() {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Test deletion");

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(documentDeleteService.deleteDocument(any(), any(), any(), any(), any())).thenReturn(null);

        service.deleteLinePayableTransfer(1L, deleteReason);

        verify(hdrRepository).save(argThat(entity -> "Y".equals(entity.getDeleted())));
        verify(documentDeleteService).deleteDocument(any(), any(), any(), eq(deleteReason), any());
    }

    @Test
    void getLinePayableTransferById_WithDetails() {
        List<ShipLineReportTransferDtl> details = Arrays.asList(testDetailEntity);
        List<LinePayableTransferReportingDtlDto> detailDtos = Arrays.asList(testDetailDto);

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(mapper.mapToDto(testEntity)).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(details);
        when(mapper.mapDtlListToDto(details)).thenReturn(detailDtos);
        when(jdbcTemplate.queryForMap(anyString(), anyLong())).thenReturn(Map.of("LINE_NAME", "Test Line", "LINE_CODE", "TL"));

        LinePayableTransferReportingDto result = service.getLinePayableTransferById(1L);

        assertNotNull(result);
        assertNotNull(result.getDetails());
        assertEquals(1, result.getDetails().size());
    }

    // ==================== Charge filter ====================

    private CallableStatement stubEmptyReportLineDatewiseCursor() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);
        when(cs.getObject(8)).thenReturn(rs);
        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
            CallableStatementCallback<?> callback = invocation.getArgument(1);
            return callback.doInCallableStatement(cs);
        });
        return cs;
    }

    private LoadDataByDateRangeRequest.LoadDataByDateRangeRequestBuilder validLoadRequest() {
        return LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31));
    }

    /**
     * Load the given request and assert which charge filter reached the procedure
     */
    private void assertChargeFilterSent(LoadDataByDateRangeRequest request, String expected) throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            CallableStatement cs = stubEmptyReportLineDatewiseCursor();
            service.loadDataBeforeCreate(request);
            verify(cs).setString(7, expected);
        }
    }

    @Test
    void loadDataBeforeCreate_DefaultsToAllFilter() throws Exception {
        assertChargeFilterSent(validLoadRequest().build(), "ALL");
    }

    @Test
    void loadDataBeforeCreate_SendsFrtThcFilter() throws Exception {
        assertChargeFilterSent(validLoadRequest().chargeFilter("FRTTHC").build(), "FRTTHC");
    }

    @Test
    void loadDataBeforeCreate_SendsOthersFilter() throws Exception {
        assertChargeFilterSent(validLoadRequest().chargeFilter("OTHERS").build(), "OTHERS");
    }

    @Test
    void loadDataBeforeCreate_BlankFilterFallsBackToAll() throws Exception {
        assertChargeFilterSent(validLoadRequest().chargeFilter("  ").build(), "ALL");
    }

    @Test
    void loadDataBeforeCreate_NormalisesFilterCase() throws Exception {
        assertChargeFilterSent(validLoadRequest().chargeFilter("others").build(), "OTHERS");
    }

    @Test
    void loadDataBeforeCreate_RejectsUnknownChargeFilter() {
        LoadDataByDateRangeRequest request = validLoadRequest().chargeFilter("SOMETHING").build();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.loadDataBeforeCreate(request));
        assertEquals("Charge filter must be one of: ALL, FRTTHC, OTHERS", ex.getMessage());
    }

    // ==================== Legacy messages ====================

    @Test
    void loadDataBeforeCreate_NullCursorReportsNoDataFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            CallableStatement cs = mock(CallableStatement.class);
            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
                CallableStatementCallback<?> callback = invocation.getArgument(1);
                return callback.doInCallableStatement(cs);
            });

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.loadDataBeforeCreate(validLoadRequest().build()));
            assertEquals("No Data Found...", ex.getMessage());
        }
    }

    @Test
    void loadDataBeforeCreate_ProcedureFailureReportsLegacyMessage() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class)))
                    .thenThrow(new IllegalStateException("ORA-06550"));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.loadDataBeforeCreate(validLoadRequest().build()));
            assertEquals("Some error occured while loading data, please check the log...", ex.getMessage());
        }
    }

    @Test
    void loadDataBeforeCreate_LineNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.loadDataBeforeCreate(validLoadRequest().build()));
        assertEquals("Line not found: 1123", ex.getMessage());
    }

    // ==================== Apply New Exchange Rate ====================

    @Test
    void applyExchangeRate_RecalculatesRowsOfTheGivenCurrency() {
        LinePayableTransferReportingDtlDto usdRow = LinePayableTransferReportingDtlDto.builder()
                .currencyCode("USD")
                .currencyAmount(BigDecimal.valueOf(100))
                .currencyExchange(BigDecimal.valueOf(3))
                .acutalAmount(BigDecimal.valueOf(300))
                .totalAmountTransfer(BigDecimal.valueOf(300))
                .build();

        List<LinePayableTransferReportingDtlDto> result = service.applyExchangeRate(
                ApplyExchangeRateRequest.builder()
                        .currencyCode("usd")
                        .currencyExchange(BigDecimal.valueOf(3.75))
                        .details(new ArrayList<>(List.of(usdRow)))
                        .build());

        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.valueOf(3.75).compareTo(result.get(0).getCurrencyExchange()));
        assertEquals(0, BigDecimal.valueOf(375).compareTo(result.get(0).getAcutalAmount()));
        assertEquals(0, BigDecimal.valueOf(375).compareTo(result.get(0).getTotalAmountTransfer()));
    }

    @Test
    void applyExchangeRate_LeavesOtherCurrenciesUntouched() {
        LinePayableTransferReportingDtlDto eurRow = LinePayableTransferReportingDtlDto.builder()
                .currencyCode("EUR")
                .currencyAmount(BigDecimal.valueOf(100))
                .currencyExchange(BigDecimal.valueOf(4))
                .acutalAmount(BigDecimal.valueOf(400))
                .totalAmountTransfer(BigDecimal.valueOf(400))
                .build();

        List<LinePayableTransferReportingDtlDto> result = service.applyExchangeRate(
                ApplyExchangeRateRequest.builder()
                        .currencyCode("USD")
                        .currencyExchange(BigDecimal.valueOf(3.75))
                        .details(new ArrayList<>(List.of(eurRow)))
                        .build());

        assertEquals(0, BigDecimal.valueOf(4).compareTo(result.get(0).getCurrencyExchange()));
        assertEquals(0, BigDecimal.valueOf(400).compareTo(result.get(0).getAcutalAmount()));
    }

    @Test
    void applyExchangeRate_RowWithoutCurrencyAmountKeepsItsAmounts() {
        LinePayableTransferReportingDtlDto row = LinePayableTransferReportingDtlDto.builder()
                .currencyCode("USD")
                .acutalAmount(BigDecimal.valueOf(300))
                .totalAmountTransfer(BigDecimal.valueOf(300))
                .build();

        List<LinePayableTransferReportingDtlDto> result = service.applyExchangeRate(
                ApplyExchangeRateRequest.builder()
                        .currencyCode("USD")
                        .currencyExchange(BigDecimal.valueOf(3.75))
                        .details(new ArrayList<>(List.of(row)))
                        .build());

        assertEquals(0, BigDecimal.valueOf(300).compareTo(result.get(0).getAcutalAmount()));
        assertEquals(0, BigDecimal.valueOf(3.75).compareTo(result.get(0).getCurrencyExchange()));
    }

    // ==================== LOV enrichment ====================

    /**
     * The three LOV lookups run on worker threads. LovDataService resolves the tenant from
     * UserContext, a plain ThreadLocal, and PROC_LOV_GETLIST silently falls back to group 1 /
     * company 1 / user 0 rather than failing, so losing the context here would return another
     * tenant's descriptions with no error anywhere.
     */
    @Test
    void loadDataBeforeCreate_LovLookupsSeeTheCallersUserContext() throws Exception {
        CustomAuthDetails caller = CustomAuthDetails.builder()
                .groupPoid(77L)
                .companyPoid(88L)
                .userPoid(99L)
                .build();

        Set<Long> groupsSeenByWorkers = ConcurrentHashMap.newKeySet();
        Set<Long> companiesSeenByWorkers = ConcurrentHashMap.newKeySet();

        when(lovDataService.getDetailsByPoidsAndLovName(any(), any())).thenAnswer(invocation -> {
            groupsSeenByWorkers.add(UserContext.getGroupPoid());
            companiesSeenByWorkers.add(UserContext.getCompanyPoid());
            return Collections.emptyMap();
        });
        when(lovDataService.getDetailsByCodesAndLovName(any(), any())).thenAnswer(invocation -> {
            groupsSeenByWorkers.add(UserContext.getGroupPoid());
            companiesSeenByWorkers.add(UserContext.getCompanyPoid());
            return Collections.emptyMap();
        });

        UserContext.setCurrentUser(caller);
        try {
            CallableStatement cs = mock(CallableStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true).thenReturn(false);
            when(rs.getObject("TRANSACTION_POID")).thenReturn(100L);
            when(rs.getObject("CHARGE_POID")).thenReturn(1L);
            when(rs.getString("CURRENCY_CODE")).thenReturn("USD");
            when(cs.getObject(8)).thenReturn(rs);
            when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenAnswer(invocation -> {
                CallableStatementCallback<?> callback = invocation.getArgument(1);
                return callback.doInCallableStatement(cs);
            });

            service.loadDataBeforeCreate(validLoadRequest().build());
        } finally {
            UserContext.clear();
        }

        assertEquals(Set.of(77L), groupsSeenByWorkers, "workers must see the caller's group, not the fallback");
        assertEquals(Set.of(88L), companiesSeenByWorkers, "workers must see the caller's company, not the fallback");
    }

    // ==================== DocumentAfterSave post processing ====================

    @Test
    void createLinePayableTransfer_RunsAfterSaveProcedure() {
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());

        service.createLinePayableTransfer(createDTO);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sql.capture(), any(CallableStatementCallback.class));
        assertTrue(sql.getValue().contains("PROC_SHIP_BL_PAGE_SAVE_AFTER"));

        // The procedure is PRAGMA AUTONOMOUS_TRANSACTION, so the rows must be committed before it
        // runs, otherwise it cannot see them
        InOrder inOrder = inOrder(transactionManager, jdbcTemplate);
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(jdbcTemplate).execute(anyString(), any(CallableStatementCallback.class));
    }

    @Test
    void updateLinePayableTransfer_RunsAfterSaveProcedure() {
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(testDto);
        when(dtlRepository.findByTransactionPoid(anyLong())).thenReturn(Collections.emptyList());
        when(mapper.mapDtlListToDto(anyList())).thenReturn(Collections.emptyList());

        service.updateLinePayableTransfer(1L, updateDTO);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sql.capture(), any(CallableStatementCallback.class));
        assertTrue(sql.getValue().contains("PROC_SHIP_BL_PAGE_SAVE_AFTER"));

        InOrder inOrder = inOrder(transactionManager, jdbcTemplate);
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(jdbcTemplate).execute(anyString(), any(CallableStatementCallback.class));
    }

    @Test
    void createLinePayableTransfer_AfterSaveFailureSurfacesWithTheDocumentAlreadySaved() {
        when(hdrRepository.save(any())).thenReturn(testEntity);
        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class)))
                .thenThrow(new IllegalStateException("ORA-20001"));

        assertThrows(ValidationException.class, () -> service.createLinePayableTransfer(createDTO));

        // Legacy behaves the same way: the document is committed and only the hook reports an error
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }
}
