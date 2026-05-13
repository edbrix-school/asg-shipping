package com.asg.shipping.shippingmanifestcorrector.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorCreateDTO;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorBlAutoPopulateDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorUpdateDTO;
import com.asg.shipping.shippingmanifestcorrector.service.ManifestCorrectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManifestCorrectorControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ManifestCorrectorService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ManifestCorrectorController controller;

    private ManifestCorrectorCreateDTO createDTO;
    private ManifestCorrectorUpdateDTO updateDTO;
    private ManifestCorrectorDto responseDTO;
    private MockedStatic<UserContext> userContext;

    @BeforeEach
    void setup() {
        userContext = mockStatic(UserContext.class);
        userContext.when(UserContext::getDocumentId).thenReturn("100-143");
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        createDTO = new ManifestCorrectorCreateDTO();
        createDTO.setBlNumber("12345");
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlReprint("Y");

        updateDTO = new ManifestCorrectorUpdateDTO();
        updateDTO.setBlNumber("12345");

        responseDTO = new ManifestCorrectorDto();
        responseDTO.setTransactionPoid(1L);
    }

    @AfterEach
    void tearDown() {
        userContext.close();
    }

    @Test
    void searchManifestCorrector_Success() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        FilterRequestDto filterRequest = new FilterRequestDto(
                "OR",
                "N",
                List.of(new FilterDto("TRANSACTION_DATE", "2024-01-15"))
        );
        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.searchManifestCorrector(any(), any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(post("/v1/shipping-manifest-corrector/search")
                        .header("X-Document-Id", "100-143")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk());

        verify(service).searchManifestCorrector(
                eq("100-143"),
                eq(filterRequest),
                eq(startDate),
                eq(endDate),
                any()
        );
    }

    @Test
    void getManifestCorrector_Success() throws Exception {
        when(service.getManifestCorrectorById(eq(1L))).thenReturn(responseDTO);

        mockMvc.perform(get("/v1/shipping-manifest-corrector/1")
                        .header("X-Document-Id", "100-143"))
                .andExpect(status().isOk());
    }

    @Test
    @Disabled
    void createManifestCorrector_Success() throws Exception {
        when(service.createManifestCorrector(any(ManifestCorrectorCreateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/shipping-manifest-corrector/create")
                        .header("X-Document-Id", "100-143")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void updateManifestCorrector_Success() throws Exception {
        when(service.updateManifestCorrector(eq(1L), any(ManifestCorrectorUpdateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/v1/shipping-manifest-corrector/1")
                        .header("X-Document-Id", "100-143")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteManifestCorrector_Success() throws Exception {
        doNothing().when(service).deleteManifestCorrector(eq(1L), any());

        mockMvc.perform(delete("/v1/shipping-manifest-corrector/1")
                        .header("X-Document-Id", "100-143"))
                .andExpect(status().isOk());
    }

    @Test
    void autoPopulateFromBlBrowse_Success() throws Exception {
        ManifestCorrectorBlAutoPopulateDto autoPopulateDto = ManifestCorrectorBlAutoPopulateDto.builder()
                .blPoid(12345L)
                .consigneePoid(99L)
                .build();

        when(service.autoPopulateFromBlBrowse(eq("12345"), any())).thenReturn(autoPopulateDto);

        mockMvc.perform(post("/v1/shipping-manifest-corrector/auto-fill/bl-after-browse/12345")
                        .header("X-Document-Id", "100-143")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionPoid\":1}"))
                .andExpect(status().isOk());
    }
}
