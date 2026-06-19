package com.asg.shipping.customerinvoicechargemapmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapDetailDto;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.service.CustomerInvoiceChargeMapMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerInvoiceChargeMapMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerInvoiceChargeMapMasterService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private CustomerInvoiceChargeMapMasterController controller;

    private ObjectMapper objectMapper;

    private CustomerInvoiceChargeMapMasterResponse mockResponse;
    private CustomerInvoiceChargeMapMasterRequest mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        mockResponse = new CustomerInvoiceChargeMapMasterResponse();
        mockResponse.setCustomerPoid(1L);
        mockResponse.setCustomerName("Test Customer");
        
        CustomerInvoiceChargeMapDetailDto detail = new CustomerInvoiceChargeMapDetailDto();
        detail.setDetRowId(1L);
        detail.setChargePoid(100L);
        detail.setLineChargeDescription("Test Charge");
        detail.setValidUntil(LocalDate.now().plusDays(30));
        
        mockResponse.setDetails(Arrays.asList(detail));

        mockRequest = new CustomerInvoiceChargeMapMasterRequest();
        mockRequest.setCustomerPoid(1L);
        mockRequest.setDetails(Arrays.asList(detail));
    }

    @Test
    void getByCustomer_Success() throws Exception {

        when(service.getByCustomer(eq(1L), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/v1/customer-invoice-charge-map-master/1")
                        .header("X-Group-Poid", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Customer invoice charge mapping fetched successfully"));
         ;

        verify(service).getByCustomer(eq(1L), any());
    }
    @Test
    void saveOrUpdate_withCustomerChange_success() throws Exception {
        mockRequest.setOldCustomerPoid(5933L);
        mockRequest.setCustomerPoid(5928L);

        CustomerInvoiceChargeMapMasterResponse changedResponse = new CustomerInvoiceChargeMapMasterResponse();
        changedResponse.setCustomerPoid(5928L);
        changedResponse.setDetails(mockResponse.getDetails());

        when(service.saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), any()))
                .thenReturn(changedResponse);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                        .header("X-Group-Poid", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.data.customerPoid").value(5928));

        verify(service).saveOrUpdate(
                argThat(r -> r.getOldCustomerPoid().equals(5933L)
                        && r.getCustomerPoid().equals(5928L)),
                isNull()
        );
    }

    @Test
    void saveOrUpdate_Success() throws Exception {

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                        .header("X-Group-Poid", "100")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Customer invoice charge mapping saved successfully"));

        verify(service).saveOrUpdate(
                any(CustomerInvoiceChargeMapMasterRequest.class),
                isNull()
        );
    }

    @Test
    void saveOrUpdate_InvalidRequest_NullCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(null);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void saveOrUpdate_InvalidRequest_ZeroCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(0L);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void saveOrUpdate_InvalidRequest_NegativeCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(-1L);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void saveOrUpdate_InvalidRequest_EmptyDetails() throws Exception {
        mockRequest.setDetails(Collections.emptyList());

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void saveOrUpdate_InvalidRequest_NullDetails() throws Exception {
        mockRequest.setDetails(null);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void saveOrUpdate_InvalidJsonFormat() throws Exception {
        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong());
    }

    @Test
    void deleteDetail_Success() throws Exception {

        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1")
                        .header("X-Group-Poid", "100")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteReasonDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Customer invoice charge details deleted successfully"));

        verify(service).deleteDetail(eq(1L), any(DeleteReasonDto.class));
    }


}