package com.asg.shipping.dayCloseShiping.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.dayCloseShiping.dto.*;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseHdr;
import com.asg.shipping.dayCloseShiping.repository.*;
import com.asg.shipping.dayCloseShiping.util.DayCloseMapper;

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
    @Mock private DayCloseMapper mapper;

    // 🔑 REQUIRED NEW DEPENDENCIES
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;

    /* ---------------- GET DAY CLOSE ---------------- */

    @Test
    void getDayClose_success() {
        ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
        hdr.setTransactionPoid(1L);
        hdr.setDeleted("N");

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of(new ArShDayEndCloseDtl()));
        when(mapper.mapToDto(hdr)).thenReturn(new DayCloseDto());
        when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

        DayCloseDto result = service.getDayClose(1L, 1L, 1L);

        assertNotNull(result);
    }

    @Test
    void getDayClose_notFound() {
        when(hdrRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getDayClose(1L, 1L, 1L));
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
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("ID", 1L)),
                Map.of("ID", "ID"),
                1L
        );

        when(documentService.resolveOperator(any())).thenReturn("OR");
        when(documentService.resolveIsDeleted(any())).thenReturn("false");
        when(documentService.resolveFilters(any())).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(), any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(raw);

        Map<String, Object> result =
                service.searchDayClose("DOC1", filters, pageable, null, null);

        assertNotNull(result);
    }

    /* ---------------- CREATE DAY CLOSE ---------------- */

    @Test
    void createDayClose_success() {
        try (
            MockedStatic<UserContext> ctx = mockStatic(UserContext.class);
            MockedConstruction<SimpleJdbcCall> jdbc =
                    Mockito.mockConstruction(SimpleJdbcCall.class,
                            (mock, context) -> {
                                when(mock.withProcedureName(any())).thenReturn(mock);
                                when(mock.execute(any(Map.class)))
                                        .thenReturn(Map.of("P_STATUS", "SUCCESS"));
                            })
        ) {
            ctx.when(UserContext::getDocumentId).thenReturn("DOC123");

            DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                    .transactionDate(LocalDate.now())
                    .cashAmount(BigDecimal.TEN)
                    .chequeAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.TEN)
                    .build();

            DayCloseDto dto = new DayCloseDto();
            dto.setHeader(hdrDto);

            ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
            hdr.setTransactionPoid(1L);
            hdr.setTransactionDate(LocalDate.now());
            hdr.setDocRef("DOC_REF");

            when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any()))
                    .thenReturn(0L);
            when(hdrRepo.save(any())).thenReturn(hdr);
            when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
            when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
            when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
            when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

            DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

            assertNotNull(result);
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
    void updateDayClose_withDenominations_success() {
        DayCloseDenominationDto denom = new DayCloseDenominationDto();
        denom.setDenomination(new BigDecimal("100"));
        denom.setNoOfTran(2L);
        denom.setAction("INSERT");

        DayCloseHdrDto hdrDto = DayCloseHdrDto.builder()
                .cashAmount(new BigDecimal("200"))
                .chequeAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("200"))
                .build();

        DayCloseDto dto = new DayCloseDto();
        dto.setHeader(hdrDto);
        dto.setDenominations(List.of(denom));

        ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
        hdr.setTransactionPoid(1L);

        when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
        when(hdrRepo.save(any())).thenReturn(hdr);
        when(dtlRepo.getMaxDetRowId(1L)).thenReturn(0L);
        when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
        when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
        when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

        DayCloseDto result = service.updateDayClose(dto, 1L, 1L, 1L, 1L);

        assertNotNull(result);
        verify(hdrRepo).save(any());
    }
}
