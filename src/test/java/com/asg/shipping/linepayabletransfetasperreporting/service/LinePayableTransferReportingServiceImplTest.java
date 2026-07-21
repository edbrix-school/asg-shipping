package com.asg.shipping.linepayabletransfetasperreporting.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

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
        verify(loggingService).createLogSummaryEntry(ArgumentMatchers.<String>isNull(), eq("1"),
                eq(LogDetailsEnum.VIEWED.getDescription() + " LPT-2024-001"));
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
        verify(loggingService).createLogSummaryEntry(ArgumentMatchers.<String>isNull(), eq("1"),
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
        verify(loggingService).createLogSummaryEntry(ArgumentMatchers.<String>isNull(), eq("1"),
                eq(LogDetailsEnum.MODIFIED.getDescription() + " LPT-2024-001"));
    }

    @Test
    void createLinePayableTransfer_LogsOneSummaryEntryForAllDetailRows() {
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
        verify(loggingService, times(1)).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("3 Row(s) Created on Line Payable Transfer Detail"));
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
                ArgumentMatchers.<String>isNull(), eq("1"), eq("TRANSACTION_POID"));

        // The snapshot must hold the pre-update value, otherwise the diff would always be empty
        assertEquals("IMPORT", oldCaptor.getValue().getBlType());
        assertEquals("EXPORT", testEntity.getBlType());
    }

    @Test
    void updateLinePayableTransfer_LogsOneSummaryEntryForAllDetailRows() {
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
        verify(loggingService, times(1)).createLogSummaryEntry(eq("100-432"), eq("1"),
                eq("2 Row(s) Created on Line Payable Transfer Detail"));
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
}
