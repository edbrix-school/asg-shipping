package com.asg.shipping.demurrage;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.demurragedetentionpayabletransfer.controller.DemurrageDetentionPayableTransferController;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferCreateDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferDto;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferUpdateDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.ProcessDataRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.UpdateFreeDaysRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.service.DemurrageDetentionPayableTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DemurrageDetentionPayableTransferControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DemurrageDetentionPayableTransferService service;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private DemurrageDetentionPayableTransferController controller;

    private DemurrageDetentionPayableTransferCreateDTO createDTO;
    private DemurrageDetentionPayableTransferUpdateDTO updateDTO;
    private DemurrageDetentionPayableTransferDto responseDTO;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        createDTO = new DemurrageDetentionPayableTransferCreateDTO();
        createDTO.setLinePoid(1001L);
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlType("IMPORT");

        updateDTO = new DemurrageDetentionPayableTransferUpdateDTO();
        updateDTO.setLinePoid(1001L);
        updateDTO.setBlType("IMPORT");

        responseDTO = new DemurrageDetentionPayableTransferDto();
        responseDTO.setTransactionPoid(1L);
    }

    @Test
    void testSearchRecords() throws Exception {
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of());
        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.searchDemurrageDetentionPayableTransfer(any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/search")
                        .param("docId", "100-151")
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "SEARCH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(service.getDemurrageDetentionPayableTransfer(eq(1L)))
                    .thenReturn(responseDTO);

            mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/1")
                            .header("X-Group-Poid", 100L)
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "VIEW"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Disabled
    void testCreate() throws Exception {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(service.createDemurrageDetentionPayableTransfer(
                    any(DemurrageDetentionPayableTransferCreateDTO.class), eq(1L), eq(100L)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/v1/demurrage-detention-payable-transfer")
                            .header("X-Group-Poid", 100L)
                            .header("X-User-Id", "admin")
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "CREATE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void testUpdate() throws Exception {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(service.updateDemurrageDetentionPayableTransfer(eq(1L),
                    any(DemurrageDetentionPayableTransferUpdateDTO.class), eq(1L), eq(100L)))
                    .thenReturn(responseDTO);

            mockMvc.perform(put("/v1/demurrage-detention-payable-transfer/1")
                            .header("X-Group-Poid", 100L)
                            .header("X-User-Id", "admin")
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "UPDATE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testDelete() throws Exception {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(service).deleteDemurrageDetentionPayableTransfer(eq(1L), eq(1L), eq(100L), any());

            mockMvc.perform(delete("/v1/demurrage-detention-payable-transfer/1")
                            .header("X-Group-Poid", 100L)
                            .header("X-User-Id", "admin")
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "DELETE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Demurrage/Detention Payable Transfer deleted successfully"));
        }
    }

    @Test
    void testProcessData() throws Exception {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);
        processRequest.setBlType("IMPORT");

        lenient().when(service.processData(eq(1L), any(ProcessDataRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/process-data")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "PROCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Disabled
    void testUpdateFreeDays() throws Exception {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        updateRequest.setContainerUpdates(List.of());
        
        doNothing().when(service).updateFreeDays(eq(1L), any(UpdateFreeDaysRequestDTO.class));

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/1/update-free-days")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "UPDATE_FREE_DAYS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Free days updated successfully"));
    }
}