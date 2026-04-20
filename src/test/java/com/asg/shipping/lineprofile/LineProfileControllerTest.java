package com.asg.shipping.lineprofile;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.shipping.lineprofile.controller.LineProfileController;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileLineDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import com.asg.shipping.lineprofile.service.LineProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LineProfileControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LineProfileService service;

    @InjectMocks
    private LineProfileController controller;

    private LineProfileRequest request;
    private LineProfileResponse response;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        request = new LineProfileRequest();
        request.setLinePoid(10L);
        request.setRegionPoids(List.of(1L, 2L));
        request.setRemarks("Remarks");
        request.setActive("Y");

        response = new LineProfileResponse();
        response.setLineProfilePoid(1L);
        response.setLinePoid(10L);
    }

    @Test
    void testListLineProfiles() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.listLineProfiles(any(), any(), any())).thenReturn(result);

        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of());
        mockMvc.perform(post("/v1/line-profile/list")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line profile list fetched successfully"));
    }

    @Test
    void testGetById() throws Exception {
        when(service.getById(eq(1L), eq(100L))).thenReturn(response);

        mockMvc.perform(get("/v1/line-profile/1")
                        .header("X-Group-Poid", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Line profile fetched successfully"))
                .andExpect(jsonPath("$.result.data.lineProfilePoid").value(1));
    }

    @Test
    void testCreate() throws Exception {
        when(service.create(any(LineProfileRequest.class), eq(100L), eq("admin"), eq("DOC-1")))
                .thenReturn(response);

        mockMvc.perform(post("/v1/line-profile")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Line profile created successfully"));
    }

    @Test
    void testUpdate() throws Exception {
        when(service.update(eq(1L), any(LineProfileRequest.class), eq(100L), eq("admin"), eq("DOC-1")))
                .thenReturn(response);

        mockMvc.perform(put("/v1/line-profile/1")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "DOC-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Line profile updated successfully"));
    }

    @Test
    void testFetchLineDetails() throws Exception {
        LineProfileLineDetailsResponse lineDetails = new LineProfileLineDetailsResponse();
        lineDetails.setLinePoid(10L);
        lineDetails.setLineCode("LINE01");
        lineDetails.setLineName("MAERSK");
        lineDetails.setCountryPoid(100L);
        lineDetails.setCountryDet(new LovGetListDto(100L, "US", "USA", 100L, "USA", null, null));
        lineDetails.setAgencyPoid(1L);
        lineDetails.setAgencyTypeDet(new LovGetListDto(1L, "MLO", "Main Line Operator", 1L, "Main Line Operator", null, null));

        when(service.fetchLineDetails(eq(10L), eq(100L), eq(200L), eq(300L))).thenReturn(lineDetails);

        mockMvc.perform(get("/v1/line-profile/lines/10/details")
                        .header("X-Group-Poid", 100L)
                        .header("X-Company-Poid", 200L)
                        .header("X-User-Poid", 300L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Line details fetched successfully"));
    }

    @Test
    void testFetchLineDetails_NotFound() throws Exception {
        when(service.fetchLineDetails(eq(10L), eq(100L), eq(200L), eq(300L))).thenReturn(null);

        mockMvc.perform(get("/v1/line-profile/lines/10/details")
                        .header("X-Group-Poid", 100L)
                        .header("X-Company-Poid", 200L)
                        .header("X-User-Poid", 300L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Line details not found"));
    }

    @Test
    void testFetchAgreementDetails() throws Exception {
        LineProfileAgreementDetailsResponse agreement = new LineProfileAgreementDetailsResponse();
        agreement.setAgreementPoid(200L);
        agreement.setAgreementId("AGR-1");

        when(service.fetchAgreementDetails(eq(200L), eq(100L), eq(200L), eq(300L))).thenReturn(agreement);

        mockMvc.perform(get("/v1/line-profile/agreements/200/details")
                        .header("X-Group-Poid", 100L)
                        .header("X-Company-Poid", 200L)
                        .header("X-User-Poid", 300L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Agreement details fetched successfully"));
    }

    @Test
    void testFetchAgreementDetails_NotFound() throws Exception {
        when(service.fetchAgreementDetails(eq(200L), eq(100L), eq(200L), eq(300L))).thenReturn(null);

        mockMvc.perform(get("/v1/line-profile/agreements/200/details")
                        .header("X-Group-Poid", 100L)
                        .header("X-Company-Poid", 200L)
                        .header("X-User-Poid", 300L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Agreement details not found"));
    }
}

