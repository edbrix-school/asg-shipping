package com.asg.shipping.shipcommisiontransferTest.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shipcommisiontransfer.controller.ShipCommissionTransferController;
import com.asg.shipping.shipcommisiontransfer.dto.CalculateCommissionRequestDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferCreateDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferUpdateDTO;
import com.asg.shipping.shipcommisiontransfer.service.ShipCommissionTransferService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShipCommissionTransferControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ShipCommissionTransferService commissionTransferService;

    @InjectMocks
    private ShipCommissionTransferController controller;

    private ShipCommissionTransferCreateDTO createDTO;
    private ShipCommissionTransferUpdateDTO updateDTO;
    private ShipCommissionTransferDto responseDTO;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new com.asg.shipping.exceptions.GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        createDTO = ShipCommissionTransferCreateDTO.builder().build();
        updateDTO = ShipCommissionTransferUpdateDTO.builder().build();
        responseDTO = ShipCommissionTransferDto.builder().transactionPoid(1L).build();
    }

    @Test
    void testList() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(commissionTransferService.searchShipCommissionTransfer(any(), any(), any(), any(),any()))
                .thenReturn(result);

        mockMvc.perform(post("/v1/ship-commission-transfer/list")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FilterRequestDto("OR", "N", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ship Commission Transfer list fetched successfully"));
    }

    @Test
    void testGet() throws Exception {
        when(commissionTransferService.getShipCommissionTransfer(eq(1L)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/v1/ship-commission-transfer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ship Commission Transfer fetched successfully"));
    }

    @Test
    void testCreate() throws Exception {
        when(commissionTransferService.createShipCommissionTransfer(any(ShipCommissionTransferCreateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/ship-commission-transfer")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ship Commission Transfer created successfully"));
    }

    @Test
    void testUpdate() throws Exception {
        when(commissionTransferService.updateShipCommissionTransfer(eq(1L), any(ShipCommissionTransferUpdateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/v1/ship-commission-transfer/1")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ship Commission Transfer updated successfully"));
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(commissionTransferService).deleteShipCommissionTransfer(eq(1L));

        mockMvc.perform(delete("/v1/ship-commission-transfer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ship Commission Transfer deleted successfully"));
    }

    @Test
    void testCalculateCommission() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("calculatedDetails", List.of());

        when(commissionTransferService.calculateCommission(eq(1L), any(CalculateCommissionRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/ship-commission-transfer/1/calculate-commission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CalculateCommissionRequestDTO.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Commission calculated successfully"));
    }

    @Test
    void testLoadFromVoyage() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("loadedDetails", List.of());

        when(commissionTransferService.loadFromVoyage(eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/ship-commission-transfer/1/load-from-voyage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Commission data loaded from voyage successfully"));
    }

    @Test
    void testInsertPdaCommission() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("inserted", true);

        when(commissionTransferService.insertPdaCommission(eq(1L)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/ship-commission-transfer/1/insert-pda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testCalculateCommissionWithNullRequest() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("calculatedDetails", 5);
        result.put("totalCommission", new BigDecimal("1000.00"));

        when(commissionTransferService.calculateCommission(eq(1L), any(CalculateCommissionRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/ship-commission-transfer/1/calculate-commission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Commission calculated successfully"));
    }

    @Test
    void testListWithException() throws Exception {
        when(commissionTransferService.searchShipCommissionTransfer(any(), any(),any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/v1/ship-commission-transfer/list")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetWithInvalidId() throws Exception {

        when(commissionTransferService.getShipCommissionTransfer(eq(0L)))
                .thenThrow(new IllegalArgumentException("Invalid ID"));

        mockMvc.perform(get("/v1/ship-commission-transfer/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUpdateWithInvalidId() throws Exception {
        when(commissionTransferService.updateShipCommissionTransfer(eq(-1L), any(ShipCommissionTransferUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid ID"));

        mockMvc.perform(put("/v1/ship-commission-transfer/-1")
                        .header("X-Document-Id", "DOC-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testDeleteWithInvalidId() throws Exception {
        doThrow(new IllegalArgumentException("Invalid ID"))
                .when(commissionTransferService).deleteShipCommissionTransfer(eq(0L));

        mockMvc.perform(delete("/v1/ship-commission-transfer/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetNotFound() throws Exception {
        when(commissionTransferService.getShipCommissionTransfer(eq(1L)))
                .thenThrow(new com.asg.shipping.exceptions.ResourceNotFoundException(
                        "Ship Commission Transfer", "transactionPoid", "1"));

        mockMvc.perform(get("/v1/ship-commission-transfer/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCalculateCommissionNotFound() throws Exception {
        when(commissionTransferService.calculateCommission(eq(1L), any(CalculateCommissionRequestDTO.class)))
                .thenThrow(new com.asg.shipping.exceptions.ResourceNotFoundException(
                        "Ship Commission Transfer", "transactionPoid", "1"));

        mockMvc.perform(post("/v1/ship-commission-transfer/1/calculate-commission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CalculateCommissionRequestDTO.builder().build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}