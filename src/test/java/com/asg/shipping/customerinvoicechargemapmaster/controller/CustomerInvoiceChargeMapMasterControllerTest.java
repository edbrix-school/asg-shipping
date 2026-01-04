package com.asg.shipping.customerinvoicechargemapmaster.controller;

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
        when(service.getByCustomer(1L, 100L)).thenReturn(mockResponse);

        mockMvc.perform(get("/v1/customer-invoice-charge-map-master/1")
                .header("X-Group-Poid", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer invoice charge mapping fetched successfully"))
                .andExpect(jsonPath("$.result.data.customerPoid").value(1))
                .andExpect(jsonPath("$.result.data.customerName").value("Test Customer"));

        verify(service).getByCustomer(1L, 100L);
    }

    @Test
    void getByCustomer_InvalidCustomerPoid_Zero() throws Exception {
        mockMvc.perform(get("/v1/customer-invoice-charge-map-master/0")
                .header("X-Group-Poid", "100"))
                .andExpect(status().isOk());

        verify(service).getByCustomer(0L, 100L);
    }

    @Test
    void getByCustomer_InvalidCustomerPoid_Negative() throws Exception {
        mockMvc.perform(get("/v1/customer-invoice-charge-map-master/-1")
                .header("X-Group-Poid", "100"))
                .andExpect(status().isOk());

        verify(service).getByCustomer(-1L, 100L);
    }

    @Test
    void getByCustomer_MissingGroupPoidHeader() throws Exception {
        mockMvc.perform(get("/v1/customer-invoice-charge-map-master/1"))
                .andExpect(status().isBadRequest());

        verify(service, never()).getByCustomer(anyLong(), anyLong());
    }

    @Test
    void saveOrUpdate_Success() throws Exception {
        doNothing().when(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer invoice charge mapping saved successfully"));

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_InvalidRequest_NullCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(null);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk());

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_InvalidRequest_ZeroCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(0L);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk());

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_InvalidRequest_NegativeCustomerPoid() throws Exception {
        mockRequest.setCustomerPoid(-1L);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk());

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_InvalidRequest_EmptyDetails() throws Exception {
        mockRequest.setDetails(Collections.emptyList());

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk());

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_InvalidRequest_NullDetails() throws Exception {
        mockRequest.setDetails(null);

        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isOk());

        verify(service).saveOrUpdate(any(CustomerInvoiceChargeMapMasterRequest.class), eq(100L), eq("user123"));
    }

    @Test
    void saveOrUpdate_MissingGroupPoidHeader() throws Exception {
        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), any(), anyString());
    }

    @Test
    void saveOrUpdate_MissingUserIdHeader() throws Exception {
        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong(), any());
    }

    @Test
    void saveOrUpdate_InvalidJsonFormat() throws Exception {
        mockMvc.perform(post("/v1/customer-invoice-charge-map-master")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).saveOrUpdate(any(), anyLong(), anyString());
    }

    @Test
    void deleteDetail_Success() throws Exception {
        doNothing().when(service).deleteDetail(1L, 10L, 100L, "user123");

        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/10")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer invoice charge detail deleted successfully"));

        verify(service).deleteDetail(1L, 10L, 100L, "user123");
    }

    @Test
    void deleteDetail_InvalidCustomerPoid_Zero() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/0/details/10")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(service).deleteDetail(0L, 10L, 100L, "user123");
    }

    @Test
    void deleteDetail_InvalidCustomerPoid_Negative() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/-1/details/10")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(service).deleteDetail(-1L, 10L, 100L, "user123");
    }

    @Test
    void deleteDetail_InvalidDetRowId_Zero() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/0")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(service).deleteDetail(1L, 0L, 100L, "user123");
    }

    @Test
    void deleteDetail_InvalidDetRowId_Negative() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/-1")
                .header("X-Group-Poid", "100")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(service).deleteDetail(1L, -1L, 100L, "user123");
    }

    @Test
    void deleteDetail_MissingGroupPoidHeader() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/10")
                .header("X-User-Id", "user123"))
                .andExpect(status().isBadRequest());

        verify(service, never()).deleteDetail(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void deleteDetail_MissingUserIdHeader() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/10")
                .header("X-Group-Poid", "100"))
                .andExpect(status().isBadRequest());

        verify(service, never()).deleteDetail(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void deleteDetail_MissingBothHeaders() throws Exception {
        mockMvc.perform(delete("/v1/customer-invoice-charge-map-master/1/details/10"))
                .andExpect(status().isBadRequest());

        verify(service, never()).deleteDetail(anyLong(), anyLong(), any(), any());
    }
}