package com.asg.shipping.tradelanemaster.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipTradeLaneControllerTest {

    @Mock
    private ShipTradeLaneService service;

    @InjectMocks
    private ShipTradeLaneController controller;

    private ShipTradelaneRequest request;
    private ShipTradelaneResponse response;

    @BeforeEach
    void setUp() {
        request = new ShipTradelaneRequest();
        request.setTradeLaneCode("TL001");
        request.setTradeLaneName("Trade Lane 1");
        request.setTradeLaneName2("TL1");
        request.setRegionPoid(1L);
        request.setActive(true);
        request.setSeqNo(10);

        response = ShipTradelaneResponse.builder()
                .tradeLanePoid(1L)
                .tradeLaneCode("TL001")
                .tradeLaneName("Trade Lane 1")
                .tradeLaneName2("TL1")
                .regionPoid(1L)
                .active(true)
                .seqNo(10)
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateSuccess() {
        when(service.create(any(ShipTradelaneRequest.class))).thenReturn(response);

        ResponseEntity<?> result = controller.create(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Ship Trade Lane created successfully", body.get("message"));
        verify(service).create(any(ShipTradelaneRequest.class));
    }

    @Test
    void testCreateValidationError() {
        when(service.create(any(ShipTradelaneRequest.class)))
                .thenThrow(new ValidationException("Trade Lane Code is required"));

        ResponseEntity<?> result = controller.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Trade Lane Code is required", body.get("message"));
    }

    @Test
    void testCreateDuplicateError() {
        when(service.create(any(ShipTradelaneRequest.class)))
                .thenThrow(new DuplicateKeyException("Trade Lane Code already exists"));

        ResponseEntity<?> result = controller.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Trade Lane Code already exists", body.get("message"));
    }

    @Test
    void testCreateInternalError() {
        when(service.create(any(ShipTradelaneRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> result = controller.create(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to create Ship Trade Lane: Database error", body.get("message"));
    }

    @Test
    void testUpdateSuccess() {
        when(service.update(eq(1L), any(ShipTradelaneRequest.class))).thenReturn(response);

        ResponseEntity<?> result = controller.update(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Ship Trade Lane updated successfully", body.get("message"));
        verify(service).update(eq(1L), any(ShipTradelaneRequest.class));
    }

    @Test
    void testUpdateValidationError() {
        when(service.update(eq(1L), any(ShipTradelaneRequest.class)))
                .thenThrow(new ValidationException("Trade Lane Code is required"));

        ResponseEntity<?> result = controller.update(1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Trade Lane Code is required", body.get("message"));
    }

    @Test
    void testUpdateNotFound() {
        when(service.update(eq(1L), any(ShipTradelaneRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ship Trade Lane", "tradeLanePoid", 1L));

        ResponseEntity<?> result = controller.update(1L, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testUpdateInternalError() {
        when(service.update(eq(1L), any(ShipTradelaneRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> result = controller.update(1L, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to update Ship Trade Lane: Database error", body.get("message"));
    }

    @Test
    void testGetByIdSuccess() {
        when(service.getById(1L)).thenReturn(response);

        ResponseEntity<?> result = controller.getById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Ship Trade Lane retrieved successfully", body.get("message"));
        verify(service).getById(1L);
    }

    @Test
    void testGetByIdNotFound() {
        when(service.getById(1L))
                .thenThrow(new ResourceNotFoundException("Ship Trade Lane", "tradeLanePoid", 1L));

        ResponseEntity<?> result = controller.getById(1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testGetByIdInternalError() {
        when(service.getById(1L))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> result = controller.getById(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to retrieve Ship Trade Lane: Database error", body.get("message"));
    }

    @Test
    void testDeleteSuccess() {
        doNothing().when(service).delete(1L);

        ResponseEntity<?> result = controller.delete(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("success"));
        assertEquals("Ship Trade Lane deleted successfully", body.get("message"));
        verify(service).delete(1L);
    }

    @Test
    void testDeleteNotFound() {
        doThrow(new ResourceNotFoundException("Ship Trade Lane", "tradeLanePoid", 1L))
                .when(service).delete(1L);

        ResponseEntity<?> result = controller.delete(1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
    }

    @Test

    void testDeleteInternalError() {
        doThrow(new RuntimeException("Database error"))
                .when(service).delete(1L);

        ResponseEntity<?> result = controller.delete(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to delete Ship Trade Lane: Database error", body.get("message"));
    }

    @Test
    void testListSuccess() {
        Map<String, Object> listResponse = Map.of(
                "content", Collections.emptyList(),
                "totalElements", 0L,
                "totalPages", 0,
                "size", 10,
                "number", 0
        );

        when(service.list(
                any(FilterRequestDto.class),
                any(Pageable.class)
        )).thenReturn(listResponse);

        FilterRequestDto filters = new FilterRequestDto(
                null,
                null,
                Collections.emptyList()
        );

        Pageable pageable = PageRequest.of(0, 10);
        String documentId = "100-012";

        ResponseEntity<?> result = controller.list(pageable, filters);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(
                "Ship Trade Lanes list retrieved successfully",
                body.get("message")
        );

        verify(service).list(
                any(FilterRequestDto.class),
                eq(pageable)
        );
    }



    @Test
    void testListInternalError() {
        when(service.list(
                any(FilterRequestDto.class),
                any(Pageable.class)
        )).thenThrow(new RuntimeException("Database error"));

        FilterRequestDto filters = new FilterRequestDto(
                null,
                null,
                Collections.emptyList()
        );

        Pageable pageable = PageRequest.of(0, 10);
        String documentId = "100-012";

        ResponseEntity<?> result = controller.list(pageable, filters);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals(
                "Failed to retrieve Ship Trade Lanes: Database error",
                body.get("message")
        );

        verify(service).list(
                any(FilterRequestDto.class),
                eq(pageable)
        );
    }


}