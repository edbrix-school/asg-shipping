package com.asg.shipping.vesselvoyagecreation.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.vesselvoyagecreation.dto.*;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipEdiExceptionUploadEntity;
import com.asg.shipping.vesselvoyagecreation.service.VesselVoyageService;
import com.asg.shipping.vesselvoyagecreation.util.FreightCargo;
import com.asg.shipping.vesselvoyagecreation.util.ImportExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselVoyageControllerTest {

    @Mock
    private VesselVoyageService vesselVoyageService;

    @InjectMocks
    private VesselVoyageController controller;

    private MockedStatic<UserContext> mockedUserContext;

    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        mockedUserContext = Mockito.mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getActionRequested).thenReturn("TEST_ACTION");
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2L);
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(3L);
        mockedUserContext.when(UserContext::getUserId).thenReturn("user1");
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void list_voyages_badRequest_whenOnlyStartDateProvided() {
        ResponseEntity<?> response = controller.list(
                pageable,
                new FilterRequestDto("OR", "false", List.of()),
                "DOC123",
                LocalDate.of(2026, 1, 1),
                null);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(vesselVoyageService);
    }

    @Test
    void list_voyages_badRequest_whenOnlyEndDateProvided() {
        ResponseEntity<?> response = controller.list(
                pageable,
                new FilterRequestDto("OR", "false", List.of()),
                "DOC123",
                null,
                LocalDate.of(2026, 12, 31));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(vesselVoyageService);
    }

    @Test
    void list_voyages_success_whenDatesProvided() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        Map<String, Object> serviceResult = Map.of("content", List.of(), "totalElements", 0);
        when(vesselVoyageService.listVoyages(eq(filters), eq(pageable), eq("DOC123"), eq(startDate), eq(endDate)))
                .thenReturn(serviceResult);

        ResponseEntity<?> response = controller.list(pageable, filters, "DOC123", startDate, endDate);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Vessel voyages fetched successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void list_voyages_internalServerError_whenServiceThrows() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

        when(vesselVoyageService.listVoyages(eq(filters), eq(pageable), eq("DOC123"), nullable(LocalDate.class), nullable(LocalDate.class)))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.list(pageable, filters, "DOC123", null, null);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(((Map<?, ?>) response.getBody()).get("message").toString().contains("boom"));
    }

    @Test
    void list_voyages_success_whenDatesEmptyAndDocIdNull() {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

        Map<String, Object> serviceResult = Map.of("content", List.of(), "totalElements", 0);
        when(vesselVoyageService.listVoyages(eq(filters), eq(pageable), isNull(), isNull(), isNull()))
                .thenReturn(serviceResult);

        ResponseEntity<?> response = controller.list(pageable, filters, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Vessel voyages fetched successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void otherEndpoints_success_smoke() throws Exception {
        // GET voyage
        VoyageResponse voyageResponse = new VoyageResponse();
        when(vesselVoyageService.getVoyage(10L)).thenReturn(voyageResponse);

        ResponseEntity<?> getResponse = controller.get(10L);
        assertEquals(200, getResponse.getStatusCode().value());

        // CREATE voyage
        VoyageUpsertRequest createRequest = new VoyageUpsertRequest();
        when(vesselVoyageService.createVoyage(any(VoyageUpsertRequest.class))).thenReturn(voyageResponse);
        ResponseEntity<?> createResponse = controller.create(createRequest);
        assertEquals(200, createResponse.getStatusCode().value());

        // UPDATE voyage
        when(vesselVoyageService.updateVoyage(eq(10L), any(VoyageUpsertRequest.class))).thenReturn(voyageResponse);
        ResponseEntity<?> updateResponse = controller.update(10L, createRequest);
        assertEquals(200, updateResponse.getStatusCode().value());

        // LIST BLs
        VoyageBlTab tab = VoyageBlTab.HOLD;
        VoyageBlFilter filter = VoyageBlFilter.ALL;
        Page<VoyageBlRow> blPage = new PageImpl<>(Collections.emptyList());
        when(vesselVoyageService.listBls(eq(10L), eq(tab), eq(filter), eq(pageable))).thenReturn(blPage);
        ResponseEntity<?> listBlsResponse = controller.listBls(10L, tab, filter, pageable);
        assertEquals(200, listBlsResponse.getStatusCode().value());

        // EDI errors/reprocess/upload
        when(vesselVoyageService.getEdiErrors(10L)).thenReturn(Collections.<VwShipEdiExceptionUploadEntity>emptyList());
        assertEquals(200, controller.ediErrors(10L).getStatusCode().value());

        when(vesselVoyageService.reprocessEdi(10L)).thenReturn("reprocessed");
        assertEquals(200, controller.ediReprocess(10L).getStatusCode().value());

        MultipartFile multipartFile = new MockMultipartFile("file", "test.edi", "text/plain", "data".getBytes());
        when(vesselVoyageService.uploadAndProcessEdi(eq(10L), any(MultipartFile.class))).thenReturn("uploaded");
        assertEquals(200, controller.ediUpload(10L, multipartFile).getStatusCode().value());

        // Transhipments
        when(vesselVoyageService.listTranshipments(10L))
                .thenReturn(Collections.<ShipVoyageTranshipDtlEntity>emptyList());
        assertEquals(200, controller.listTranshipments(10L).getStatusCode().value());

        TranshipmentUpdateRequest transUpdateRequest = TranshipmentUpdateRequest.builder()
                .items(List.of(TranshipmentUpdateItem.builder().detRowId(1L).build()))
                .build();
        when(vesselVoyageService.updateTranshipments(eq(10L), any(TranshipmentUpdateRequest.class)))
                .thenReturn(Collections.<ShipVoyageTranshipDtlEntity>emptyList());
        assertEquals(200, controller.updateTranshipments(10L, transUpdateRequest).getStatusCode().value());

        TranshipmentTransferRequest transferRequest = TranshipmentTransferRequest.builder()
                .targetVoyagePoid(20L)
                .detRowIds(List.of(1L, 2L))
                .build();
        when(vesselVoyageService.transferTranshipments(eq(10L), any(TranshipmentTransferRequest.class)))
                .thenReturn(Map.of("message", "Transfer Assignment Completed (2 rows). Press Save/Refresh.", "loadTransactionPoid", 20L));
        assertEquals(200, controller.transferTranshipments(10L, transferRequest).getStatusCode().value());

        when(vesselVoyageService.importHnjnTranshipments(10L)).thenReturn("imported");
        assertEquals(200, controller.importHnjn(10L).getStatusCode().value());

        // Currencies
        when(vesselVoyageService.listVoyageCurrencies(10L))
                .thenReturn(Collections.emptyList());
        assertEquals(200, controller.listCurrency(10L).getStatusCode().value());

        CurrencyUpdateRequest currencyUpdateRequest = CurrencyUpdateRequest.builder()
                .items(List.of(CurrencyUpdateItem.builder().currencyCode("USD").newCurrencyExchange(1.0).build()))
                .build();
        when(vesselVoyageService.updateCurrencyRates(eq(10L), any(CurrencyUpdateRequest.class))).thenReturn("updated");
        assertEquals(200, controller.updateCurrency(10L, currencyUpdateRequest).getStatusCode().value());

        // CAN resend + manifest/TDR + import/export
        when(vesselVoyageService.resendCan(eq(10L), nullable(Long.class))).thenReturn("can-resend");
        assertEquals(200, controller.resendCan(10L, null).getStatusCode().value());

        when(vesselVoyageService.createEmptyManifest(10L)).thenReturn("created-empty-manifest");
        assertEquals(200, controller.emptyManifest(10L).getStatusCode().value());

        when(vesselVoyageService.createTdr(eq(10L), eq(false))).thenReturn("tdr-created");
        assertEquals(200, controller.createTdr(10L, false).getStatusCode().value());

        when(vesselVoyageService.exportEdiCosco(eq(10L), nullable(Long.class))).thenReturn("cosco-export");
        assertEquals(200, controller.exportCosco(10L, null).getStatusCode().value());

        when(vesselVoyageService.importGeneralCargo(10L)).thenReturn("imported-general-cargo");
        assertEquals(200, controller.importGeneralCargo(10L).getStatusCode().value());

        when(vesselVoyageService.importSelectedXl(10L)).thenReturn("imported-selected-xl");
        assertEquals(200, controller.importSelectedXl(10L).getStatusCode().value());

        doNothing().when(vesselVoyageService).ediMovesLoadDischarge(eq(10L), eq("2026-04-15"));
        assertEquals(200, controller.ediMovesLoadDischarge(10L, "2026-04-15").getStatusCode().value());

        doNothing().when(vesselVoyageService).deleteVoyage(eq(10L), nullable(DeleteReasonDto.class));
        assertEquals(200, controller.deleteVoyage(10L, null).getStatusCode().value());
    }

    @Test
    void ediUpload_success_whenFileIsNull() {
        when(vesselVoyageService.uploadAndProcessEdi(eq(10L), isNull())).thenReturn("uploaded");

        ResponseEntity<?> response = controller.ediUpload(10L, null);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void downloadExcel_apmtDischarge_success() {
        byte[] content = new byte[]{1, 2, 3};
        when(vesselVoyageService.downloadExcelExport(10L, "apmt-discharge", "Discharge_list.xlsx"))
                .thenReturn(content);

        ResponseEntity<?> response = controller.downloadExcel(10L, "apmt-discharge");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"Discharge_list.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertArrayEquals(content, (byte[]) response.getBody());
    }

    @Test
    void downloadExcel_transhipmentDischarge_success() {
        byte[] content = new byte[]{4, 5, 6};
        when(vesselVoyageService.downloadExcelExport(10L, "transhipment-discharge", "Transhipment_Discharge_list.xlsx"))
                .thenReturn(content);

        ResponseEntity<?> response = controller.downloadExcel(10L, "transhipment-discharge");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"Transhipment_Discharge_list.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(content, (byte[]) response.getBody());
    }

    @Test
    void downloadExcel_apmtGeneralVesselDischarge_success() {
        byte[] content = new byte[]{7, 8, 9};
        when(vesselVoyageService.downloadExcelExport(10L, "apmt-general-vessel-discharge", "APMTLISTGERN.xlsx"))
                .thenReturn(content);

        ResponseEntity<?> response = controller.downloadExcel(10L, "apmt-general-vessel-discharge");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"APMTLISTGERN.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(content, (byte[]) response.getBody());
    }

    @Test
    void downloadExcel_ymlExportCsv_success() {
        byte[] content = new byte[]{10, 11, 12};
        when(vesselVoyageService.downloadExcelExport(10L, "yml-export-csv", "OA_Booking_csv_format.csv"))
                .thenReturn(content);

        ResponseEntity<?> response = controller.downloadExcel(10L, "yml-export-csv");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"OA_Booking_csv_format.csv\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(content, (byte[]) response.getBody());
    }

    @Test
    void downloadExcel_tbl_success() {
        byte[] content = new byte[]{13, 14, 15};
        when(vesselVoyageService.downloadExcelExport(10L, "tbl", "TBLManifestTemplate.xlsx"))
                .thenReturn(content);

        ResponseEntity<?> response = controller.downloadExcel(10L, "tbl");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"TBLManifestTemplate.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(content, (byte[]) response.getBody());
    }

    @Test
    void downloadManifest_success_filenameNonNull() {
        Resource resource = mock(Resource.class);
        when(resource.getFilename()).thenReturn("manifest.pdf");
        when(vesselVoyageService.downloadManifestReport(10L, "FALSE", "EXPORT")).thenReturn(resource);

        ResponseEntity<?> response = controller.downloadManifest(10L, "FALSE", "EXPORT");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"manifest.pdf\"", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertSame(resource, response.getBody());
    }

    @Test
    void downloadManifest_success_filenameNull_usesDefault() {
        Resource resource = mock(Resource.class);
        when(resource.getFilename()).thenReturn(null);
        when(vesselVoyageService.downloadManifestReport(10L, "FALSE", "EXPORT")).thenReturn(resource);

        ResponseEntity<?> response = controller.downloadManifest(10L, "FALSE", "EXPORT");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"manifest.pdf\"", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertSame(resource, response.getBody());
    }

    @Test
    void print_success_freightCargoFalse_importExportNull() throws Exception {
        byte[] pdfBytes = new byte[] {9, 9, 9};

        when(vesselVoyageService.print(123L, "FALSE", null)).thenReturn(pdfBytes);

        ResponseEntity<?> response = controller.print(123L, FreightCargo.FALSE, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=cargo-manifest-123.pdf", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertArrayEquals(pdfBytes, (byte[]) response.getBody());
    }

    @Test
    void print_success_freightCargoTrue_importExportExport() throws Exception {
        byte[] pdfBytes = new byte[] {7, 7, 7};

        when(vesselVoyageService.print(123L, "TRUE", "EXPORT")).thenReturn(pdfBytes);

        ResponseEntity<?> response = controller.print(123L, FreightCargo.TRUE, ImportExport.EXPORT);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=freight-manifest-123.pdf", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertArrayEquals(pdfBytes, (byte[]) response.getBody());
    }

    @Test
    void print_exception_returnsError() throws Exception {
        when(vesselVoyageService.print(123L, "FALSE", null)).thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> response = controller.print(123L, FreightCargo.FALSE, null);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to generate PDF: PDF generation failed", ((Map<?, ?>) response.getBody()).get("message"));
        assertFalse((boolean) ((Map<?, ?>) response.getBody()).get("success"));
    }
}

