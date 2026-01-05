package com.asg.shipping.importManifestUpdateTest.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.controller.ImportManifestBlController;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlRequestDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlUpdateDTO;
import com.asg.shipping.importManifestUpdate.service.ImportManifestBlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ImportManifestBlControllerTest {

    @Mock
    private ImportManifestBlService service;

    @InjectMocks
    private ImportManifestBlController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getImportManifestList_Success() throws Exception {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("data", "test data");
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("OR", "N", null);
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("123");
            when(service.listOfImportManifest(eq("123"), any(FilterRequestDto.class), any(Pageable.class)))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<?> response = controller.getImportManifestList(pageable, filters);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertEquals("Import Manifest list fetched successfully", responseBody.get("message"));
            verify(service).listOfImportManifest(eq("123"), any(FilterRequestDto.class), any(Pageable.class));
        }
    }

    @Test
    void getImportManifestList_Exception() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        FilterRequestDto filters = new FilterRequestDto("OR", "N", null);
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("123");
            when(service.listOfImportManifest(any(), any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            ResponseEntity<?> response = controller.getImportManifestList(pageable, filters);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertTrue(responseBody.get("message").toString().contains("Error fetching Import Manifest List"));
        }
    }


    @Test
    void updateImportManifestBl_Success() {
        // Given
        Long id = 1L;
        ImportManifestBlUpdateDTO updateDto = ImportManifestBlUpdateDTO.builder()
                .blNumber("TEST123")
                .agentReference("AGENT001")
                .transactionDate(LocalDate.now())
                .build();
        
        ImportManifestBlRequestDto expectedResponse = ImportManifestBlRequestDto.builder()
                .transactionPoid(id)
                .blNumber("TEST123")
                .build();

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(200L);
            
            when(service.updateImportManifestBl(eq(id), eq(updateDto), eq(100L), eq(200L)))
                    .thenReturn(expectedResponse);

            // When
            ResponseEntity<?> response = controller.updateImportManifestBl(id, updateDto);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertEquals("Import Manifest BL updated successfully", responseBody.get("message"));
            verify(service).updateImportManifestBl(eq(id), eq(updateDto), eq(100L), eq(200L));
        }
    }

    @Test
    void getImportManifestBl_Success() {
        // Given
        Long id = 1L;
        ImportManifestBlRequestDto expectedResponse = ImportManifestBlRequestDto.builder()
                .transactionPoid(id)
                .blNumber("TEST123")
                .agentReference("AGENT001")
                .build();

        when(service.getImportManifestBl(id)).thenReturn(expectedResponse);

        // When
        ResponseEntity<?> response = controller.getImportManifestBl(id);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Import Manifest BL retrieved successfully", responseBody.get("message"));
        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        assertEquals(expectedResponse, result.get("data"));
        verify(service).getImportManifestBl(id);
    }

    @Test
    void deleteImportManifestBl_Success() {
        // Given
        Long id = 1L;
        doNothing().when(service).deleteImportManifestBl(id);

        // When
        ResponseEntity<?> response = controller.deleteImportManifestBl(id);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Import Manifest BL deleted successfully", responseBody.get("message"));
        verify(service).deleteImportManifestBl(id);
    }

    @Test
    void updateImportManifestBl_WithNullDto() {
        // Given
        Long id = 1L;
        ImportManifestBlUpdateDTO updateDto = null;
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(200L);
            
            when(service.updateImportManifestBl(eq(id), eq(updateDto), eq(100L), eq(200L)))
                    .thenThrow(new IllegalArgumentException("Update DTO cannot be null"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                controller.updateImportManifestBl(id, updateDto);
            });
        }
    }

    @Test
    void getImportManifestBl_NotFound() {
        // Given
        Long id = 999L;
        when(service.getImportManifestBl(id))
                .thenThrow(new RuntimeException("Import Manifest BL not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            controller.getImportManifestBl(id);
        });
        verify(service).getImportManifestBl(id);
    }

    @Test
    void deleteImportManifestBl_NotFound() {
        // Given
        Long id = 999L;
        doThrow(new RuntimeException("Import Manifest BL not found"))
                .when(service).deleteImportManifestBl(id);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            controller.deleteImportManifestBl(id);
        });
        verify(service).deleteImportManifestBl(id);
    }



}
