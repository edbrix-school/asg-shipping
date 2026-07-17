package com.asg.shipping.daycloseshiping.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.daycloseshiping.dto.*;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseHdr;
import com.asg.shipping.daycloseshiping.repository.*;
import com.asg.shipping.daycloseshiping.util.DayCloseMapper;

@ExtendWith(MockitoExtension.class)
class DayCloseServiceImplTest {

    @Spy
    @InjectMocks
    private DayCloseServiceImpl service;

    @Mock private ArShDayEndCloseHdrRepository hdrRepo;
    @Mock private ArShDayEndCloseDtlRepository dtlRepo;
    @Mock private GlobalCurrencyDenominationRepository denomRepo;
    @Mock private ArShReceiptHdrRepository receiptHdrRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private DocumentSearchService documentService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private DayCloseMapper mapper;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;

    @Mock private EntityManager entityManager;

    /* ---------------- GET DAY CLOSE ---------------- */
    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field field = DayCloseServiceImpl.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(service, entityManager);
    }
    @Test
    void getDayClose_success() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .companyPoid(1L)
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .deleted("N")
                .build();

        ArShDayEndCloseDtl dtl = ArShDayEndCloseDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .currencyAmount(BigDecimal.TEN)
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of(dtl));

        DayCloseDto result = service.getDayClose(1L, 1L, 1L);

        assertNotNull(result);
        verify(hdrRepo).findById(1L);
        verify(dtlRepo).findByTransactionPoid(1L);
    }

    @Test
    void getDayClose_notFound() {
        when(hdrRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getDayClose(1L, 1L, 1L));
    }

    @Test
    void getDayClose_deleted() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .deleted("Y")
                .build();
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());

        DayCloseDto result = service.getDayClose(1L, 1L, 1L);

        assertNotNull(result);
        verify(hdrRepo).findById(1L);
    }

    /* ---------------- NEW DAY CLOSE ---------------- */

    @Test
    void getNewDayCloseData_success() {
        DayCloseSummaryProjection summary =
                DayCloseSummaryProjectionImpl.builder()
                        .transactionDate(LocalDate.now())
                        .cashAmount(BigDecimal.TEN)
                        .chequeAmount(BigDecimal.ONE)
                        .totalAmount(BigDecimal.valueOf(11))
                        .chequeCount(1L)
                        .build();

        when(receiptHdrRepository.fetchNewDayCloseSummary(1L, 1L, "2024-01-01"))
                .thenReturn(Optional.of(summary));

        DayCloseSummaryProjection result =
                service.getNewDayCloseData(1L, 1L, "2024-01-01");

        assertEquals(BigDecimal.TEN, result.getCashAmount());
    }

    @Test
    void getNewDayCloseData_notFound() {
        when(receiptHdrRepository.fetchNewDayCloseSummary(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getNewDayCloseData(1L, 1L, "2024-01-01"));
    }

    /* ---------------- DENOMINATIONS ---------------- */

    @Test
    void getDenominations_success() {
        var denom = mock(com.asg.shipping.common.entity.GlobalCurrencyDenomination.class);
        when(denom.getCurrencyAmount()).thenReturn("100");
        when(denom.getCurrencyType()).thenReturn("NOTE");

        when(denomRepo.findByCurrencyCodeOrderBySeqNo("INR"))
                .thenReturn(List.of(denom));

        List<Map<String, Object>> result = service.getDenominations("INR");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100"), result.get(0).get("denomination"));
    }

    /* ---------------- SEARCH ---------------- */

    @Test
    void searchDayClose_success() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveFilters(any())).thenReturn(List.of());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.searchDayClose("DOC123", request, pageable, null, null);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchDayClose_withDateRange() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveDateFilters(any(), any(), any(), any())).thenReturn(List.of());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.searchDayClose("DOC123", request, pageable, startDate, endDate);

        assertNotNull(result);
        verify(documentService).resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
    }

    /* ---------------- CREATE DAY CLOSE ---------------- */

    @Test
    void createDayClose_success() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of());

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("SUCCESS");

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(hdrRepo).saveAndFlush(any());
            verify(loggingService, atLeastOnce()).createLogSummaryEntry(anyString(), anyString(), anyString());
        }
    }

    @Test
    void createDayClose_withDenominations() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(1L)
                .denomination(BigDecimal.TEN)
                .noOfTran(1L)
                .cashAmount(BigDecimal.TEN)
                .action("ISCREATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(dtlRepo.getMaxDetRowId(any())).thenReturn(0L);
        when(dtlRepo.saveAll(anyList())).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("SUCCESS");

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).saveAll(anyList());
        }
    }

    @Test
    void createDayClose_procedureFailure() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of());

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("ERROR: Failed");

            assertThrows(RuntimeException.class, () -> service.createDayClose(dto, 1L, 1L, 1L));
        }
    }


    /* ---------------- VALIDATION ---------------- */

    @Test
    void createDayClose_amountMismatch_throwsException() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.TEN)
                .totalAmount(BigDecimal.ONE)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);

        assertThrows(ValidationException.class,
                () -> service.createDayClose(dto, 1L, 1L, 1L));
    }

    @Test
    void createDayClose_duplicateTransactionDate_throwsException() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any()))
                .thenReturn(1L);

        assertThrows(ValidationException.class,
                () -> service.createDayClose(dto, 1L, 1L, 1L));
    }

    /* ---------------- UPDATE ---------------- */

    @Test
    void updateDayClose_success() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of());

        ArShDayEndCloseHdr existingHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .cashAmount(BigDecimal.ONE)
                .deleted("N")
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(existingHdr));
        when(hdrRepo.save(any())).thenReturn(existingHdr);
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            DayCloseDto result = service.updateDayClose(dto, 1L, 1L, 1L, 1L);

            assertNotNull(result);
            verify(hdrRepo).save(any());
            verify(loggingService).logChanges(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void updateDayClose_notFound() {
        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(DayCloseHdrDto.builder().build());

        when(hdrRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateDayClose(dto, 1L, 1L, 1L, 1L));
    }

    @Test
    void updateDayClose_withDenominationUpdate() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(1L)
                .denomination(BigDecimal.TEN)
                .noOfTran(2L)
                .cashAmount(BigDecimal.valueOf(20))
                .action("ISUPDATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.valueOf(20))
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(20))
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr existingHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .deleted("N")
                .build();

        ArShDayEndCloseDtl existingDtl = ArShDayEndCloseDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .currencyAmount(BigDecimal.TEN)
                .noOfTran(1L)
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(existingHdr));
        when(hdrRepo.save(any())).thenReturn(existingHdr);
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(dtlRepo.findByTransactionPoidAndDetRowId(1L, 1L)).thenReturn(Optional.of(existingDtl));
        when(dtlRepo.saveAll(anyList())).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            DayCloseDto result = service.updateDayClose(dto, 1L, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).saveAll(anyList());
        }
    }

    @Test
    void updateDayClose_withDenominationDelete() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(1L)
                .action("ISDELETED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.ZERO)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr existingHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .deleted("N")
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(existingHdr));
        when(hdrRepo.save(any())).thenReturn(existingHdr);
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            DayCloseDto result = service.updateDayClose(dto, 1L, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).deleteByTransactionPoidAndDetRowIdIn(eq(1L), anyList());
        }
    }

    /* ---------------- DELETE ---------------- */

    @Test
    void deleteDayClose_success() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .deleted("N")
                .build();

        when(hdrRepo.findByTransactionPoidDeleted(1L)).thenReturn(Optional.of(hdr));

        service.deleteDayClose(1L, new DeleteReasonDto());

        verify(hdrRepo).findByTransactionPoidDeleted(1L);
    }

    @Test
    void deleteDayClose_notFound() {
        when(hdrRepo.findByTransactionPoidDeleted(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteDayClose(1L, new DeleteReasonDto()));
    }

    /* ---------------- PRINT ---------------- */

    @Test
    void print_success() throws Exception {
        when(printService.buildBaseParams(1L, "300-106")).thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.print(1L);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), any(), any());
    }

    @Test
    void printDetails_success() throws Exception {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.of(2026, 7, 17))
                .companyPoid(9L)
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(printService.buildBaseParams(1L, "300-106")).thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.printDetails(1L);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), any(), any());
        verify(hdrRepo).findById(1L);
    }

    @Test
    void printSplitReceipt_success() throws Exception {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.of(2026, 7, 17))
                .companyPoid(9L)
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(printService.buildBaseParams(1L, "300-106")).thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.printSplitReceipt(1L);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), any(), any());
        verify(hdrRepo).findById(1L);
    }

    @Test
    void printSummary_success() throws Exception {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.of(2026, 7, 17))
                .companyPoid(9L)
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(printService.buildBaseParams(1L, "300-106")).thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.printSummary(1L);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), any(), any());
        verify(hdrRepo).findById(1L);
    }

    /* ---------------- VALIDATION HELPERS ---------------- */

    @Test
    void validateAmounts_denominationMismatch() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .denomination(BigDecimal.TEN)
                .noOfTran(1L)
                .cashAmount(BigDecimal.TEN)
                .action("ISCREATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.valueOf(20))
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(20))
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        assertThrows(ValidationException.class,
                () -> service.createDayClose(dto, 1L, 1L, 1L));
    }

    @Test
    void createDayClose_withDenominationWithDetRowId() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(5L)
                .denomination(BigDecimal.TEN)
                .noOfTran(1L)
                .cashAmount(BigDecimal.TEN)
                .action("ISCREATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(dtlRepo.getMaxDetRowId(any())).thenReturn(0L);
        when(dtlRepo.saveAll(anyList())).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("SUCCESS");

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).saveAll(anyList());
        }
    }

    @Test
    void updateDayClose_denominationNotFound() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(999L)
                .denomination(BigDecimal.TEN)
                .noOfTran(2L)
                .cashAmount(BigDecimal.valueOf(20))
                .action("ISUPDATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(BigDecimal.valueOf(20))
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(20))
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr existingHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .deleted("N")
                .build();

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(existingHdr));
        when(hdrRepo.save(any())).thenReturn(existingHdr);
        when(dtlRepo.findByTransactionPoidAndDetRowId(1L, 999L)).thenReturn(Optional.empty());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            assertThrows(com.asg.shipping.exceptions.ValidationException.class,
                    () -> service.updateDayClose(dto, 1L, 1L, 1L, 1L));
        }
    }

    @Test
    void createDayClose_withDenominationCalculatedAmount() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(1L)
                .denomination(BigDecimal.valueOf(5))
                .noOfTran(2L)
                .action("ISCREATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(dtlRepo.getMaxDetRowId(any())).thenReturn(0L);
        when(dtlRepo.saveAll(anyList())).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("SUCCESS");

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).saveAll(anyList());
        }
    }

    @Test
    void createDayClose_procedureReturnsNull() {
        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.TEN)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of());

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn(null);

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(hdrRepo).saveAndFlush(any());
        }
    }

    @Test
    void createDayClose_withDenominationOnlyDenomination() {
        DayCloseDenominationDto denomDto = DayCloseDenominationDto.builder()
                .detRowId(1L)
                .denomination(BigDecimal.TEN)
                .noOfTran(null)
                .cashAmount(null)
                .action("ISCREATED")
                .build();

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.now())
                .cashAmount(BigDecimal.ZERO)
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denomDto));

        ArShDayEndCloseHdr savedHdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
        when(hdrRepo.saveAndFlush(any())).thenReturn(savedHdr);
        when(hdrRepo.findById(1L)).thenReturn(Optional.of(savedHdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(dtlRepo.getMaxDetRowId(any())).thenReturn(0L);
        when(dtlRepo.saveAll(anyList())).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedContext = Mockito.mockStatic(UserContext.class)) {
            mockedContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            mockedContext.when(UserContext::getTimeZoneCode).thenReturn("UTC");

            when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn("SUCCESS");

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(dtlRepo).saveAll(anyList());
        }
    }
}
