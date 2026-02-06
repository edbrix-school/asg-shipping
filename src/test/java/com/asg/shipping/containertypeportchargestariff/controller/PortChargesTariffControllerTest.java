package com.asg.shipping.containertypeportchargestariff.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import com.asg.shipping.containertypeportchargestariff.service.PortChargesTariffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PortChargesTariffControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortChargesTariffService portChargesTariffService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private PortChargesTariffController controller;

    private ObjectMapper objectMapper;
    private PortChargesTariffDto mockTariffDto;
    private PortChargesTariffCreateDto createDto;
    private PortChargesTariffUpdateDto updateDto;
    private ValidateOverlapRequestDto overlapRequest;
    private DeleteReasonDto deleteReasonDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockTariffDto = PortChargesTariffDto.builder()
                .transactionPoid(1L)
                .description("Test Tariff")
                .portPoid(200L)
                .chargeLinePoid(300L)
                .chargeDivision("DIV1")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        createDto = new PortChargesTariffCreateDto();
        createDto.setDescription("New Tariff");
        createDto.setPortPoid(200L);
        createDto.setChargeLinePoid(300L);
        createDto.setChargeDivision("DIV1");
        createDto.setPeriodFrom(LocalDate.of(2024, 1, 1));
        createDto.setPeriodTo(LocalDate.of(2024, 12, 31));

        updateDto = new PortChargesTariffUpdateDto();
        updateDto.setDescription("Updated Tariff");
        updateDto.setPortPoid(200L);
        updateDto.setChargeLinePoid(300L);
        updateDto.setChargeDivision("DIV1");
        updateDto.setPeriodFrom(LocalDate.of(2024, 1, 1));
        updateDto.setPeriodTo(LocalDate.of(2024, 12, 31));

        overlapRequest = new ValidateOverlapRequestDto();
        overlapRequest.setPortPoid(200L);
        overlapRequest.setChargeLinePoid(300L);
        overlapRequest.setChargeDivision("DIV1");
        overlapRequest.setPeriodFrom(LocalDate.of(2024, 1, 1));
        overlapRequest.setPeriodTo(LocalDate.of(2024, 12, 31));

        deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion");
    }

    @Test
    void testGetPortChargesTariff() throws Exception {
        Long tariffId = 1L;

        when(portChargesTariffService.getPortChargesTariff(tariffId)).thenReturn(mockTariffDto);

        mockMvc.perform(get("/v1/container-type-port-charges-tariff/{id}", tariffId))
                .andExpect(status().isOk());

        verify(portChargesTariffService).getPortChargesTariff(tariffId);
    }

    @Test
    void testCreatePortChargesTariff() throws Exception {
        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getUserPoid).thenReturn(50L);

            when(portChargesTariffService.createPortChargesTariff(any(PortChargesTariffCreateDto.class), eq(100L), eq(50L)))
                    .thenReturn(mockTariffDto);

            mockMvc.perform(post("/v1/container-type-port-charges-tariff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk());

            verify(portChargesTariffService).createPortChargesTariff(any(PortChargesTariffCreateDto.class), eq(100L), eq(50L));
        }
    }

    @Test
    void testUpdatePortChargesTariff() throws Exception {
        Long tariffId = 1L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(100L);
            userContext.when(UserContext::getUserPoid).thenReturn(50L);

            when(portChargesTariffService.updatePortChargesTariff(eq(tariffId), any(PortChargesTariffUpdateDto.class), eq(100L), eq(50L)))
                    .thenReturn(mockTariffDto);

            mockMvc.perform(put("/v1/container-type-port-charges-tariff/{id}", tariffId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk());

            verify(portChargesTariffService).updatePortChargesTariff(eq(tariffId), any(PortChargesTariffUpdateDto.class), eq(100L), eq(50L));
        }
    }

    @Test
    void testDeletePortChargesTariff() throws Exception {
        Long tariffId = 1L;

        doNothing().when(portChargesTariffService).deletePortChargesTariff(tariffId, deleteReasonDto);

        mockMvc.perform(delete("/v1/container-type-port-charges-tariff/{id}", tariffId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk());

        verify(portChargesTariffService).deletePortChargesTariff(eq(tariffId), any(DeleteReasonDto.class));
    }

    @Test
    void testValidateOverlap() throws Exception {
        ValidateOverlapResponseDto mockResponse = ValidateOverlapResponseDto.builder()
                .overlapping(false)
                .conflicts(List.of())
                .build();

        when(portChargesTariffService.validateOverlap(any(ValidateOverlapRequestDto.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/v1/container-type-port-charges-tariff/validate-overlap")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(overlapRequest)))
                .andExpect(status().isOk());

        verify(portChargesTariffService).validateOverlap(any(ValidateOverlapRequestDto.class));
    }
}