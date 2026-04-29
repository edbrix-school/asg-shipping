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
                .setControllerAdvice(new com.asg.shipping.exceptions.GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        createDTO = new DemurrageDetentionPayableTransferCreateDTO();
        createDTO.setLinePoid(1001L);
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlType("IMPORT");
        createDTO.setIncomeGlPoid(67890L);
        createDTO.setPayableGlPoid(12345L);

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

        when(service.searchDemurrageDetentionPayableTransfer(any(), any(), any(), any(), any()))
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
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("100-151");

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
    void testGetById_NotFound() throws Exception {
        when(service.getDemurrageDetentionPayableTransfer(eq(999L)))
                .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException(
                        "Demurrage/Detention Payable Transfer", "transactionPoid", "999"));

        mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/999")
                        .header("X-Document-Id", "100-151"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
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
                    .andExpect(status().isOk());
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

        Map<String, Object> result = new HashMap<>();
        result.put("data", new HashMap<>());

        when(service.processDataBeforeCreate(any(ProcessDataRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/process-data")
                        .header("X-Group-Poid", "100")
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "PROCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // This test is removed because the endpoint doesn't exist in the controller

    @Test
    void testLoadBillwiseData() throws Exception {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest =
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        loadRequest.setBlType("IMPORT");
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer container = 
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.SelectedContainer();
        container.setMainfestTransactionPoid(1001L);
        container.setContainerNo("CONT001");
        container.setBlNumber("BL001");
        loadRequest.setSelectedContainers(List.of(container));

        Map<String, Object> result = new HashMap<>();
        result.put("billDetails", new Object[]{});
        result.put("totalCount", 0);

        when(service.loadBillwiseDataBeforeCreate(any(com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/load-billwise")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "LOAD_BILLWISE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdatePrincipalDays() throws Exception {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate containerUpdate = new UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate();
        containerUpdate.setContainerNo("CONT001");
        containerUpdate.setMainfestTransactionPoid(1001L);
        containerUpdate.setDetRowId(1L);
        containerUpdate.setExtraFreeDaysPrnpls(java.math.BigDecimal.valueOf(5));
        updateRequest.setContainerUpdates(List.of(containerUpdate));

        doNothing().when(service).updatePrincipalDays(any(UpdateFreeDaysRequestDTO.class));

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/update-principal-days")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "UPDATE_PRINCIPAL_DAYS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updatation Completed, Requery for check..."));
    }

    @Test
    void testGetAutoPopulatedGlAccounts() throws Exception {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            Map<String, Object> result = new HashMap<>();
            result.put("payableGlPoid", 12345L);
            result.put("incomeGlPoid", 67890L);

            when(service.getAutoPopulatedGlAccounts(eq(1001L), eq("IMPORT"), eq(100L)))
                    .thenReturn(result);

            mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/auto-populate-gl-accounts")
                            .param("linePoid", "1001")
                            .param("blType", "IMPORT")
                            .header("X-Group-Poid", 100L)
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "GET_GL_ACCOUNTS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("GL accounts auto-populated successfully"));
        }
    }

    @Test
    void testGetGlAccountsDirect() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("payableGlPoid", "12345");

        when(service.getGlAccountsDirectFromSp(eq(1001L), eq("IMPORT")))
                .thenReturn(result);

        mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/get-gl-accounts-direct")
                        .param("linePoid", "1001")
                        .param("blType", "IMPORT")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "GET_GL_ACCOUNTS_DIRECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("GL accounts retrieved successfully"));
    }

    // Validation and Error Handling Tests

    @Test
    void testProcessData_ValidationError() throws Exception {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        // Missing required fields

        when(service.processDataBeforeCreate(any(ProcessDataRequestDTO.class)))
                .thenThrow(new com.asg.shipping.exceptions.ValidationException("Line POID and BL Type are required"));

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/process-data")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "PROCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePrincipalDays_ValidationError() throws Exception {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        updateRequest.setContainerUpdates(List.of());

        doThrow(new com.asg.shipping.exceptions.ValidationException("Container updates are required"))
                .when(service).updatePrincipalDays(any(UpdateFreeDaysRequestDTO.class));

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/update-principal-days")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "UPDATE_PRINCIPAL_DAYS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAutoPopulatedGlAccounts_MissingParams() throws Exception {
        mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/auto-populate-gl-accounts")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "GET_GL_ACCOUNTS"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetGlAccountsDirect_MissingParams() throws Exception {
        mockMvc.perform(get("/v1/demurrage-detention-payable-transfer/get-gl-accounts-direct")
                        .param("linePoid", "1001")
                        // Missing blType parameter
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "GET_GL_ACCOUNTS_DIRECT"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDelete_NotFound() throws Exception {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", "999"))
                    .when(service).deleteDemurrageDetentionPayableTransfer(eq(999L), eq(1L), eq(100L), any());

            mockMvc.perform(delete("/v1/demurrage-detention-payable-transfer/999")
                            .header("X-Group-Poid", 100L)
                            .header("X-User-Id", "admin")
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "DELETE"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void testUpdate_NotFound() throws Exception {
        try (var mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            DemurrageDetentionPayableTransferUpdateDTO updateDTO = new DemurrageDetentionPayableTransferUpdateDTO();
            updateDTO.setLinePoid(1001L);
            updateDTO.setBlType("IMPORT");

            when(service.updateDemurrageDetentionPayableTransfer(eq(999L), any(), eq(1L), eq(100L)))
                    .thenThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", "999"));

            mockMvc.perform(put("/v1/demurrage-detention-payable-transfer/999")
                            .header("X-Group-Poid", 100L)
                            .header("X-User-Id", "admin")
                            .header("X-Document-Id", "100-151")
                            .header("X-Action-Requested", "UPDATE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void testCreate_ValidationError() throws Exception {
        DemurrageDetentionPayableTransferCreateDTO createDTO = new DemurrageDetentionPayableTransferCreateDTO();
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlType("INVALID");
        // Missing required fields: linePoid, incomeGlPoid, payableGlPoid

        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoadBillwiseData_EmptyContainers() throws Exception {
        com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO loadRequest =
                new com.asg.shipping.demurragedetentionpayabletransfer.dto.LoadBillwiseRequestDTO();
        loadRequest.setBlType("IMPORT");
        loadRequest.setSelectedContainers(List.of());

        // This will fail validation due to @NotEmpty annotation
        mockMvc.perform(post("/v1/demurrage-detention-payable-transfer/load-billwise")
                        .header("X-Group-Poid", 100L)
                        .header("X-Document-Id", "100-151")
                        .header("X-Action-Requested", "LOAD_BILLWISE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loadRequest)))
                .andExpect(status().isBadRequest());
    }
}
