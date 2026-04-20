package com.asg.shipping.regionmaster;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.regionmaster.controller.RegionMasterController;
import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import com.asg.shipping.regionmaster.service.RegionMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegionMasterControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RegionMasterService service;

    @InjectMocks
    private RegionMasterController controller;

    private RegionMasterRequest request;
    private RegionMasterResponse response;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setup() {
        userContextMock = org.mockito.Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
        userContextMock.when(UserContext::getUserId).thenReturn("admin");
        userContextMock.when(UserContext::getDocumentId).thenReturn("100-480");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        request = new RegionMasterRequest();
        request.setRegionCode("ME");
        request.setRegionName("Middle East");
        request.setActive("Y");

        response = new RegionMasterResponse();
        response.setRegionPoid(1L);
        response.setRegionCode("ME");
        response.setRegionName("Middle East");
        response.setActive("Y");
    }

    private FilterRequestDto buildFilterRequest() {
        return new FilterRequestDto("OR", "N", List.of());
    }

    // ---------- LIST ----------
    @Test
    void testSearchRegionMasters() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.listRegionMasters(any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/v1/region-master/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFilterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Region masters retrieved successfully"));
    }

    @Test
    void testSearchRegionMasters_WhenServiceThrows_ReturnsInternalServerError() throws Exception {
        when(service.listRegionMasters(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/v1/region-master/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFilterRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Unable to fetch region masters: boom"));
    }

    // ---------- GET BY ID ----------
    @Test
    void testGetById() throws Exception {
        when(service.getById(eq(1L), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(get("/v1/region-master/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.regionCode").value("ME"));
    }

    // ---------- CREATE ----------
    @Test
    void testCreateRegionMaster() throws Exception {
        when(service.create(any(), eq(1L), eq("admin"), eq("100-480")))
                .thenReturn(response);

        mockMvc.perform(post("/v1/region-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Region master created successfully"));
    }

    // ---------- UPDATE ----------
    @Test
    void testUpdateRegionMaster() throws Exception {
        when(service.update(eq(1L), any(), eq(1L), eq("admin"), eq("100-480")))
                .thenReturn(response);

        mockMvc.perform(put("/v1/region-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Region master updated successfully"));
    }

    // ---------- TOGGLE ACTIVE ----------
    @Test
    void testToggleActiveStatus() throws Exception {
        doNothing().when(service)
                .toggleActiveStatus(1L, 1L, "admin");

        mockMvc.perform(put("/v1/region-master/1/activate"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ---------- DELETE ----------
    @Test
    void testDeleteRegionMaster() throws Exception {
        doNothing().when(service)
                .delete(eq(1L), any());

        mockMvc.perform(delete("/v1/region-master/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deleteReason\":\"Test deletion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Region master deleted successfully"));
    }

    @AfterEach
    void closeMock() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }
}

