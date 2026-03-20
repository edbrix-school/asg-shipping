package com.asg.shipping.agentMasterTest.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.agentmaster.controller.ShipAgentMasterController;
import com.asg.shipping.agentmaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentmaster.dto.ShipAgentMasterResponseDto;
import com.asg.shipping.agentmaster.service.ShipAgentMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShipAgentMasterControllerTest {

    @Mock
    private ShipAgentMasterService service;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private ShipAgentMasterController controller;

    private ShipAgentMasterRequestDto requestDto;
    private ShipAgentMasterResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = ShipAgentMasterRequestDto.builder()
                .agentName("Test Agent")
                .details("Test Details")
                .linePoid(1L)
                .portPoid(2L)
                .email(Collections.singletonList("test@example.com"))
                .contactNo("1234567890")
                .active("Y")
                .build();

        responseDto = ShipAgentMasterResponseDto.builder()
                .agentPoid(1L)
                .agentName("Test Agent")
                .details("Test Details")
                .linePoid(1L)
                .portPoid(2L)
                .email("test@example.com")
                .contactNo("1234567890")
                .active("Y")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createAgent_Success() {
        when(service.createAgentMaster(any(ShipAgentMasterRequestDto.class)))
                .thenReturn(responseDto);

        ResponseEntity<?> response = controller.createAgent(requestDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).createAgentMaster(any(ShipAgentMasterRequestDto.class));
    }

    @Test
    void updateAgent_Success() {
        when(service.updateAgentMaster(eq(1L), any(ShipAgentMasterRequestDto.class)))
                .thenReturn(responseDto);

        ResponseEntity<?> response = controller.updateAgent(1L, requestDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).updateAgentMaster(eq(1L), any(ShipAgentMasterRequestDto.class));
    }

    @Test
    void getAgentById_Success() {
        when(service.findByIdAgentMaster(1L)).thenReturn(responseDto);

        ResponseEntity<?> response = controller.getAgentById(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).findByIdAgentMaster(1L);
    }

    @Test
    void deleteAgent_Success() {
        doNothing().when(service).deleteAgentMaster(1L);

        ResponseEntity<?> response = controller.deleteAgent(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).deleteAgentMaster(1L);
    }

    @Test
    void searchAgents_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", null);
            Map<String, Object> searchResult = Map.of("content", "test", "totalElements", 1);
            
            when(service.listAgents(eq("100-063"), any(FilterRequestDto.class), any(Pageable.class)))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.searchAgents(filterRequest, 0, 20, "agentName,asc");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(service).listAgents(eq("100-063"), any(FilterRequestDto.class), any(Pageable.class));
        }
    }

    @Test
    void searchAgents_WithoutSort() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            Map<String, Object> searchResult = Map.of("content", "test");
            
            when(service.listAgents(eq("100-063"), any(), any(Pageable.class)))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.searchAgents(null, 0, 10, null);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(service).listAgents(eq("100-063"), any(), 
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "AGENT_NAME"))));
        }
    }

    @Test
    void searchAgents_WithDescSort() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            Map<String, Object> searchResult = Map.of("content", "test");
            
            when(service.listAgents(eq("100-063"), any(), any(Pageable.class)))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.searchAgents(null, 0, 10, "createdDate,desc");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(service).listAgents(eq("100-063"), any(), 
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "CREATED_DATE"))));
        }
    }

    @Test
    void searchAgents_WithDifferentSortFields() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            Map<String, Object> searchResult = Map.of("content", "test");
            when(service.listAgents(eq("100-063"), any(), any(Pageable.class)))
                    .thenReturn(searchResult);

            controller.searchAgents(null, 0, 10, "agentName2,asc");
            verify(service).listAgents(eq("100-063"), any(), 
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "AGENT_NAME2"))));

            controller.searchAgents(null, 0, 10, "contactPerson,asc");
            verify(service, times(2)).listAgents(eq("100-063"), any(), any(Pageable.class));

            controller.searchAgents(null, 0, 10, "email,asc");
            verify(service, times(3)).listAgents(eq("100-063"), any(), any(Pageable.class));
        }
    }

    @Test
    void searchAgents_WithInvalidSortFormat() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            Map<String, Object> searchResult = Map.of("content", "test");
            when(service.listAgents(eq("100-063"), any(), any(Pageable.class)))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.searchAgents(null, 0, 10, "invalidFormat");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(service).listAgents(eq("100-063"), any(), 
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "AGENT_NAME"))));
        }
    }

    @Test
    void searchAgents_WithUnknownSortField() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-063");
            
            Map<String, Object> searchResult = Map.of("content", "test");
            when(service.listAgents(eq("100-063"), any(), any(Pageable.class)))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.searchAgents(null, 0, 10, "unknownField,asc");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(service).listAgents(eq("100-063"), any(), 
                    eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "AGENT_NAME"))));
        }
    }

}
