package com.asg.shipping.chargeGroupMaster.controller;


import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargeGroupMaster.service.ChargeGroupMasterService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ChargeGroupMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChargeGroupMasterService service;

    @InjectMocks
    private ChargeGroupMasterController controller;

    private ObjectMapper objectMapper;

    private ChargeGroupMasterRequestDto requestDto;
    private ChargeGroupMasterResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        objectMapper = new ObjectMapper();
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
                .andExpect(status().isOk());

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
    void updateChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        when(service.update(eq(id), any(ChargeGroupMasterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/charge-group-master/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(service).update(eq(id), any(ChargeGroupMasterRequestDto.class));
    }

    @Test
    void getChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        when(service.findById(id)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/charge-group-master/{id}", id))
                .andExpect(status().isOk());

        verify(service).findById(id);
    }

    @Test
    void deleteChargeGroupMaster_Success() throws Exception {
        Long id = 1L;
        doNothing().when(service).delete(id);

        mockMvc.perform(delete("/v1/charge-group-master/{id}", id))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

}
