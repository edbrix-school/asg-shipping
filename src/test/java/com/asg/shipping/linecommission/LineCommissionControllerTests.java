package com.asg.shipping.linecommission;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.linecommission.controller.LineCommissionController;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.service.LineCommissionService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LineCommissionControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private LineCommissionService service;

    @InjectMocks
    private LineCommissionController controller;

    private LineCommissionRequest requestDTO;
    private LineCommissionResponse responseDTO;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        requestDTO = new LineCommissionRequest();
        requestDTO.setLinePoid(1L);
        requestDTO.setCurrencyPoid(1L);
        requestDTO.setTransactionDate(java.time.LocalDate.of(2024, 1, 1));
        requestDTO.setPeriodFrom(java.time.LocalDate.of(2024, 1, 1));
        requestDTO.setPeriodTo(java.time.LocalDate.of(2024, 12, 31));
        requestDTO.setRenewalDate(java.time.LocalDate.of(2024, 12, 31));

        responseDTO = new LineCommissionResponse();
    }

    // ---------- LIST ----------

    private FilterRequestDto buildFilterRequest() {
        return new FilterRequestDto("OR", "N", List.of());
    }

    @Test
    void testListLineCommissions() throws Exception {

        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.listLineCommissions(any(), any(), isNull(), isNull(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/v1/line-commission/list")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFilterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line commission list fetched successfully"));
    }

    // ---------- GET BY ID ----------

    @Test
    void testGetLineCommissionById() throws Exception {

        when(service.getById(eq(1L), eq(100L)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/v1/line-commission/1")
                        .header("X-Group-Poid", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line commission fetched successfully"));
    }

    // ---------- CREATE ----------

    @Test
    void testCreateLineCommission() throws Exception {

        when(service.create(any(LineCommissionRequest.class),
                eq(100L), eq("admin"), eq("DOC-001")))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/line-commission")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line commission created successfully"));
    }

    // ---------- UPDATE ----------

    @Test
    void testUpdateLineCommission() throws Exception {

        when(service.update(eq(1L),
                any(LineCommissionRequest.class),
                eq(100L), eq("admin"), eq("DOC-001")))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/v1/line-commission/1")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line commission updated successfully"));
    }

    // ---------- DELETE ----------

    @Test
    void testDeleteLineCommission() throws Exception {

        doNothing().when(service).delete(eq(1L), any(DeleteReasonDto.class));

        mockMvc.perform(delete("/v1/line-commission/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Line commission deleted successfully"));
    }

    // ---------- LOAD CONTAINER TYPES BY LINE ----------

    @Test
    void testLoadContainerTypesByLine() throws Exception {

        List<ContainerTypeDto> list = List.of(new ContainerTypeDto());

        when(service.loadContainerTypes(eq(10L), eq(100L), eq("admin")))
                .thenReturn(list);

        mockMvc.perform(get("/v1/line-commission/lines/10/container-types")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records loaded"));
    }
}

