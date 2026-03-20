package com.asg.shipping.vesselvoyagecreation.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.vesselvoyagecreation.dto.CurrencyUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentTransferRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlFilter;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlTab;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageResponse;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageUpsertRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.asg.shipping.vesselvoyagecreation.service.VesselVoyageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VesselVoyageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VesselVoyageService vesselVoyageService;

    @InjectMocks
    private VesselVoyageController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void list_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("SEARCH");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-101");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(10L);

            Map<String, Object> mockResult = Collections.singletonMap("content", Collections.emptyList());
            when(vesselVoyageService.listVoyages(any(), any(), any(), any(), any())).thenReturn(mockResult);

            FilterRequestDto body = new FilterRequestDto(null, null, null);

            mockMvc.perform(post("/v1/vessel-voyage-creation-line-edi/list")
                            .param("page", "0")
                            .param("size", "20")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Vessel voyages fetched successfully"));

            verify(vesselVoyageService).listVoyages(any(), any(Pageable.class), any(), any(), any());
        }
    }

    @Test
    void list_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/vessel-voyage-creation-line-edi/list")
                        .param("page", "0")
                        .param("size", "20")
                        .param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vesselVoyageService);
    }

    @Test
    void getVoyage_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("VIEW");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(10L);

            when(vesselVoyageService.getVoyage(1L)).thenReturn(new VoyageResponse());

            mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Vessel voyage fetched successfully"));

            verify(vesselVoyageService).getVoyage(1L);
        }
    }

    @Test
    void createVoyage_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("CREATE");
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            VoyageUpsertRequest request = VoyageUpsertRequest.builder()
                    .voyageNo("V001")
                    .linePoid(10L)
                    .vesselPoid(20L)
                    .expectedDate(now)
                    .arrivalDate(now.plusHours(1))
                    .sailDate(now.plusHours(2))
                    .preArrivalMsgVessel(now.minusHours(2))
                    .preArrivalMsgPort(now.minusHours(1))
                    .entryInGctos(now)
                    .entryInMarassi(now)
                    .build();

            when(vesselVoyageService.createVoyage(any(VoyageUpsertRequest.class))).thenReturn(new VoyageResponse());

            mockMvc.perform(post("/v1/vessel-voyage-creation-line-edi")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Vessel voyage created successfully"));

            verify(vesselVoyageService).createVoyage(any(VoyageUpsertRequest.class));
        }
    }

    @Test
    void updateVoyage_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getActionRequested).thenReturn("UPDATE");
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            VoyageUpsertRequest request = VoyageUpsertRequest.builder()
                    .voyageNo("V001")
                    .linePoid(10L)
                    .vesselPoid(20L)
                    .expectedDate(now)
                    .arrivalDate(now.plusHours(1))
                    .sailDate(now.plusHours(2))
                    .preArrivalMsgVessel(now.minusHours(2))
                    .preArrivalMsgPort(now.minusHours(1))
                    .entryInGctos(now)
                    .entryInMarassi(now)
                    .build();

            when(vesselVoyageService.updateVoyage(eq(1L), any(VoyageUpsertRequest.class)))
                    .thenReturn(new VoyageResponse());

            mockMvc.perform(put("/v1/vessel-voyage-creation-line-edi/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Vessel voyage updated successfully"));

            verify(vesselVoyageService).updateVoyage(eq(1L), any(VoyageUpsertRequest.class));
        }
    }

    @Test
    void listBls_Success() throws Exception {
        when(vesselVoyageService.listBls(eq(1L), eq(VoyageBlTab.HOLD), eq(VoyageBlFilter.ALL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/1/bls")
                        .param("tab", "HOLD")
                        .param("filter", "ALL")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("BLs fetched successfully"));

        verify(vesselVoyageService).listBls(eq(1L), eq(VoyageBlTab.HOLD), eq(VoyageBlFilter.ALL), any(Pageable.class));
    }

    @Test
    void ediErrors_Success() throws Exception {
        when(vesselVoyageService.getEdiErrors(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/1/edi/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("EDI errors fetched successfully"));

        verify(vesselVoyageService).getEdiErrors(1L);
    }

    @Test
    void ediReprocess_Success() throws Exception {
        when(vesselVoyageService.reprocessEdi(1L)).thenReturn("OK");

        mockMvc.perform(post("/v1/vessel-voyage-creation-line-edi/1/edi/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("EDI reprocess completed"));

        verify(vesselVoyageService).reprocessEdi(1L);
    }

    @Test
    void ediUpload_Success() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.edi");
        when(mockFile.getSize()).thenReturn(10L);
        when(mockFile.isEmpty()).thenReturn(false);

        when(vesselVoyageService.uploadAndProcessEdi(eq(1L), any(MultipartFile.class))).thenReturn("OK");

        mockMvc.perform(multipart("/v1/vessel-voyage-creation-line-edi/1/edi/upload")
                        .file("file", "DATA".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("EDI upload processed"));

        verify(vesselVoyageService).uploadAndProcessEdi(eq(1L), any(MultipartFile.class));
    }

    @Test
    void deleteVoyage_Success() throws Exception {
        mockMvc.perform(delete("/v1/vessel-voyage-creation-line-edi/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vessel voyage deleted successfully"));

        verify(vesselVoyageService).deleteVoyage(1L);
    }

    @Test
    void downloadExcel_Success() throws Exception {
        Resource resource = new ByteArrayResource("excel".getBytes()) {
            @Override
            public String getFilename() {
                return "Discharge_list.xlsx";
            }
        };
        when(vesselVoyageService.downloadExcelExport(1L, "apmt-discharge")).thenReturn(resource);

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/1/exports/excel/apmt-discharge"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("Discharge_list.xlsx")));

        verify(vesselVoyageService).downloadExcelExport(1L, "apmt-discharge");
    }

    @Test
    void downloadManifest_Success() throws Exception {
        Resource resource = new ByteArrayResource("pdf".getBytes()) {
            @Override
            public String getFilename() {
                return "manifest.pdf";
            }
        };
        when(vesselVoyageService.downloadManifestReport(1L, "FALSE", "BOTH")).thenReturn(resource);

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/1/reports/manifest")
                        .param("freightCargo", "FALSE")
                        .param("importExport", "BOTH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("manifest.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(vesselVoyageService).downloadManifestReport(1L, "FALSE", "BOTH");
    }

    @Test
    void print_Success() throws Exception {
        when(vesselVoyageService.print(eq(1L), eq("FALSE"), isNull())).thenReturn("PDF".getBytes());

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/print/1")
                        .param("freightCargo", "FALSE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("cargo-manifest-1.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(vesselVoyageService).print(eq(1L), eq("FALSE"), isNull());
    }

    @Test
    void print_Error_ReturnsApiError() throws Exception {
        when(vesselVoyageService.print(anyLong(), anyString(), any()))
                .thenThrow(new com.asg.shipping.exceptions.CustomException("Failed", 500));

        mockMvc.perform(get("/v1/vessel-voyage-creation-line-edi/print/1")
                        .param("freightCargo", "FALSE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}

