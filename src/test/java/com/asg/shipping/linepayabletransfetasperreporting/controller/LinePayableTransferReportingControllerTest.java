package com.asg.shipping.linepayabletransfetasperreporting.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import com.asg.shipping.linepayabletransfetasperreporting.service.LinePayableTransferReportingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LinePayableTransferReportingControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LinePayableTransferReportingService service;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private LinePayableTransferReportingController controller;

    private LinePayableTransferReportingDto testDto;
    private LinePayableTransferReportingCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper.findAndRegisterModules();
        
        testDto = LinePayableTransferReportingDto.builder()
                .transactionPoid(1L)
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .docRef("LPT-2024-001")
                .build();

        createDTO = LinePayableTransferReportingCreateDTO.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .docRef("LPT-2024-001")
                .build();
    }

    @Test
    void searchLinePayableTransfer_Success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("content", Collections.singletonList(testDto));
        result.put("totalElements", 1);

        when(service.searchLinePayableTransfer(anyString(), any(), any())).thenReturn(result);

        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());

        mockMvc.perform(post("/v1/line-payable-transfer-reporting/search")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getLinePayableTransfer_Success() throws Exception {
        when(service.getLinePayableTransferById(1L)).thenReturn(testDto);

        mockMvc.perform(get("/v1/line-payable-transfer-reporting/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createLinePayableTransfer_Success() throws Exception {
        when(service.createLinePayableTransfer(any())).thenReturn(testDto);

        mockMvc.perform(post("/v1/line-payable-transfer-reporting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateLinePayableTransfer_Success() throws Exception {
        LinePayableTransferReportingUpdateDTO updateDTO = LinePayableTransferReportingUpdateDTO.builder()
                .linePoid(1123L)
                .blType("EXPORT")
                .build();

        when(service.updateLinePayableTransfer(eq(1L), any())).thenReturn(testDto);

        mockMvc.perform(put("/v1/line-payable-transfer-reporting/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteLinePayableTransfer_Success() throws Exception {
        doNothing().when(service).deleteLinePayableTransfer(eq(1L), any());
        
        mockMvc.perform(delete("/v1/line-payable-transfer-reporting/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loadDataBeforeCreate_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .blType("IMPORT")
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 31))
                .build();

        when(service.loadDataBeforeCreate(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/v1/line-payable-transfer-reporting/load-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void processWeeklyBeforeCreate_Success() throws Exception {
        LoadDataByDateRangeRequest request = LoadDataByDateRangeRequest.builder()
                .linePoid(1123L)
                .reportStartDate(LocalDate.of(2024, 1, 1))
                .reportEndDate(LocalDate.of(2024, 1, 7))
                .build();

        when(service.processWeeklyBeforeCreate(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/v1/line-payable-transfer-reporting/process-weekly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
