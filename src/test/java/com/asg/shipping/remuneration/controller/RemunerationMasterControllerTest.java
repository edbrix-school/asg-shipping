package com.asg.shipping.remuneration.controller;

import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.service.RemunerationMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemunerationMasterControllerTest {

    @Mock
    private RemunerationMasterService service;

    @InjectMocks
    private RemunerationMasterController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListRemunerations() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", "test");
        when(service.listRemunerations(isNull(), isNull(), any())).thenReturn(response);

        ResponseEntity<?> result = controller.listRemunerations(
                PageRequest.of(0, 10), null);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(service, times(1)).listRemunerations(isNull(), isNull(), any());
    }

    @Test
    void testCreateRemuneration() throws Exception {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunDescription("Test Description");

        ResponseEntity<?> result = controller.createRemuneration(request);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service, times(1)).createRemuneration(any());
    }

    @Test
    void testUpdateRemuneration() {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunDescription("Updated Description");

        ResponseEntity<?> result = controller.update(1L, request);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service, times(1)).updateRemuneration(eq(1L), any());
    }

    @Test
    void testGetById() {
        ResponseEntity<?> result = controller.getById(1L);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service, times(1)).getRemunerationById(eq(1L));
    }

    @Test
    void testDelete() {
        doNothing().when(service).softDeleteRemuneration(anyLong());

        ResponseEntity<?> result = controller.delete(1L);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service, times(1)).softDeleteRemuneration(eq(1L));
    }
}
