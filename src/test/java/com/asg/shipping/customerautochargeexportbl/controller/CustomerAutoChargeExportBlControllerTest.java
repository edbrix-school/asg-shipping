package com.asg.shipping.customerautochargeexportbl.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import com.asg.shipping.customerautochargeexportbl.service.CustomerAutoChargeExportBlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAutoChargeExportBlControllerTest {

    @Mock
    private CustomerAutoChargeExportBlService service;

    @InjectMocks
    private CustomerAutoChargeExportBlController controller;

    @Mock
    private LoggingService loggingService;

    private CustomerAutoChargeExportBLCreateDTO createDTO;
    private CustomerAutoChargeExportBLUpdateDTO updateDTO;
    private CustomerAutoChargeExportBLDto responseDto;

    @BeforeEach
    void setUp() {
        createDTO = new CustomerAutoChargeExportBLCreateDTO();
        createDTO.setCustomerPoid(100L);
        createDTO.setDescription("Test Charge");
        createDTO.setPeriodFrom(LocalDate.of(2024, 1, 1));
        createDTO.setPeriodTo(LocalDate.of(2024, 12, 31));
        createDTO.setDocRef("DOC001");

        updateDTO = new CustomerAutoChargeExportBLUpdateDTO();
        updateDTO.setDescription("Updated Charge");
        updateDTO.setPeriodFrom(LocalDate.of(2024, 1, 1));
        updateDTO.setPeriodTo(LocalDate.of(2024, 12, 31));

        responseDto = CustomerAutoChargeExportBLDto.builder()
                .transactionPoid(1L)
                .customerPoid(100L)
                .description("Test Charge")
                .docRef("DOC001")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateSuccess() {
        when(service.createCustomerAutoChargeExportBL(any(CustomerAutoChargeExportBLCreateDTO.class)))
                .thenReturn(responseDto);

        ResponseEntity<?> result = controller.create(createDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Customer Auto Charge Export BL created successfully", body.get("message"));
        verify(service).createCustomerAutoChargeExportBL(any(CustomerAutoChargeExportBLCreateDTO.class));
    }

    @Test
    void testCreateValidationError() {
        when(service.createCustomerAutoChargeExportBL(any(CustomerAutoChargeExportBLCreateDTO.class)))
                .thenThrow(new ValidationException("Customer POID is required"));

        ResponseEntity<?> result = controller.create(createDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Customer POID is required", body.get("message"));
    }

    @Test
    void testCreateInternalError() {
        when(service.createCustomerAutoChargeExportBL(any(CustomerAutoChargeExportBLCreateDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> result = controller.create(createDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("Failed to create"));
    }

    @Test
    void testUpdateSuccess() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenReturn(responseDto);

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Customer Auto Charge Export BL updated successfully", body.get("message"));
        verify(service).updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class));
    }

    @Test
    void testUpdateValidationError() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenThrow(new ValidationException("Period from date must be less than period to date"));

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testUpdateNotFound() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", "1"));

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testUpdateIllegalArgumentError() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testUpdateIllegalStateError() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenThrow(new IllegalStateException("Record is deleted"));

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testUpdateInternalError() {
        when(service.updateCustomerAutoChargeExportBL(eq(1L), any(CustomerAutoChargeExportBLUpdateDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> result = controller.update(1L, updateDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("Failed to update"));
    }

    @Test
    void testGetByIdSuccess() {
        when(service.getCustomerAutoChargeExportBL(1L)).thenReturn(responseDto);

        ResponseEntity<?> result = controller.getById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Customer Auto Charge Export BL retrieved successfully", body.get("message"));
        verify(service).getCustomerAutoChargeExportBL(1L);
    }


    @Test
    void testDeleteSuccess() {
        doNothing().when(service).deleteCustomerAutoChargeExportBL(1L,new DeleteReasonDto());

        ResponseEntity<?> result = controller.delete(1L,new DeleteReasonDto());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Customer Auto Charge Export BL deleted successfully", body.get("message"));
        verify(service).deleteCustomerAutoChargeExportBL(1L,new DeleteReasonDto());
    }

    @Test
    void testDeleteNotFound() {
        doThrow(new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", "1"))
                .when(service).deleteCustomerAutoChargeExportBL(1L,new DeleteReasonDto());

        ResponseEntity<?> result = controller.delete(1L,new DeleteReasonDto());

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testDeleteInternalError() {
        doThrow(new RuntimeException("Database error"))
                .when(service).deleteCustomerAutoChargeExportBL(1L,new DeleteReasonDto());

        ResponseEntity<?> result = controller.delete(1L,new DeleteReasonDto());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("Failed to delete"));
    }


    @Test
    void testListInternalError() {
        when(service.list(any(FilterRequestDto.class), any(Pageable.class),any(LocalDate.class),any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database error"));

        FilterRequestDto filters = new FilterRequestDto(null, null, Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);

        ResponseEntity<?> result = controller.list(pageable, filters,LocalDate.now(), LocalDate.now());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.get("message").toString().contains("Failed to retrieve"));
    }
}
