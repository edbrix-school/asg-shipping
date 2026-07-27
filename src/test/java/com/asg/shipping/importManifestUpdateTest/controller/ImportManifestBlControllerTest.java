package com.asg.shipping.importManifestUpdateTest.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.importmanifestupdate.controller.ImportManifestBlController;
import com.asg.shipping.importmanifestupdate.dto.ImportManifestUpdateOpsDto;
import com.asg.shipping.importmanifestupdate.service.ImportManifestBlService;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ImportManifestBlControllerTest {

    @Mock
    private ImportManifestBlService service;

    @Spy

    private DocumentDownloadHeaderService downloadHeaderService =

            new DocumentDownloadHeaderService(mock(JdbcTemplate.class));


    @InjectMocks
    private ImportManifestBlController controller;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private ImportManifestService manifestService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getImportManifestList_Success() throws Exception {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("data", "test data");
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("OR", "N", null);
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("123");
            when(service.list( any(FilterRequestDto.class),isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<?> response = controller.getImportManifestList( pageable,filters, null, null);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertEquals("Import Manifest list fetched successfully", responseBody.get("message"));
            verify(service).list( any(FilterRequestDto.class),isNull(),isNull(), any(Pageable.class));
        }
    }




    @Test
    void updateImportManifestBl_Success() {
        // Given
        Long id = 1L;
        ImportManifestUpdateOpsDto updateDto = ImportManifestUpdateOpsDto.builder()
                .blNumber("TEST123")
                .agentReference("AGENT001")
                .build();
        
        ImportManifestUpdateOpsDto expectedResponse = ImportManifestUpdateOpsDto.builder()
                .transactionPoid(id)
                .blNumber("TEST123")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(200L);
            
            when(service.updateImportManifestUpdateOps(eq(id), eq(updateDto), eq(100L), eq(200L)))
                    .thenReturn(expectedResponse);

            // When
            ResponseEntity<?> response = controller.updateImportManifestBl(id, updateDto);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertEquals("Import Manifest BL updated successfully", responseBody.get("message"));
            verify(service).updateImportManifestUpdateOps(eq(id), eq(updateDto), eq(100L), eq(200L));
        }
    }

    @Test
    void getImportManifestBl_Success() {
        // Given
        Long id = 1L;
        ImportManifestUpdateOpsDto expectedResponse = ImportManifestUpdateOpsDto.builder()
                .transactionPoid(id)
                .blNumber("TEST123")
                .agentReference("AGENT001")
                .build();

        when(service.getImportManifestUpdateOps(id)).thenReturn(expectedResponse);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("123");

            // When
            ResponseEntity<?> response = controller.getImportManifestBl(id);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertEquals("Import Manifest BL retrieved successfully", responseBody.get("message"));
            Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
            assertEquals(expectedResponse, result.get("data"));
            verify(service).getImportManifestUpdateOps(id);
        }
    }

    @Test
    void deleteImportManifestBl_Success() {
        // Given
        Long id = 1L;
        doNothing().when(service).deleteImportManifestBl(id,new DeleteReasonDto());

        // When
        ResponseEntity<?> response = controller.deleteImportManifestBl(id,new DeleteReasonDto());

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Import Manifest BL deleted successfully", responseBody.get("message"));
        verify(service).deleteImportManifestBl(id,new DeleteReasonDto());
    }

    @Test
    void updateImportManifestBl_WithNullDto() {
        // Given
        Long id = 1L;
        ImportManifestUpdateOpsDto updateDto = null;
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(200L);
            
            when(service.updateImportManifestUpdateOps(eq(id), eq(updateDto), eq(100L), eq(200L)))
                    .thenThrow(new IllegalArgumentException("Update DTO cannot be null"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                controller.updateImportManifestBl(id, updateDto);
            });
        }
    }

    @Test
    void getImportManifestBl_NotFound() {
        // Given
        Long id = 999L;
        when(service.getImportManifestUpdateOps(id))
                .thenThrow(new RuntimeException("Import Manifest BL not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            controller.getImportManifestBl(id);
        });
        verify(service).getImportManifestUpdateOps(id);
    }

    @Test
    void deleteImportManifestBl_NotFound() {
        Long id = 999L;
        doThrow(new RuntimeException("Import Manifest BL not found"))
                .when(service).deleteImportManifestBl(id, new DeleteReasonDto());

        assertThrows(RuntimeException.class, () -> {
            controller.deleteImportManifestBl(id, new DeleteReasonDto());
        });
        verify(service).deleteImportManifestBl(id, new DeleteReasonDto());
    }



    @Test
    void resendCan_Success() {
        var request = new com.asg.shipping.importmanifestbl.dto.ResendCanRequestDto();
        request.setTransactionPoId(1L);
        request.setUpdateDemurrage("Y");
        var response = com.asg.shipping.importmanifestupdate.dto.ResendCanResponseDto.builder()
                .status("SUCCESS")
                .build();

        when(service.resendCan(1L, "Y")).thenReturn(response);

        ResponseEntity<?> result = controller.resendCan(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).resendCan(1L, "Y");
    }



    @Test
    void getBlStatus_Success() {
        var response = com.asg.shipping.importmanifestupdate.dto.BlStatusResponseDto.builder()
                .status(com.asg.shipping.importmanifestupdate.dto.BlStatusResponseDto.StatusDetails.builder()
                        .jobNo("NEW")
                        .build())
                .build();

        when(service.getBlStatus(1L)).thenReturn(response);

        ResponseEntity<?> result = controller.getBlStatus(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).getBlStatus(1L);
    }

    @Test
    void loadEmailFax_Success() {
        var response = com.asg.shipping.importmanifestupdate.dto.LoadEmailFaxResponseDto.builder()
                .emailFaxDetails(java.util.List.of())
                .build();

        when(service.loadEmailFax(1L, "CONSIGNEE")).thenReturn(response);

        ResponseEntity<?> result = controller.loadEmailFax(1L, "CONSIGNEE");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).loadEmailFax(1L, "CONSIGNEE");
    }

    @Test
    void loadEmailFax_NotFound() {
        when(service.loadEmailFax(999L, "CONSIGNEE"))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Import Manifest BL", "id", 999L));

        ResponseEntity<?> result = controller.loadEmailFax(999L, "CONSIGNEE");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(service).loadEmailFax(999L, "CONSIGNEE");
    }



    @Test
    void resendCan_NotFound() {
        var request = new com.asg.shipping.importmanifestbl.dto.ResendCanRequestDto();
        request.setTransactionPoId(999L);
        request.setUpdateDemurrage("N");

        when(service.resendCan(999L, "N"))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Import Manifest BL", "id", 999L));

        ResponseEntity<?> result = controller.resendCan(request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(service).resendCan(999L, "N");
    }



    @Test
    void getBlStatus_NotFound() {
        when(service.getBlStatus(999L))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Import Manifest BL", "id", 999L));

        ResponseEntity<?> result = controller.getBlStatus(999L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(service).getBlStatus(999L);
    }

    @Test
    void printCargoArrivalNotice_Success() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(manifestService.printCargoArrivalNotice(100L, 1L)).thenReturn(pdfBytes);

        ResponseEntity<?> result = controller.printCargoArrivalNotice(1L, 100L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(pdfBytes, (byte[]) result.getBody());
        verify(manifestService).printCargoArrivalNotice(100L, 1L);
    }

    @Test
    void printCargoArrivalNotice_Error() throws Exception {
        when(manifestService.printCargoArrivalNotice(100L, 1L))
                .thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> result = controller.printCargoArrivalNotice(1L, 100L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("500", responseBody.get("statusCode"));
        verify(manifestService).printCargoArrivalNotice(100L, 1L);
    }

    @Test
    void printUnclearedCargoNotice_Success() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(manifestService.printUnclearedCargoNotice(1L)).thenReturn(pdfBytes);

        ResponseEntity<?> result = controller.printUnclearedCargoNotice(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(pdfBytes, (byte[]) result.getBody());
        verify(manifestService).printUnclearedCargoNotice(1L);
    }

    @Test
    void printUnclearedCargoNotice_Error() throws Exception {
        when(manifestService.printUnclearedCargoNotice(1L))
                .thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> result = controller.printUnclearedCargoNotice(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("500", responseBody.get("statusCode"));
        verify(manifestService).printUnclearedCargoNotice(1L);
    }

    @Test
    void printCargoManifest_Success() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(manifestService.printCargoManifest(1L, true)).thenReturn(pdfBytes);

        ResponseEntity<?> result = controller.printCargoManifest(1L, true);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(pdfBytes, (byte[]) result.getBody());
        verify(manifestService).printCargoManifest(1L, true);
    }

    @Test
    void printCargoManifest_Error() throws Exception {
        when(manifestService.printCargoManifest(1L, false))
                .thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> result = controller.printCargoManifest(1L, false);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertEquals("500", responseBody.get("statusCode"));
        verify(manifestService).printCargoManifest(1L, false);
    }


}
