package com.asg.shipping.linemasterthirdparty.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.service.LineMasterThirdPartyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineMasterThirdPartyControllerTest {

    @Mock
    private LineMasterThirdPartyService lineService;

    @InjectMocks
    private LineMasterThirdPartyController controller;

    private LineMasterThirdPartyDto testDto;
    private LineMasterThirdPartyCreateDTO createDto;
    private LineMasterThirdPartyUpdateDTO updateDto;
    private LineMasterThirdPartyListResponse listResponse;

    @BeforeEach
    void setUp() {
        testDto = LineMasterThirdPartyDto.builder()
                .linePoid(1L)
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .active("Y")
                .build();

        createDto = LineMasterThirdPartyCreateDTO.builder()
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .active("Y")
                .build();

        updateDto = LineMasterThirdPartyUpdateDTO.builder()
                .lineName("Updated Third Party Line")
                .active("Y")
                .build();

        listResponse = LineMasterThirdPartyListResponse.builder()
                .linePoid(1L)
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .build();
    }

    @Test
    void searchThirdPartyLines_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto(null, null, null);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            when(lineService.searchThirdPartyLines(any(com.asg.common.lib.dto.FilterRequestDto.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchThirdPartyLines(filterRequest, 0, 20, "lineName,asc");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineService).searchThirdPartyLines(any(com.asg.common.lib.dto.FilterRequestDto.class), any(org.springframework.data.domain.Pageable.class));
        }
    }

    @Test
    void searchThirdPartyLines_WithNullRequest() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            when(lineService.searchThirdPartyLines(any(), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchThirdPartyLines(null, 0, 20, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void getThirdPartyLine_Success() {
        when(lineService.getThirdPartyLine(1L)).thenReturn(testDto);

        ResponseEntity<?> response = controller.getThirdPartyLine(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).getThirdPartyLine(1L);
    }

    @Test
    void getThirdPartyLine_NotFound() {
        when(lineService.getThirdPartyLine(1L))
                .thenThrow(new ResourceNotFoundException("Third Party Line", "linePoid", "1"));

        assertThrows(ResourceNotFoundException.class, () -> controller.getThirdPartyLine(1L));
    }

    @Test
    void createThirdPartyLine_Success() {
        when(lineService.createThirdPartyLine(any())).thenReturn(testDto);

        ResponseEntity<?> response = controller.createThirdPartyLine(createDto);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).createThirdPartyLine(any());
    }

    @Test
    void createThirdPartyLine_DuplicateCode() {
        when(lineService.createThirdPartyLine(any()))
                .thenThrow(new ValidationException("Line code already exists"));

        assertThrows(ValidationException.class, () -> controller.createThirdPartyLine(createDto));
    }

    @Test
    void updateThirdPartyLine_Success() {
        when(lineService.updateThirdPartyLine(eq(1L), any())).thenReturn(testDto);

        ResponseEntity<?> response = controller.updateThirdPartyLine(1L, updateDto);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).updateThirdPartyLine(eq(1L), any());
    }

    @Test
    void updateThirdPartyLine_NotFound() {
        when(lineService.updateThirdPartyLine(eq(1L), any()))
                .thenThrow(new ResourceNotFoundException("Third Party Line", "linePoid", "1"));

        assertThrows(ResourceNotFoundException.class, () -> controller.updateThirdPartyLine(1L, updateDto));
    }

    @Test
    void toggleActive_Success() {
        doNothing().when(lineService).toggleActive(1L);

        ResponseEntity<?> response = controller.toggleActive(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).toggleActive(1L);
    }

    @Test
    void toggleActive_NotFound() {
        doThrow(new ResourceNotFoundException("Third Party Line", "linePoid", "1"))
                .when(lineService).toggleActive(1L);

        assertThrows(ResourceNotFoundException.class, () -> controller.toggleActive(1L));
    }

    @Test
    void deleteThirdPartyLine_Success() {
        doNothing().when(lineService).deleteThirdPartyLine(1L);

        ResponseEntity<?> response = controller.deleteThirdPartyLine(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).deleteThirdPartyLine(1L);
    }

    @Test
    void deleteThirdPartyLine_NotFound() {
        doThrow(new ResourceNotFoundException("Third Party Line", "linePoid", "1"))
                .when(lineService).deleteThirdPartyLine(1L);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteThirdPartyLine(1L));
    }
}
