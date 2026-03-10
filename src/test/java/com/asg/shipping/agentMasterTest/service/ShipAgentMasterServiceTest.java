package com.asg.shipping.agentMasterTest.service;


import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterResponseDto;
import com.asg.shipping.agentMaster.entity.ShipAgentMasterEntity;
import com.asg.shipping.agentMaster.repository.ShipAgentMasterRepository;
import com.asg.shipping.agentMaster.service.ShipAgentMasterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShipAgentMasterServiceTest {

    @Mock
    private ShipAgentMasterRepository repository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private ShipAgentMasterServiceImpl service;

    private ShipAgentMasterRequestDto requestDto;
    private ShipAgentMasterEntity entity;
    private ShipAgentMasterResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = ShipAgentMasterRequestDto.builder()
                .agentName("Test Agent")
                .agentName2("Test Agent 2")
                .contactPerson("John Doe")
                .details("Test Details")
                .linePoid(1L)
                .portPoid(2L)
                .email(Collections.singletonList("test@example.com"))
                .contactNo("1234567890")
                .faxNo("0987654321")
                .remarks("Test Remarks")
                .seqNo(1)
                .active("Y")
                .build();

        entity = ShipAgentMasterEntity.builder()
                .agentPoid(1L)
                .groupPoid(100L)
                .agentName("Test Agent")
                .agentName2("Test Agent 2")
                .contactPerson("John Doe")
                .details("Test Details")
                .linePoid(1L)
                .portPoid(2L)
                .email("test@example.com")
                .contactNo("1234567890")
                .faxNo("0987654321")
                .remarks("Test Remarks")
                .seqNo(1)
                .active("Y")
                .deleted("N")
                .build();

        responseDto = ShipAgentMasterResponseDto.builder()
                .agentPoid(1L)
                .groupPoid(100L)
                .agentName("Test Agent")
                .agentName2("Test Agent 2")
                .contactPerson("John Doe")
                .details("Test Details")
                .linePoid(1L)
                .portPoid(2L)
                .email("test@example.com")
                .contactNo("1234567890")
                .faxNo("0987654321")
                .remarks("Test Remarks")
                .seqNo(1)
                .active("Y")
                .createdBy("testUser")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createAgentMaster_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            
            when(repository.save(any(ShipAgentMasterEntity.class))).thenReturn(entity);

            ShipAgentMasterResponseDto result = service.createAgentMaster(requestDto);

            assertNotNull(result);
            assertEquals("Test Agent", result.getAgentName());
            assertEquals("Test Details", result.getDetails());
            verify(repository).save(any(ShipAgentMasterEntity.class));
        }
    }

    @Test
    void updateAgentMaster_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));

            ShipAgentMasterResponseDto result = service.updateAgentMaster(1L, requestDto);

            assertNotNull(result);
            assertEquals("Test Agent", result.getAgentName());
            verify(repository).findById(1L);
        }
    }

    @Test
    void updateAgentMaster_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> service.updateAgentMaster(1L, requestDto));
    }

    @Test
    void findByIdAgentMaster_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ShipAgentMasterResponseDto result = service.findByIdAgentMaster(1L);

        assertNotNull(result);
        assertEquals(1L, result.getAgentPoid());
        assertEquals("Test Agent", result.getAgentName());
    }

    @Test
    void findByIdAgentMaster_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> service.findByIdAgentMaster(1L));
    }

    @Test
    void deleteAgentMaster_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            
            when(repository.findById(1L)).thenReturn(Optional.of(entity));

            service.deleteAgentMaster(1L);

            assertEquals("Y", entity.getDeleted());
            assertEquals("N", entity.getActive());
            verify(repository).findById(1L);
        }
    }

    @Test
    void deleteAgentMaster_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> service.deleteAgentMaster(1L));
    }

   @Test
    void listAgents_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("AGENT_NAME", "Test Agent")), 
                Map.of("AGENT_NAME", "Agent Name"), 
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(), 
                any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.listAgents("docId", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("docId"), anyList(), eq("AND"), 
                eq(pageable), eq("N"), eq("AGENT_NAME"), eq("AGENT_POID"));
    }

    @Test
    void getCurrentUser_WithUserId() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("123");
            
            String result = ShipAgentMasterServiceImpl.getCurrentUser();
            
            assertEquals("123", result);
        }
    }

    @Test
    void getCurrentUser_WithoutUserId() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(null);
            
            String result = ShipAgentMasterServiceImpl.getCurrentUser();
            
            assertEquals("SYSTEM", result);
        }
    }
}
