package com.asg.shipping.importmanifestbl.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImportManifestControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private ImportManifestService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ImportManifestController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testCreate() throws Exception {
        ImportManifestBlDto request = ImportManifestBlDto.builder()
                .blNumber("BL123")
                .vesselVoyagePoid(100L)
                .build();
        ImportManifestBlResponseDto response = new ImportManifestBlResponseDto();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(service.createImportManifestBl(any(), anyLong(), anyLong())).thenReturn(response);

            mockMvc.perform(post("/v1/import-manifest-bl")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Import Manifest BL created successfully"));
        }
    }

    @Test
    void testGetImportManifest() throws Exception {
        ImportManifestBlDto response = ImportManifestBlDto.builder().build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(service.getImportManifest(1L)).thenReturn(response);

            mockMvc.perform(get("/v1/import-manifest-bl/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Import Manifest BL retrieved successfully"));
        }
    }

    @Test
    void testDelete() throws Exception {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        deleteReason.setDeleteReason("Testing");

        mockMvc.perform(delete("/v1/import-manifest-bl/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Import Manifest BL deleted successfully"));
    }

    @Test
    void testUpdateImportManifestBl() throws Exception {
        ImportManifestBlDto request = ImportManifestBlDto.builder().build();
        ImportManifestBlResponseDto response = new ImportManifestBlResponseDto();

        when(service.updateImportManifestBl(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/v1/import-manifest-bl/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Import Manifest BL updated successfully"));
    }

    @Test
    void testList() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
        Map<String, Object> response = new HashMap<>();

        when(service.list(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/import-manifest-bl/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Import Manifest BL list retrieved successfully"));
    }

    @Test
    void testGetDefaultValues() throws Exception {
        DefaultValueDto response = new DefaultValueDto();
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(service.getDefaultValues("DOC-001")).thenReturn(response);

            mockMvc.perform(get("/v1/import-manifest-bl/default-values"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Default values retrieved successfully"));
        }
    }

    @Test
    void testResendCan() throws Exception {
        ResendCanRequestDto request = new ResendCanRequestDto();
        request.setTransactionPoId(1L);
        request.setUpdateDemurrage("Y");
        ResendCanResponseDto response = new ResendCanResponseDto();

        when(service.resendCan(eq(1L), eq("Y"))).thenReturn(response);

        mockMvc.perform(post("/v1/import-manifest-bl/resend-can")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("CAN resent successfully"));
    }

    @Test
    void testUpdateEmailVerification() throws Exception {
        EmailVerificationRequestDto request = new EmailVerificationRequestDto();
        EmailVerificationResponseDto response = new EmailVerificationResponseDto();

        when(service.updateEmailVerification(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/v1/import-manifest-bl/1/update-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verification updated successfully"));
    }

    @Test
    void testSaveEmails() throws Exception {
        SaveEmailsRequestDto request = new SaveEmailsRequestDto();
        request.setEmailsText("test@test.com");
        when(service.saveEmails(eq(1L), any())).thenReturn("Emails saved");

        mockMvc.perform(post("/v1/import-manifest-bl/1/save-emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Emails saved"));
    }

    @Test
    void testLoadEmailFax() throws Exception {
        LoadEmailFaxResponseDto response = LoadEmailFaxResponseDto.builder().build();

        when(service.loadEmailFax(eq(BigDecimal.valueOf(1L)), eq("CONSIGNEE"))).thenReturn(response);

        mockMvc.perform(get("/v1/import-manifest-bl/load-email-fax")
                        .param("addressMasterPoid", "1")
                        .param("addressType", "CONSIGNEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email/Fax data loaded successfully"));
    }

    @Test
    void testGetBlStatus() throws Exception {
        BlStatusResponseDto response = new BlStatusResponseDto();
        when(service.getBlStatus(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/import-manifest-bl/1/bl-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("BL status retrieved successfully"));
    }

    @Test
    void testGetContainerTypesByVoyage() throws Exception {
        ContainersDropDownDto response = new ContainersDropDownDto();
        when(service.getContainerTypesByVoyage(anyLong())).thenReturn(response);

        mockMvc.perform(get("/v1/import-manifest-bl/containers-dropdown")
                        .param("voyageTransPoid", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void testPrintUnclearedCargoNotice() throws Exception {
        when(service.printUnclearedCargoNotice(1L)).thenReturn(new byte[0]);

        mockMvc.perform(get("/v1/import-manifest-bl/uncleared-cargo-notice/1"))
                .andExpect(status().isOk())
                .andExpect(status().isOk());
    }

    @Test
    void testPrintCargoArrivalNotice() throws Exception {
        when(service.printCargoArrivalNotice(anyLong(), anyLong())).thenReturn(new byte[0]);

        mockMvc.perform(get("/v1/import-manifest-bl/cargo-arrival-notice/1")
                        .param("voyageTransactionPoid", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void testSendEdiEmails() throws Exception {
        SendEdiEmailsResponseDto response = new SendEdiEmailsResponseDto();

        when(service.sendEdiEmails(anyLong())).thenReturn(response);

        mockMvc.perform(get("/v1/import-manifest-bl/1/get-edi-emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("EDI emails retrieved successfully"));
    }

    @Test
    void testPrintProformaInvoice() throws Exception {
        when(service.printProformaInvoice(anyLong(), any(), any())).thenReturn(new byte[0]);

        mockMvc.perform(get("/v1/import-manifest-bl/proforma-invoice/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testPrintCargoManifest() throws Exception {
        when(service.printCargoManifest(anyLong(), anyBoolean())).thenReturn(new byte[0]);

        mockMvc.perform(get("/v1/import-manifest-bl/cargo-manifest-print/1")
                        .param("isCargoManifestPrint", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void testPrintCheckPortCharges() throws Exception {
        when(service.printCheckPortCharges(1L)).thenReturn(new byte[0]);

        mockMvc.perform(get("/v1/import-manifest-bl/port-charges/1"))
                .andExpect(status().isOk());
    }
}
