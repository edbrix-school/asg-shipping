package com.asg.shipping.collectionhandover.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDetailCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtlId;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverDtlRepository;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverHdrRepository;
import com.asg.shipping.collectionhandover.util.CollectionHandoverMapper;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionHandoverServiceImplTest {

    @Mock
    private CollectionHandoverHdrRepository headerRepository;

    @Mock
    private CollectionHandoverDtlRepository detailRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private CollectionHandoverMapper mapper;

    @Mock
    private LoggingService loggingService;

    @Mock
    private PrintService printService;

    @Mock
    private DataSource dataSource;

    @InjectMocks
    private CollectionHandoverServiceImpl service;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void searchCollectionHandovers_success_withoutDates() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", Collections.emptyList());

        when(documentService.resolveOperator(request)).thenReturn("AND");
        when(documentService.resolveIsDeleted(request)).thenReturn("N");
        when(documentService.resolveFilters(request)).thenReturn(Collections.emptyList());

        RawSearchResult raw = new RawSearchResult(Collections.emptyList(), Map.of(), 0L);
        when(documentService.search(eq("300-106"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("DOC_REF"), eq("TRANSACTION_POID"))).thenReturn(raw);

        Map<String, Object> result = service.searchCollectionHandovers("300-106", request, pageable, null, null);
        assertNotNull(result);

        verify(documentService, never()).resolveDateFilters(any(), anyString(), any(), any());
    }

    @Test
    void searchCollectionHandovers_success_withDates_callsResolveDateFilters() {
        FilterRequestDto request = new FilterRequestDto("AND", "N", Collections.emptyList());

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(documentService.resolveOperator(request)).thenReturn("AND");
        when(documentService.resolveIsDeleted(request)).thenReturn("N");
        when(documentService.resolveFilters(request)).thenReturn(Collections.emptyList());
        when(documentService.resolveDateFilters(eq(request), eq("TRANSACTION_DATE"), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());

        RawSearchResult raw = new RawSearchResult(Collections.emptyList(), Map.of(), 0L);
        when(documentService.search(eq("300-106"), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("DOC_REF"), eq("TRANSACTION_POID"))).thenReturn(raw);

        Map<String, Object> result = service.searchCollectionHandovers("300-106", request, pageable, start, end);
        assertNotNull(result);

        verify(documentService).resolveDateFilters(eq(request), eq("TRANSACTION_DATE"), eq(start), eq(end));
    }

    @Test
    void getCollectionHandover_notFound_throws() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getCollectionHandover(1L));
        }
    }

    @Test
    void getCollectionHandover_success_mapsAndReturns() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                    .transactionPoid(1L)
                    .groupPoid(1L)
                    .companyPoid(1L)
                    .docRef("DOC1")
                    .deleted("N")
                    .build();

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());

            CollectionHandoverDto mapped = new CollectionHandoverDto();
            when(mapper.mapToDto(eq(hdr), anyList())).thenReturn(mapped);

            CollectionHandoverDto result = service.getCollectionHandover(1L);
            assertSame(mapped, result);

            verify(mapper).mapToDto(eq(hdr), anyList());
        }
    }

    @Test
    void createCollectionHandover_docRefExists_throwsValidation() {
        CollectionHandoverCreateDTO dto = new CollectionHandoverCreateDTO();
        dto.setDocRef("DOC_DUP");

        when(headerRepository.existsByDocRef("DOC_DUP")).thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.createCollectionHandover(dto, 1L, 2L));
    }

    @Test
    void createCollectionHandover_success_createsDetailsAndLogs() {
        CollectionHandoverCreateDTO dto = new CollectionHandoverCreateDTO();
        dto.setDocRef(null);
        dto.setTransactionDate(LocalDate.of(2026, 1, 1));
        dto.setCompanyPoid(100L);
        dto.setDetails(List.of(CollectionHandoverDetailCreateDTO.builder()
                .detRowId(1L)
                .currencyAmount(BigDecimal.TEN)
                .currencyType("USD")
                .noOfTran(1)
                .cashAmount(BigDecimal.ONE)
                .build()));

        ArShDayEndCloseHdr saved = new ArShDayEndCloseHdr();
        saved.setTransactionPoid(10L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(saved);
            when(detailRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            CollectionHandoverDto mapped = new CollectionHandoverDto();
            when(mapper.mapToDto(eq(saved), anyList())).thenReturn(mapped);
            doNothing().when(mapper).mapCreateDTOToEntity(eq(dto), any(ArShDayEndCloseHdr.class), eq(1L), eq(2L));
            doNothing().when(mapper).mapDetailCreateDTOToEntity(any(CollectionHandoverDetailCreateDTO.class), any(ArShDayEndCloseDtl.class));

            when(detailRepository.findByTransactionPoidOrderByDetRowId(10L)).thenReturn(Collections.emptyList());

            CollectionHandoverDto result = service.createCollectionHandover(dto, 1L, 2L);
            assertSame(mapped, result);

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC123", "10");
            verify(detailRepository).saveAll(anyList());
        }
    }

    @Test
    void updateCollectionHandover_notFound_throws() {
        CollectionHandoverUpdateDTO dto = new CollectionHandoverUpdateDTO();

        when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateCollectionHandover(1L, dto, 1L, 2L));
    }

    @Test
    void updateCollectionHandover_detailsNull_noDetailRepoOperationsButLogs() {
        CollectionHandoverUpdateDTO dto = new CollectionHandoverUpdateDTO();
        dto.setDetails(null); // covers updateDetailRecords early return branch

        ArShDayEndCloseHdr handover = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .companyPoid(1L)
                .deleted("N")
                .docRef(null)
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(handover));
            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(handover);

            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            CollectionHandoverDto mapped = new CollectionHandoverDto();
            when(mapper.mapToDto(any(ArShDayEndCloseHdr.class), anyList())).thenReturn(mapped);

            doNothing().when(mapper).mapUpdateDTOToEntity(eq(dto), any(ArShDayEndCloseHdr.class), eq(1L), eq(2L));

            CollectionHandoverDto result = service.updateCollectionHandover(1L, dto, 1L, 2L);
            assertSame(mapped, result);

            verify(loggingService).logChanges(any(), any(), eq(ArShDayEndCloseHdr.class),
                    eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
            // updateDetailRecords returns early because dto.getDetails() == null
            verify(detailRepository, never()).deleteById(any(ArShDayEndCloseDtlId.class));
        }
    }

    @Test
    void deleteCollectionHandover_alreadyDeleted_returnsWithoutSave() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .deleted("Y")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedHelper = mockStatic(ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("CURRENT_USER");

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));

            service.deleteCollectionHandover(1L);

            verify(headerRepository, never()).save(any());
        }
    }

    @Test
    void deleteCollectionHandover_notDeleted_softDeletesAndSaves() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .deleted("N")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedHelper = mockStatic(ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("CURRENT_USER");

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(hdr);

            service.deleteCollectionHandover(1L);

            ArgumentCaptor<ArShDayEndCloseHdr> captor = ArgumentCaptor.forClass(ArShDayEndCloseHdr.class);
            verify(headerRepository).save(captor.capture());

            assertEquals("Y", captor.getValue().getDeleted());
            assertEquals("CURRENT_USER", captor.getValue().getLastModifiedBy());
            assertNotNull(captor.getValue().getLastModifiedDate());
        }
    }

    @Test
    void toggleVerifyStatus_invalidVerifiedRcvd_throwsValidation() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .deleted("N")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedHelper = mockStatic(ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("CURRENT_USER");

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));

            assertThrows(ValidationException.class, () -> service.toggleVerifyStatus(1L, "A", null));
            verify(headerRepository, never()).save(any());
        }
    }

    @Test
    void toggleVerifyStatus_success_savesAndSetsRemarks() {
        ArShDayEndCloseHdr hdr = ArShDayEndCloseHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .deleted("N")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedHelper = mockStatic(ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("CURRENT_USER");

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(hdr);

            service.toggleVerifyStatus(1L, "Y", "remarks");

            ArgumentCaptor<ArShDayEndCloseHdr> captor = ArgumentCaptor.forClass(ArShDayEndCloseHdr.class);
            verify(headerRepository).save(captor.capture());

            assertEquals("Y", captor.getValue().getVerifiedRcvd());
            assertEquals("remarks", captor.getValue().getMainOfcRemarks());
            assertEquals("CURRENT_USER", captor.getValue().getLastModifiedBy());
            assertNotNull(captor.getValue().getLastModifiedDate());
        }
    }

    @Test
    void print_success_buildsParamsAndFillsReport() throws Exception {
        when(printService.buildBaseParams(10L, "300-114")).thenReturn(new java.util.HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(JasperReport.class), anyMap(), eq(dataSource)))
                .thenReturn(new byte[] {1, 2});

        byte[] result = service.print(10L);
        assertArrayEquals(new byte[] {1, 2}, result);

        verify(printService).fillReportToPdf(any(JasperReport.class), anyMap(), eq(dataSource));
    }
}

