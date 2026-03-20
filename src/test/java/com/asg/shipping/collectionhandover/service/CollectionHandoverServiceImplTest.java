package com.asg.shipping.collectionhandover.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.collectionhandover.dto.*;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtlId;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverDtlRepository;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverHdrRepository;
import com.asg.shipping.collectionhandover.service.CollectionHandoverServiceImpl;
import com.asg.shipping.collectionhandover.util.CollectionHandoverMapper;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    private ArShDayEndCloseHdr hdr;
    private ArShDayEndCloseDtl dtl;
    private CollectionHandoverDto dto;

    @BeforeEach
    void init() {
        hdr = new ArShDayEndCloseHdr();
        hdr.setTransactionPoid(1L);
        hdr.setGroupPoid(1L);
        hdr.setCompanyPoid(100L);
        hdr.setDocRef("DOC1");
        hdr.setDeleted("N");

        dtl = new ArShDayEndCloseDtl();
        dtl.setTransactionPoid(1L);
        dtl.setDetRowId(1L);

        dto = CollectionHandoverDto.builder()
                .transactionPoid(1L)
                .companyPoid(100L)
                .docRef("DOC1")
                .details(Collections.emptyList())
                .build();
    }

    @Test
    void searchCollectionHandovers_Success() {
        try (MockedStatic<PaginationUtil> mockedPagination = mockStatic(PaginationUtil.class)) {
            FilterRequestDto request = new FilterRequestDto("AND", "N", new ArrayList<>());
            Pageable pageable = PageRequest.of(0, 20);

            when(documentService.resolveOperator(any())).thenReturn("AND");
            when(documentService.resolveIsDeleted(any())).thenReturn("N");
            when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
            when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            mockedPagination.when(() -> PaginationUtil.wrapPage(any(Page.class), any()))
                    .thenReturn(Map.of("content", Collections.emptyList()));

            Map<String, Object> result = service.searchCollectionHandovers("300-106", request, pageable, null, null);

            assertNotNull(result);
            verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void getCollectionHandover_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of(dtl));
            when(mapper.mapToDto(hdr, List.of(dtl))).thenReturn(dto);

            CollectionHandoverDto result = service.getCollectionHandover(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            verify(headerRepository).findByTransactionPoidAndGroupPoid(1L, 1L);
        }
    }

    @Test
    void getCollectionHandover_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getCollectionHandover(1L));
        }
    }

    @Test
    void createCollectionHandover_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("300-106");

            CollectionHandoverCreateDTO createDTO = CollectionHandoverCreateDTO.builder()
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .companyPoid(100L)
                    .docRef("DOC1")
                    .details(Collections.emptyList())
                    .build();

            when(headerRepository.existsByDocRef("DOC1")).thenReturn(false);
            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(hdr);
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());
            when(mapper.mapToDto(hdr, Collections.emptyList())).thenReturn(dto);

            CollectionHandoverDto result = service.createCollectionHandover(createDTO, 1L, 2L);

            assertNotNull(result);
            verify(headerRepository).save(any(ArShDayEndCloseHdr.class));
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("300-106"), eq("1"));
        }
    }

    @Test
    void createCollectionHandover_DuplicateDocRef_Throws() {
        CollectionHandoverCreateDTO createDTO = CollectionHandoverCreateDTO.builder()
                .transactionDate(LocalDate.of(2026, 1, 1))
                .companyPoid(100L)
                .docRef("DOC1")
                .build();

        when(headerRepository.existsByDocRef("DOC1")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.createCollectionHandover(createDTO, 1L, 2L));
    }

    @Test
    void updateCollectionHandover_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("300-106");

            CollectionHandoverUpdateDTO updateDTO = CollectionHandoverUpdateDTO.builder()
                    .docRef("DOC2")
                    .details(Collections.emptyList())
                    .build();

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));
            when(headerRepository.existsByDocRefExcludingPoid("DOC2", 1L)).thenReturn(false);
            when(headerRepository.save(any(ArShDayEndCloseHdr.class))).thenReturn(hdr);
            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());
            when(mapper.mapToDto(hdr, Collections.emptyList())).thenReturn(dto);

            CollectionHandoverDto result = service.updateCollectionHandover(1L, updateDTO, 1L, 2L);

            assertNotNull(result);
            verify(loggingService).logChanges(any(), any(), eq(ArShDayEndCloseHdr.class),
                    eq("300-106"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void updateCollectionHandover_DuplicateDocRef_Throws() {
        CollectionHandoverUpdateDTO updateDTO = CollectionHandoverUpdateDTO.builder()
                .docRef("DOC2")
                .build();

        when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                .thenReturn(Optional.of(hdr));
        when(headerRepository.existsByDocRefExcludingPoid("DOC2", 1L)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.updateCollectionHandover(1L, updateDTO, 1L, 2L));
    }

    @Test
    void deleteCollectionHandover_SoftDelete() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));

            service.deleteCollectionHandover(1L);

            assertEquals("Y", hdr.getDeleted());
            verify(headerRepository).save(hdr);
        }
    }

    @Test
    void deleteCollectionHandover_AlreadyDeleted_NoOp() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            hdr.setDeleted("Y");
            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));

            service.deleteCollectionHandover(1L);

            verify(headerRepository, never()).save(any());
        }
    }

    @Test
    void deleteCollectionHandover_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteCollectionHandover(1L));
        }
    }

    @Test
    void toggleVerifyStatus_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));

            service.toggleVerifyStatus(1L, "Y", "OK");

            assertEquals("Y", hdr.getVerifiedRcvd());
            assertEquals("OK", hdr.getMainOfcRemarks());
            verify(headerRepository).save(hdr);
        }
    }

    @Test
    void toggleVerifyStatus_InvalidFlag_Throws() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(hdr));

            assertThrows(ValidationException.class, () -> service.toggleVerifyStatus(1L, "X", null));
        }
    }

    @Test
    void print_Success() throws Exception {
        JasperReport report = mock(JasperReport.class);
        when(printService.buildBaseParams(1L, "300-114"))
                .thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(report);
        when(printService.fillReportToPdf(any(), anyMap(), eq(dataSource)))
                .thenReturn("PDF".getBytes());

        byte[] result = service.print(1L);

        assertNotNull(result);
        verify(printService).fillReportToPdf(any(), anyMap(), eq(dataSource));
    }
}

