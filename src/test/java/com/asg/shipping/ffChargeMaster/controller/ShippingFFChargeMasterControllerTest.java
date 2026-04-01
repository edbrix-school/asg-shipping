package com.asg.shipping.ffChargeMaster.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shippingFFChargeMaster.controller.ShippingFFChargeMasterController;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingFFChargeMaster.service.ShippingFFChargeMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShippingFFChargeMasterControllerTest {

    @Mock
    private ShippingFFChargeMasterService chargeMasterService;

    @InjectMocks
    private ShippingFFChargeMasterController controller;

    private ChargeCreateDTO createDto;
    private ChargeUpdateDTO updateDto;
    private ChargeDto responseDto;

    @BeforeEach
    void setUp() {
        createDto = ChargeCreateDTO.builder()
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .chargeRevenueType("REVENUE")
                .chargeType("FIXED")
                .divisionCode("DIV001")
                .active("Y")
                .seqno(1)
                .build();

        updateDto = ChargeUpdateDTO.builder()
                .chargeName("Updated Charge")
                .chargeRevenueType("REVENUE")
                .chargeType("FIXED")
                .divisionCode("DIV001")
                .active("Y")
                .seqno(1)
                .build();

        responseDto = ChargeDto.builder()
                .chargePoid(1L)
                .groupPoid(100L)
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .active("Y")
                .createdBy("testUser")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    @Disabled
    void getCharge_Success() {
        when(chargeMasterService.getCharge(1L)).thenReturn(responseDto);

        ResponseEntity<?> response = controller.getCharge(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(chargeMasterService).getCharge(1L);
    }

    @Test
    void createCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(123L);

            when(chargeMasterService.createCharge(createDto, 100L, 123L)).thenReturn(responseDto);

            ResponseEntity<?> response = controller.createCharge(createDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(chargeMasterService).createCharge(createDto, 100L, 123L);
        }
    }

    @Test
    void updateCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(123L);

            when(chargeMasterService.updateCharge(1L, updateDto, 100L, 123L)).thenReturn(responseDto);

            ResponseEntity<?> response = controller.updateCharge(1L, updateDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(chargeMasterService).updateCharge(1L, updateDto, 100L, 123L);
        }
    }

    @Test
    void deleteCharge_Success() {
        doNothing().when(chargeMasterService).deleteCharge(1L, null);

        ResponseEntity<?> response = controller.deleteCharge(1L, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(chargeMasterService).deleteCharge(1L, null);
    }

    @Test
    void searchCharges_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
            Pageable pageable = PageRequest.of(0, 10);
            Map<String, Object> searchResult = Map.of("content", "test", "totalElements", 1);

            when(chargeMasterService.searchCharges("DOC001", filterRequest, pageable))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.geSearchCharges(pageable, filterRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(chargeMasterService).searchCharges("DOC001", filterRequest, pageable);
        }
    }

    @Test
    void searchCharges_WithoutFilters() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            Pageable pageable = PageRequest.of(0, 10);
            Map<String, Object> searchResult = Map.of("content", "test");

            when(chargeMasterService.searchCharges("DOC001", null, pageable))
                    .thenReturn(searchResult);

            ResponseEntity<?> response = controller.geSearchCharges(pageable, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(chargeMasterService).searchCharges("DOC001", null, pageable);
        }
    }

    @Test
    void searchCharges_Exception() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            Pageable pageable = PageRequest.of(0, 10);
            when(chargeMasterService.searchCharges(anyString(), any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database error"));

            ResponseEntity<?> response = controller.geSearchCharges(pageable, null);

            assertNotNull(response);
            assertEquals(500, response.getStatusCode().value());
        }
    }

    @Test
    void getCharge_NotFound() {
        when(chargeMasterService.getCharge(999L))
                .thenThrow(new ResourceNotFoundException("Charge", "chargePoid", "999"));

        assertThrows(ResourceNotFoundException.class, () -> controller.getCharge(999L));
        verify(chargeMasterService).getCharge(999L);
    }

    @Test
    void createCharge_ValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(123L);

            when(chargeMasterService.createCharge(createDto, 100L, 123L))
                    .thenThrow(new ValidationException("Charge Code already exists"));

            assertThrows(ValidationException.class, () -> controller.createCharge(createDto));
            verify(chargeMasterService).createCharge(createDto, 100L, 123L);
        }
    }

    @Test
    void updateCharge_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(123L);

            when(chargeMasterService.updateCharge(999L, updateDto, 100L, 123L))
                    .thenThrow(new ResourceNotFoundException("Charge", "chargePoid", "999"));

            assertThrows(ResourceNotFoundException.class, () -> controller.updateCharge(999L, updateDto));
            verify(chargeMasterService).updateCharge(999L, updateDto, 100L, 123L);
        }
    }

    @Test
    void updateCharge_ValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(123L);

            when(chargeMasterService.updateCharge(1L, updateDto, 100L, 123L))
                    .thenThrow(new ValidationException("Charge Name already exists"));

            assertThrows(ValidationException.class, () -> controller.updateCharge(1L, updateDto));
            verify(chargeMasterService).updateCharge(1L, updateDto, 100L, 123L);
        }
    }

    @Test
    void deleteCharge_NotFound() {
        doThrow(new ResourceNotFoundException("Charge", "chargePoid", "999"))
                .when(chargeMasterService).deleteCharge(999L, null);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteCharge(999L, null));
        verify(chargeMasterService).deleteCharge(999L, null);
    }
}
