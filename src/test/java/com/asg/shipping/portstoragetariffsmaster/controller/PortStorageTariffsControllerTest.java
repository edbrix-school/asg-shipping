package com.asg.shipping.portstoragetariffsmaster.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.portstoragetariffsmaster.dto.*;
import com.asg.shipping.portstoragetariffsmaster.service.PortStorageTariffsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortStorageTariffsControllerTest {

    @Mock
    private PortStorageTariffsService tariffService;

    @InjectMocks
    private PortStorageTariffsController controller;

    private PortStorageTariffDto testDto;
    private PortStorageTariffCreateDTO createDto;
    private PortStorageTariffUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        testDto = PortStorageTariffDto.builder()
                .transactionPoid(1L)
                .portPoid(100L)
                .description("Test Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        createDto = PortStorageTariffCreateDTO.builder()
                .portPoid(100L)
                .description("Test Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        updateDto = PortStorageTariffUpdateDTO.builder()
                .portPoid(100L)
                .description("Updated Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();
    }

    @Test
    void searchTariffs_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto(null, null, null);
            Map<String, Object> result = new HashMap<>();
            when(tariffService.searchTariffs(anyString(), any(), any())).thenReturn(result);

            ResponseEntity<?> response = controller.searchTariffs(filterRequest, 0, 20, "description,asc");

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).searchTariffs(anyString(), any(), any());
        }
    }

    @Test
    void searchTariffs_WithNullRequest() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            Map<String, Object> result = new HashMap<>();
            when(tariffService.searchTariffs(anyString(), any(), any())).thenReturn(result);

            ResponseEntity<?> response = controller.searchTariffs(null, 0, 20, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void getTariff_Success() {
        when(tariffService.getTariff(1L)).thenReturn(testDto);

        ResponseEntity<?> response = controller.getTariff(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(tariffService).getTariff(1L);
    }

    @Test
    void getTariff_NotFound() {
        when(tariffService.getTariff(1L))
                .thenThrow(new ResourceNotFoundException("Tariff", "transactionPoid", "1"));

        assertThrows(ResourceNotFoundException.class, () -> controller.getTariff(1L));
    }

    @Test
    void createTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(tariffService.createTariff(any(), anyLong(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.createTariff(createDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).createTariff(any(), anyLong(), anyLong(), anyLong());
        }
    }

    @Test
    void createTariff_InvalidPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(tariffService.createTariff(any(), anyLong(), anyLong(), anyLong()))
                    .thenThrow(new ValidationException("Period from date must be less than or equal to period to date"));

            assertThrows(ValidationException.class, () -> controller.createTariff(createDto));
        }
    }

    @Test
    void createTariff_OverlappingPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(tariffService.createTariff(any(), anyLong(), anyLong(), anyLong()))
                    .thenThrow(new ValidationException("Period overlaps with an existing tariff"));

            assertThrows(ValidationException.class, () -> controller.createTariff(createDto));
        }
    }

    @Test
    void updateTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(tariffService.updateTariff(eq(1L), any(), anyLong(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.updateTariff(1L, updateDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).updateTariff(eq(1L), any(), anyLong(), anyLong(), anyLong());
        }
    }

    @Test
    void updateTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            when(tariffService.updateTariff(eq(1L), any(), anyLong(), anyLong(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Tariff", "transactionPoid", "1"));

            assertThrows(ResourceNotFoundException.class, () -> controller.updateTariff(1L, updateDto));
        }
    }

    @Test
    void deleteTariff_Success() {
        doNothing().when(tariffService).deleteTariff(1L);

        ResponseEntity<?> response = controller.deleteTariff(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(tariffService).deleteTariff(1L);
    }

    @Test
    void deleteTariff_NotFound() {
        doThrow(new ResourceNotFoundException("Tariff", "transactionPoid", "1"))
                .when(tariffService).deleteTariff(1L);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteTariff(1L));
    }

    @Test
    void deleteTariff_AlreadyDeleted() {
        doNothing().when(tariffService).deleteTariff(1L);

        ResponseEntity<?> response = controller.deleteTariff(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(tariffService).deleteTariff(1L);
    }
}
