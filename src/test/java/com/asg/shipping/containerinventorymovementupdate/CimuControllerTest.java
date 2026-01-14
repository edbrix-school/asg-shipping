package com.asg.shipping.containerinventorymovementupdate;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.containerinventorymovementupdate.controller.CimuController;
import com.asg.shipping.containerinventorymovementupdate.dto.*;
import com.asg.shipping.containerinventorymovementupdate.service.CimuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CimuControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CimuService cimuService;

    @InjectMocks
    private CimuController controller;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn("admin");
        userContextMock.when(UserContext::getUserPoid).thenReturn(1L);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(10L);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(20L);
        userContextMock.when(UserContext::getActionRequested).thenReturn("VIEW");
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    // ---------------- suggestContainers ----------------

    @Test
    void testSuggestContainers() throws Exception {
        when(cimuService.suggestContainers("CONT"))
                .thenReturn(new SuggestContainerResponse());

        mockMvc.perform(get("/v1/container-inventory-movement-update/containers/suggest")
                        .param("query", "CONT")
                        .header("X-Document-Id", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Suggestions fetched successfully"));
    }

    // ---------------- query ----------------

    @Test
    void testQuery() throws Exception {
        QueryCimuRequest request = new QueryCimuRequest();
        request.setContainerNo("CONT001");

        when(cimuService.queryScreenData(any()))
                .thenReturn(new QueryCimuResponse());

        mockMvc.perform(post("/v1/container-inventory-movement-update/query")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container data fetched successfully"));
    }

    // ---------------- update ----------------

    @Test
    void testUpdate() throws Exception {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setContainerNo("CONT001");

        when(cimuService.updateContainerData(any()))
                .thenReturn(new UpdateCimuResponse());

        mockMvc.perform(post("/v1/container-inventory-movement-update/update")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Update processed"));
    }

    // ---------------- socUpdate ----------------

    @Test
    void testSocUpdate() throws Exception {
        SocUpdateRequest request = new SocUpdateRequest();
        request.setBlNumber("BL001");

        when(cimuService.socUpdate(any()))
                .thenReturn(new SocUpdateResponse());

        mockMvc.perform(post("/v1/container-inventory-movement-update/soc-update")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("SOC update processed"));
    }

    // ---------------- demurrage calculate ----------------

    @Test
    void testCalculateDemurrage() throws Exception {
        DemurrageCalculateRequest request = new DemurrageCalculateRequest();
        request.setContainerNo("CONT001");

        when(cimuService.calculateDemurrage(any()))
                .thenReturn(new DemurrageCalculateResponse());

        mockMvc.perform(post("/v1/container-inventory-movement-update/demurrage/calculate")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Demurrage calculated successfully"));
    }

    // ---------------- importFile (multipart) ----------------

    @Test
    void testImportFile() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "inspection.xlsx",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        "dummy-data".getBytes()
                );

        when(cimuService.importFile(any()))
                .thenReturn(new InspectionUploadResponse());

        mockMvc.perform(multipart("/v1/container-inventory-movement-update/container-inspection/import")
                        .file(file)
                        .header("X-Document-Id", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("File imported successfully"));
    }

    // ---------------- loadContainerDetails ----------------

    @Test
    void testLoadContainerDetails() throws Exception {
        when(cimuService.loadContainerDetails())
                .thenReturn(new InspectionLoadResponse());

        mockMvc.perform(post("/v1/container-inventory-movement-update/container-inspection/load")
                        .header("X-Document-Id", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container details loaded"));
    }
}

