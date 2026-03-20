package com.asg.shipping.collectionhandover.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;
import com.asg.shipping.collectionhandover.service.CollectionHandoverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CollectionHandoverControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CollectionHandoverService service;

    @InjectMocks
    private CollectionHandoverController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void searchCollectionHandovers_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        when(service.searchCollectionHandovers(anyString(), any(), any(), any(), any()))
                .thenReturn(result);

        FilterRequestDto body = new FilterRequestDto(null, null, null);

        mockMvc.perform(post("/v1/collection-handover-shipping/search")
                        .param("page", "0")
                        .param("size", "20")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Collection handovers retrieved successfully"));

        verify(service).searchCollectionHandovers(anyString(), any(), any(), any(), any());
    }

    @Test
    void searchCollectionHandovers_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/collection-handover-shipping/search")
                        .param("page", "0")
                        .param("size", "20")
                        .param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void searchCollectionHandovers_ServiceThrows_ReturnsInternalServerError() throws Exception {
        when(service.searchCollectionHandovers(anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/v1/collection-handover-shipping/search")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getCollectionHandover_Success() throws Exception {
        when(service.getCollectionHandover(1L))
                .thenReturn(CollectionHandoverDto.builder().transactionPoid(1L).build());

        mockMvc.perform(get("/v1/collection-handover-shipping/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Collection handover retrieved successfully"));

        verify(service).getCollectionHandover(1L);
    }

    @Test
    void createCollectionHandover_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext =
                     mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(2L);

            CollectionHandoverCreateDTO dto = CollectionHandoverCreateDTO.builder()
                    .transactionDate(LocalDate.of(2026, 1, 1))
                    .companyPoid(100L)
                    .cashAmount(BigDecimal.TEN)
                    .build();

            when(service.createCollectionHandover(any(), eq(1L), eq(2L)))
                    .thenReturn(CollectionHandoverDto.builder().transactionPoid(1L).build());

            mockMvc.perform(post("/v1/collection-handover-shipping")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Collection handover created successfully"));

            verify(service).createCollectionHandover(any(), eq(1L), eq(2L));
        }
    }

    @Test
    void updateCollectionHandover_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext =
                     mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(2L);

            CollectionHandoverUpdateDTO dto = CollectionHandoverUpdateDTO.builder()
                    .docRef("REF1")
                    .build();

            when(service.updateCollectionHandover(eq(1L), any(), eq(1L), eq(2L)))
                    .thenReturn(CollectionHandoverDto.builder().transactionPoid(1L).build());

            mockMvc.perform(put("/v1/collection-handover-shipping/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Collection handover updated successfully"));

            verify(service).updateCollectionHandover(eq(1L), any(), eq(1L), eq(2L));
        }
    }

    @Test
    void deleteCollectionHandover_Success() throws Exception {
        mockMvc.perform(delete("/v1/collection-handover-shipping/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Collection handover deleted successfully"));

        verify(service).deleteCollectionHandover(1L);
    }

    @Test
    void toggleVerifyStatus_Success() throws Exception {
        mockMvc.perform(put("/v1/collection-handover-shipping/1/verify")
                        .param("verifiedRcvd", "Y")
                        .param("mainOfcRemarks", "OK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verify status updated successfully"));

        verify(service).toggleVerifyStatus(1L, "Y", "OK");
    }

    @Test
    void print_Success() throws Exception {
        when(service.print(1L)).thenReturn("PDF".getBytes());

        mockMvc.perform(get("/v1/collection-handover-shipping/print/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("collection-handover-1.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(service).print(1L);
    }

    @Test
    void print_Error_ReturnsApiError() throws Exception {
        when(service.print(anyLong())).thenThrow(new com.asg.shipping.exceptions.CustomException("Failed", 500));

        mockMvc.perform(get("/v1/collection-handover-shipping/print/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}

