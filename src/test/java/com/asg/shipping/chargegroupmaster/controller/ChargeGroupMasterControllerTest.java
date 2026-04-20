package com.asg.shipping.chargegroupmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargegroupmaster.service.ChargeGroupMasterService;
import com.asg.shipping.exceptions.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ChargeGroupMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChargeGroupMasterService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ChargeGroupMasterController controller;

    private ObjectMapper objectMapper;

    private ChargeGroupMasterRequestDto requestDto;
    private ChargeGroupMasterResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        requestDto = ChargeGroupMasterRequestDto.builder()
                .chargeGroupCode("CGM001")
                .chargeGroupName("Test Charge Group")
                .chargeGroupName2("Test Charge Group 2")
                .chargeGlPayable(1001L)
                .chargeGlSale(2001L)
                .chargeGlCostSale(3001L)
                .linewisePayablePosting("Y")
                .seqNo(1L)
                .active("Y")
                .build();

        responseDto = ChargeGroupMasterResponseDto.builder()
                .chargeGroupPoid(1L)
                .groupPoid(100L)
                .chargeGroupCode("CGM001")
                .chargeGroupName("Test Charge Group")
                .chargeGroupName2("Test Charge Group 2")
                .chargeGlPayable(1001L)
                .chargeGlSale(2001L)
                .chargeGlCostSale(3001L)
                .linewisePayablePosting("Y")
                .active("Y")
                .seqNo(1L)
                .glPrefix("GL")
                .createdBy("testuser")
                .createdDate(LocalDateTime.now())
                .lastModifiedBy("testuser")
                .lastModifiedDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createChargeGroupMaster_Success() throws Exception {
        when(service.create(any(ChargeGroupMasterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/charge-group-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Charge Group Master created successfully"));

        verify(service).create(any(ChargeGroupMasterRequestDto.class));
    }

    @Test
    void createChargeGroupMaster_ValidationError() throws Exception {
        ChargeGroupMasterRequestDto invalidDto = ChargeGroupMasterRequestDto.builder()
                .chargeGroupCode("")
                .chargeGroupName("")
                .build();

        mockMvc.perform(post("/v1/charge-group-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(ChargeGroupMasterRequestDto.class));
    }

    @Test
    void createChargeGroupMaster_ServiceException() throws Exception {
        when(service.create(any(ChargeGroupMasterRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Charge Group Code already exists"));

        mockMvc.perform(post("/v1/charge-group-master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Charge Group Code already exists"));
    }

    @Test
    void updateChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        when(service.update(eq(id), any(ChargeGroupMasterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/charge-group-master/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Charge Group Master updated successfully"));

        verify(service).update(eq(id), any(ChargeGroupMasterRequestDto.class));
    }

    @Test
    void updateChargeGroupMaster_NotFound() throws Exception {
        Long id = 999L;
        when(service.update(eq(id), any(ChargeGroupMasterRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Charge Group not found", "ChargeGroupPoid", id));

        mockMvc.perform(put("/v1/charge-group-master/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        when(service.findById(id)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/charge-group-master/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Charge group master retrieved successfully"));

        verify(service).findById(id);
    }

    @Test
    void getChargeGroupMaster_NotFound() throws Exception {
        Long id = 999L;
        when(service.findById(id))
                .thenThrow(new ResourceNotFoundException("Charge Group not found", "ChargeGroupPoid", id));

        mockMvc.perform(get("/v1/charge-group-master/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        doNothing().when(service).delete(eq(id), any());

        mockMvc.perform(delete("/v1/charge-group-master/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Charge group master deleted successfully"));

        verify(service).delete(eq(id), any());
    }

    @Test
    void deleteChargeGroupMaster_NotFound() throws Exception {
        Long id = 999L;
        doThrow(new ResourceNotFoundException("Charge Group not found", "ChargeGroupPoid", id))
                .when(service).delete(eq(id), any());

        mockMvc.perform(delete("/v1/charge-group-master/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getChargeGroupMasterList_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("content", java.util.List.of());
        result.put("totalElements", 0);

        when(service.listChargeGroupMaster(any(), any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/v1/charge-group-master/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Charge Group Master list fetched successfully"));

        verify(service).listChargeGroupMaster(any(), any(), any(), any(), any());
    }

    @Test
    void getChargeGroupMasterList_WithoutBody() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("content", java.util.List.of());

        when(service.listChargeGroupMaster(any(), any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/v1/charge-group-master/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getChargeGroupMasterList_WithDateParams() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("content", java.util.List.of());

        when(service.listChargeGroupMaster(any(), any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/v1/charge-group-master/list")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getChargeGroupMasterList_ServiceException() throws Exception {
        when(service.listChargeGroupMaster(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/v1/charge-group-master/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
