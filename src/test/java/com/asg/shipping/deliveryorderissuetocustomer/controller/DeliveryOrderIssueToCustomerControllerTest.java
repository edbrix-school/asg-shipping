package com.asg.shipping.deliveryorderissuetocustomer.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.service.DeliveryOrderIssueToCustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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
public class DeliveryOrderIssueToCustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeliveryOrderIssueToCustomerService service;

    @InjectMocks
    private DeliveryOrderIssueToCustomerController controller;

    private ObjectMapper objectMapper;
    private DeliveryOrderIssueToCustomerDto responseDto;
    private IssueDeliveryOrderRequestDto issueRequestDto;
    private UpdateDeliveryOrderRequestDto updateRequestDto;
    private FilterRequestDto filterRequestDto;

    @BeforeEach
    void setUp() {
        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(pageableResolver)
                .build();
        objectMapper = new ObjectMapper();
        
        responseDto = DeliveryOrderIssueToCustomerDto.builder()
                .transactionPoid(1L)
                .companyPoid(100L)
                .docRef("DO-001")
                .transactionDate(LocalDate.now())
                .jobNo("JOB001")
                .arrivalDate(LocalDate.now())
                .blNumber("BL001")
                .line("TEST LINE")
                .consignee("Test Consignee")
                .notify("Test Notify")
                .c20(5)
                .c40(3)
                .holdDo("N")
                .doReleasedIdPerson("REL001")
                .doReleasedToPerson("John Doe")
                .doReleasedAddrsPerson("123 Test Street")
                .blReleaseTypeOffice("OFFICE")
                .originalBlReleaseCr("Y")
                .doPriority("HIGH")
                .doPriorityDet(new LovGetListDto(1L, "HIGH", "High Priority", null, null, null, null))
                .doIssueAuth("AUTH001")
                .doIssueAuthPoid(200L)
                .doIssueAuthPoidDet(new LovGetListDto(2L, "AUTH001", "Authorized Person", null, null, null, null))
                .doCntToConsignee("Y")
                .doCntToNotify("Y")
                .doCntToOthers("N")
                .doCntToOthersMails("test@example.com")
                .doEmails("do@example.com")
                .deliverySentTo("C")
                .deliverySentToDet(new LovGetListDto(3L, "C", "Consignee", null, null, null, null))
                .principalDoNumber("PDO001")
                .principalDoRequired("Y")
                .build();

        issueRequestDto = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doReleasedToPerson("John Doe")
                .doReleasedAddressPerson("123 Test Street")
                .originalBlReleaseCr("Y")
                .doPriority("HIGH")
                .emailsConsg("consignee@example.com")
                .emailNotify("notify@example.com")
                .emailsOthers("others@example.com")
                .emailsAdditional("additional@example.com")
                .emailsDo("do@example.com")
                .deliverySentTo("C")
                .principalDoNumber("PDO001")
                .doCntToConsignee("Y")
                .doCntToNotify("Y")
                .doCntToOthers("N")
                .doCntToOthersMails("test@example.com")
                .doCntToRegsMails("regs@example.com")
                .build();

        updateRequestDto = UpdateDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL002")
                .doReleasedToPerson("Jane Doe")
                .doReleasedAddressPerson("456 Updated Street")
                .originalBlReleaseCr("N")
                .doPriority("MEDIUM")
                .deliverySentTo("N")
                .principalDoNumber("PDO002")
                .doCntToConsignee("N")
                .doCntToNotify("N")
                .doCntToOthers("Y")
                .doCntToOthersMails("updated@example.com")
                .build();

        filterRequestDto = new FilterRequestDto("OR", "false", List.of());
    }

    @Test
    void getDeliveryOrderIssueToCustomer_Success() throws Exception {
        Long id = 1L;
        when(service.getDeliveryOrderIssueToCustomer(id)).thenReturn(responseDto);

        mockMvc.perform(get("/v1/delivery-order-issue-to-customer/{id}", id))
                .andExpect(status().isOk());

        verify(service).getDeliveryOrderIssueToCustomer(id);
    }

    @Test
    void issueDeliveryOrder_Success() throws Exception {
        Long id = 1L;
        doNothing().when(service).issueDeliveryOrder(eq(id), any(IssueDeliveryOrderRequestDto.class));

        mockMvc.perform(post("/v1/delivery-order-issue-to-customer/{id}/issue", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequestDto)))
                .andExpect(status().isOk());

        verify(service).issueDeliveryOrder(eq(id), any(IssueDeliveryOrderRequestDto.class));
    }

    @Test
    void updateDeliveryOrder_Success() throws Exception {
        Long id = 1L;
        when(service.updateDeliveryOrder(eq(id), any(UpdateDeliveryOrderRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/v1/delivery-order-issue-to-customer/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isOk());

        verify(service).updateDeliveryOrder(eq(id), any(UpdateDeliveryOrderRequestDto.class));
    }
}