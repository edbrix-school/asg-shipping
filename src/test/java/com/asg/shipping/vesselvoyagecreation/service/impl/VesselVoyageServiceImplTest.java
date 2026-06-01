package com.asg.shipping.vesselvoyagecreation.service.impl;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.exceptions.ResourceAlreadyExistsException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.vesselvoyagecreation.dto.*;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageHdrEntity;
import com.asg.shipping.vesselvoyagecreation.repository.*;
import com.asg.shipping.vesselvoyagecreation.util.FreightCargo;
import com.asg.shipping.vesselvoyagecreation.util.ImportExport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import net.sf.jasperreports.engine.JasperReport;
import com.asg.common.lib.dto.excel.ExcelFileData;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselVoyageServiceImplTest {

    @Mock
    private ShipVoyageHdrRepository voyageHdrRepository;

    @Mock
    private VoyageLineMasterRepository voyageLineMasterRepository;

    @Mock
    private ShipVoyageTranshipDtlRepository transhipDtlRepository;

    @Mock
    private VwShipEdiExceptionUploadRepository ediExceptionUploadRepository;

    @Mock
    private VwShipVoyageCurrencyRepository voyageCurrencyRepository;

    @Mock
    private StoredProcedureRepository storedProcedureRepository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private VoyageBillsRepository voyageBillsRepository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private PrintService printService;

    @Mock
    private ExcelExportService excelExportService;

    @Mock
    private DataSource dataSource;

    @InjectMocks
    private VesselVoyageServiceImpl service;

    @Test
    void listVoyages_throwsWhenDocIdEffectiveBlank() {
        Pageable pageable = PageRequest.of(0, 20);
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getDocumentId).thenReturn("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.listVoyages(filters, pageable, null, null, null));
            assertTrue(ex.getMessage().contains("X-Document-Id header"));
        }
    }

    @Test
    void listVoyages_fastPath_whenLinePoidPresentAndAllowedLinesFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

        List<Map<String, Object>> rows = List.of(
                new HashMap<>(Map.of("TRANSACTION_POID", 100L, "LINE_POID", 1L)),
                new HashMap<>(Map.of("TRANSACTION_POID", 101L, "LINE_POID", 2L))
        );

        RawSearchResult raw = new RawSearchResult(rows, Map.of(), 2L);

        when(documentSearchService.resolveOperator(filters)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentSearchService.resolveFilters(filters)).thenReturn(Collections.emptyList());
        when(documentSearchService.search(eq("DOC123"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("VOYAGE_NO"), eq("TRANSACTION_POID"))).thenReturn(raw);

        when(storedProcedureRepository.procGlobUserLineListing(999L)).thenReturn("(1)");

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getUserPoid).thenReturn(999L);

            Map<String, Object> response = service.listVoyages(filters, pageable, "DOC123", null, null);
            assertNotNull(response);
        }

        verify(voyageHdrRepository, never()).findAllById(any());
    }

    @Test
    void listVoyages_fallbackPath_whenLinePoidMissing_usesVoyageHdrLookup() {
        Pageable pageable = PageRequest.of(0, 20);
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

        List<Map<String, Object>> rows = List.of(
                new HashMap<>(Map.of("TRANSACTION_POID", 100L)),
                new HashMap<>(Map.of("TRANSACTION_POID", 101L))
        );

        RawSearchResult raw = new RawSearchResult(rows, Map.of(), 2L);

        when(documentSearchService.resolveOperator(filters)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentSearchService.resolveFilters(filters)).thenReturn(Collections.emptyList());
        when(documentSearchService.search(eq("DOC123"), anyList(), eq("OR"), eq(pageable), eq("N"),
                eq("VOYAGE_NO"), eq("TRANSACTION_POID"))).thenReturn(raw);

        when(storedProcedureRepository.procGlobUserLineListing(999L)).thenReturn("(1)");

        ShipVoyageHdrEntity voyage100 = ShipVoyageHdrEntity.builder().transactionPoid(100L).linePoid(1L).build();
        ShipVoyageHdrEntity voyage101 = ShipVoyageHdrEntity.builder().transactionPoid(101L).linePoid(2L).build();
        when(voyageHdrRepository.findAllById(any())).thenReturn(List.of(voyage100, voyage101));

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getUserPoid).thenReturn(999L);

            Map<String, Object> response = service.listVoyages(filters, pageable, "DOC123", null, null);
            assertNotNull(response);
        }

        verify(voyageHdrRepository).findAllById(any());
    }

    @Test
    void getVoyage_notFound_throws() {
        long voyagePoid = 10L;
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(1L);

            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(voyagePoid, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getVoyage(voyagePoid));
        }
    }

    @Test
    void getVoyage_success_returnsVoyageResponseAndLogs() {
        long voyagePoid = 10L;

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(1L);
            mocked.when(UserContext::getDocumentId).thenReturn("DOC123");

            ShipVoyageHdrEntity entity = ShipVoyageHdrEntity.builder()
                    .transactionPoid(voyagePoid)
                    .groupPoid(1L)
                    .companyPoid(2L)
                    .linePoid(7L)
                    .vesselPoid(8L)
                    .voyageNo("V001")
                    .docRef("DR001")
                    .jobNo("JOB001")
                    .deleted("N")
                    .build();

            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(voyagePoid, 1L))
                    .thenReturn(Optional.of(entity));
            when(voyageLineMasterRepository.findLineCodeByLinePoid(7L)).thenReturn(Optional.of("LINE_CODE"));

            VoyageResponse response = service.getVoyage(voyagePoid);
            assertNotNull(response);
            assertEquals(voyagePoid, response.getTransactionPoid());
            assertEquals("LINE_CODE", response.getLineCode());

            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC123", String.valueOf(voyagePoid));
        }
    }

    @Test
    void createVoyage_duplicate_throwsResourceAlreadyExists() {
        VoyageUpsertRequest req = VoyageUpsertRequest.builder()
                .voyageNo("V001")
                .linePoid(10L)
                .vesselPoid(20L)
                .expectedDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .arrivalDate(LocalDateTime.of(2026, 1, 2, 0, 0))
                .sailDate(LocalDateTime.of(2026, 1, 3, 0, 0))
                .customRegdate(LocalDateTime.of(2026, 1, 4, 0, 0))
                .preArrivalMsgVessel(LocalDateTime.of(2026, 1, 1, 1, 0))
                .preArrivalMsgPort(LocalDateTime.of(2026, 1, 1, 2, 0))
                .entryInGctos(LocalDateTime.of(2026, 1, 1, 3, 0))
                .entryInMarassi(LocalDateTime.of(2026, 1, 1, 4, 0))
                .operationStartDate(LocalDateTime.of(2026, 1, 5, 0, 0))
                .operationEndDate(LocalDateTime.of(2026, 1, 6, 0, 0))
                .build();

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(1L);
            mocked.when(UserContext::getCompanyPoid).thenReturn(2L);
            mocked.when(UserContext::getUserId).thenReturn("user1");

            when(voyageLineMasterRepository.findCompanyPoidByLinePoid(10L)).thenReturn(Optional.of(2L));
            when(voyageHdrRepository.existsByGroupPoidAndLinePoidAndVesselPoidAndVoyageNo(1L, 10L, 20L, "V001"))
                    .thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, () -> service.createVoyage(req));
        }
    }

    @Test
    void uploadAndProcessEdi_nullOrEmpty_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.uploadAndProcessEdi(10L, null));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.edi", "text/plain", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.uploadAndProcessEdi(10L, emptyFile));
    }

    @Test
    void uploadAndProcessEdi_success_storesAndReprocesses() throws Exception {
        Path tmp = Files.createTempDirectory("edi-test");
        ReflectionTestUtils.setField(service, "ediUploadDir", tmp.toString());

        MockMultipartFile file = new MockMultipartFile("file", "test.edi", "text/plain", "DATA".getBytes());

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getGroupPoid).thenReturn(1L);
            mocked.when(UserContext::getCompanyPoid).thenReturn(2L);
            mocked.when(UserContext::getDocumentId).thenReturn("DOC123");
            mocked.when(UserContext::getUserPoid).thenReturn(3L);

            when(voyageHdrRepository.existsById(123L)).thenReturn(true);
            when(storedProcedureRepository.procAttachmentsEdiProcNew(1L, 2L, "DOC123", 123L, 123L, 3L))
                    .thenReturn("REPROCESSED");

            String status = service.uploadAndProcessEdi(123L, file);
            assertEquals("REPROCESSED", status);
        }
    }

    @Test
    void downloadExcelExport_apmtDischarge_callsExcelServiceWithCorrectDocId() {
        byte[] expected = new byte[]{1, 2, 3};
        com.asg.common.lib.dto.excel.ExcelFileData fileData =
                com.asg.common.lib.dto.excel.ExcelFileData.builder()
                        .content(expected)
                        .fileName("Discharge_list.xlsx")
                        .build();

        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getCompanyPoid).thenReturn(2L);
            mocked.when(UserContext::getUserPoid).thenReturn(3L);

            when(excelExportService.generateExcel(
                    eq("100-291"), eq("418537"), anyMap(), eq("Discharge_list.xlsx")))
                    .thenReturn(fileData);

            byte[] result = service.downloadExcelExport(418537L, "apmt-discharge", "Discharge_list.xlsx");
            assertArrayEquals(expected, result);
        }
    }

    @Test
    void downloadExcelExport_unsupportedType_throwsIllegalArgument() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getCompanyPoid).thenReturn(2L);
            mocked.when(UserContext::getUserPoid).thenReturn(3L);

            assertThrows(IllegalArgumentException.class,
                    () -> service.downloadExcelExport(418537L, "unknown-type", "export.xlsx"));
        }
    }

    @Test
    void print_success_setsParamsAndCallsFillReport() throws Exception {
        when(printService.buildBaseParams(10L, "100-101")).thenReturn(new HashMap<>());

        JasperReport jr = mock(JasperReport.class);
        when(printService.load(anyString())).thenReturn(jr);

        byte[] pdf = new byte[] {1, 2, 3};
        when(printService.fillReportToPdf(any(JasperReport.class), anyMap(), eq(dataSource))).thenReturn(pdf);

        byte[] result = service.print(10L, FreightCargo.FALSE.name(), ImportExport.EXPORT.name());

        assertArrayEquals(pdf, result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(printService).fillReportToPdf(any(JasperReport.class), paramsCaptor.capture(), eq(dataSource));

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(FreightCargo.FALSE.name(), params.get("P_FREIGHTCARGO"));
        assertEquals(ImportExport.EXPORT.name(), params.get("P_IMPORT_EXPORT"));
    }
}

