package com.asg.shipping.vesseltypemaster.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;
import com.asg.shipping.vesseltypemaster.service.VesselTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselTypeMasterControllerTest {

    @Mock
    private VesselTypeService vesselTypeService;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private VesselTypeMasterController controller;

    private VesselTypeDto testDto;
    private VesselTypeCreateDTO createDto;
    private VesselTypeUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        testDto = VesselTypeDto.builder()
                .vesselTypePoid(1L)
                .vesselTypeCode("VT001")
                .vesselTypeName("Container Ship")
                .active("Y")
                .build();

        createDto = VesselTypeCreateDTO.builder()
                .vesselTypeCode("VT001")
                .vesselTypeName("Container Ship")
                .active("Y")
                .build();

        updateDto = VesselTypeUpdateDTO.builder()
                .vesselTypeName("Updated Ship")
                .active("Y")
                .build();
    }

    @Test
    void searchVesselTypes_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto(null, null, null);
            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchVesselTypes(filterRequest, 0, 20, "vesselTypeName,asc");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(vesselTypeService).searchVesselTypes(eq("100-003"), any(), any(Pageable.class));
        }
    }

    @Test
    void searchVesselTypes_WithNullRequest() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchVesselTypes(null, 0, 20, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void getVesselType_Success() {
        when(vesselTypeService.getVesselType(1L)).thenReturn(testDto);

        ResponseEntity<?> response = controller.getVesselType(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(vesselTypeService).getVesselType(1L);
    }

    @Test
    void getVesselType_NotFound() {
        when(vesselTypeService.getVesselType(1L))
                .thenThrow(new ResourceNotFoundException("Vessel Type", "vesselTypePoid", "1"));

        assertThrows(ResourceNotFoundException.class, () -> controller.getVesselType(1L));
    }

    @Test
    void createVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(vesselTypeService.createVesselType(any(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.createVesselType(createDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(vesselTypeService).createVesselType(any(), anyLong(), anyLong());
        }
    }

    @Test
    void createVesselType_DuplicateCode() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(vesselTypeService.createVesselType(any(), anyLong(), anyLong()))
                    .thenThrow(new ResourceAlreadyExistsException("Vessel type code", "VT001"));

            assertThrows(ResourceAlreadyExistsException.class, () -> controller.createVesselType(createDto));
        }
    }

    @Test
    void updateVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(vesselTypeService.updateVesselType(eq(1L), any(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.updateVesselType(1L, updateDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(vesselTypeService).updateVesselType(eq(1L), any(), anyLong(), anyLong());
        }
    }

    @Test
    void updateVesselType_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(vesselTypeService.updateVesselType(eq(1L), any(), anyLong(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Vessel Type", "vesselTypePoid", "1"));

            assertThrows(ResourceNotFoundException.class, () -> controller.updateVesselType(1L, updateDto));
        }
    }

    @Test
    void toggleActive_Success() {
        doNothing().when(vesselTypeService).toggleActive(1L);

        ResponseEntity<?> response = controller.toggleActive(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(vesselTypeService).toggleActive(1L);
    }

    @Test
    void toggleActive_NotFound() {
        doThrow(new ResourceNotFoundException("Vessel Type", "vesselTypePoid", "1"))
                .when(vesselTypeService).toggleActive(1L);

        assertThrows(ResourceNotFoundException.class, () -> controller.toggleActive(1L));
    }

    @Test
    void deleteVesselType_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(vesselTypeService).deleteVesselType(1L, 1L, 1L, null);

            ResponseEntity<?> response = controller.deleteVesselType(1L, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(vesselTypeService).deleteVesselType(1L, 1L, 1L, null);
        }
    }

    @Test
    void deleteVesselType_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doThrow(new ResourceNotFoundException("Vessel Type", "vesselTypePoid", "1"))
                    .when(vesselTypeService).deleteVesselType(1L, 1L, 1L, null);

            assertThrows(ResourceNotFoundException.class, () -> controller.deleteVesselType(1L, null));
        }
    }

    @Test
    void deleteVesselType_WithDeleteReason() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            com.asg.common.lib.dto.DeleteReasonDto deleteReason = new com.asg.common.lib.dto.DeleteReasonDto();
            deleteReason.setDeleteReason("Test reason");
            
            doNothing().when(vesselTypeService).deleteVesselType(1L, 1L, 1L, deleteReason);

            ResponseEntity<?> response = controller.deleteVesselType(1L, deleteReason);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(vesselTypeService).deleteVesselType(1L, 1L, 1L, deleteReason);
        }
    }

    @Test
    void searchVesselTypes_WithDescSort() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchVesselTypes(null, 0, 10, "vesselTypeName,desc");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void searchVesselTypes_WithDifferentSortFields() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            controller.searchVesselTypes(null, 0, 10, "vesselTypeCode,asc");
            verify(vesselTypeService, times(1)).searchVesselTypes(anyString(), any(), any(Pageable.class));

            controller.searchVesselTypes(null, 0, 10, "vesselTypeName2,asc");
            verify(vesselTypeService, times(2)).searchVesselTypes(anyString(), any(), any(Pageable.class));

            controller.searchVesselTypes(null, 0, 10, "createdDate,asc");
            verify(vesselTypeService, times(3)).searchVesselTypes(anyString(), any(), any(Pageable.class));

            controller.searchVesselTypes(null, 0, 10, "lastmodifiedDate,asc");
            verify(vesselTypeService, times(4)).searchVesselTypes(anyString(), any(), any(Pageable.class));
        }
    }

    @Test
    void searchVesselTypes_WithInvalidSortFormat() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchVesselTypes(null, 0, 10, "invalidFormat");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void searchVesselTypes_WithUnknownSortField() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(vesselTypeService.searchVesselTypes(anyString(), any(), any(Pageable.class)))
                    .thenReturn(result);

            ResponseEntity<?> response = controller.searchVesselTypes(null, 0, 10, "unknownField,asc");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }
}

