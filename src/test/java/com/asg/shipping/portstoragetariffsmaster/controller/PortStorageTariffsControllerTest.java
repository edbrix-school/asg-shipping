package com.asg.shipping.portstoragetariffsmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortStorageTariffsControllerTest {

    @Mock
    private PortStorageTariffsService tariffService;

    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private PortStorageTariffsController controller;

    private PortStorageTariffDto testDto;
    private PortStorageTariffCreateDTO createDto;
    private PortStorageTariffUpdateDTO updateDto;
    private DeleteReasonDto deleteReasonDto;

    private void stubSearch(Map<String, Object> result) {
        lenient().doReturn(result).when(tariffService).searchTariffs(
                anyString(), any(), any(), any(), any());
    }

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

        deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion reason");
    }

    @Test
    void searchTariffs_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto(null, null, null);
            Map<String, Object> result = new HashMap<>();
            stubSearch(result);

            ResponseEntity<?> response = controller.searchTariffs(filterRequest, 0, 20, "description,asc", null, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).searchTariffs(eq("100-060"), eq(filterRequest), isNull(), isNull(), any());
        }
    }

    @Test
    void searchTariffs_WithNullRequest() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            stubSearch(new HashMap<>());

            ResponseEntity<?> response = controller.searchTariffs(null, 0, 20, null, null, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }

    @Test
    void getTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");
            
            when(tariffService.getTariff(1L)).thenReturn(testDto);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC_ID"), eq("1"));

            ResponseEntity<?> response = controller.getTariff(1L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).getTariff(1L);
            verify(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), eq("DOC_ID"), eq("1"));
        }
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
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(tariffService).deleteTariff(1L, 1L, 1L, null);

            ResponseEntity<?> response = controller.deleteTariff(1L, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).deleteTariff(1L, 1L, 1L, null);
        }
    }

    @Test
    void deleteTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doThrow(new ResourceNotFoundException("Tariff", "transactionPoid", "1"))
                    .when(tariffService).deleteTariff(1L, 1L, 1L, null);

            assertThrows(ResourceNotFoundException.class, () -> controller.deleteTariff(1L, null));
        }
    }

    @Test
    void deleteTariff_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(tariffService).deleteTariff(1L, 1L, 1L, null);

            ResponseEntity<?> response = controller.deleteTariff(1L, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).deleteTariff(1L, 1L, 1L, null);
        }
    }

    // Additional tests for 100% coverage
    @Test
    void searchTariffs_WithDifferentSortFormats() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            stubSearch(new HashMap<>());

            // Test with single field sort
            ResponseEntity<?> response1 = controller.searchTariffs(null, 0, 20, "description", null, null);
            assertNotNull(response1);
            assertEquals(200, response1.getStatusCode().value());

            // Test with desc sort
            ResponseEntity<?> response2 = controller.searchTariffs(null, 0, 20, "description,desc", null, null);
            assertNotNull(response2);
            assertEquals(200, response2.getStatusCode().value());

            // Test with empty sort
            ResponseEntity<?> response3 = controller.searchTariffs(null, 0, 20, "", null, null);
            assertNotNull(response3);
            assertEquals(200, response3.getStatusCode().value());
        }
    }

    @Test
    void deleteTariff_WithDeleteReason() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            doNothing().when(tariffService).deleteTariff(1L, 1L, 1L, deleteReasonDto);

            ResponseEntity<?> response = controller.deleteTariff(1L, deleteReasonDto);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).deleteTariff(1L, 1L, 1L, deleteReasonDto);
        }
    }

    @Test
    void searchTariffs_WithComplexFilterRequest() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            FilterRequestDto complexFilter = new FilterRequestDto("OR", "Y", null);
            stubSearch(new HashMap<>());

            ResponseEntity<?> response = controller.searchTariffs(complexFilter, 1, 50, "periodFrom,desc", null, null);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).searchTariffs(eq("100-060"), eq(complexFilter), isNull(), isNull(), any());
        }
    }

    @Test
    void searchTariffs_RejectsPartialPeriodRange() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            ResponseEntity<?> response = controller.searchTariffs(
                    null, 0, 20, null, LocalDate.of(2026, 1, 1), null);

            assertEquals(400, response.getStatusCode().value());
            verify(tariffService, never()).searchTariffs(anyString(), any(), any(), any(), any());
        }
    }

    @Test
    void searchTariffs_PassesPeriodRangeToService() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            LocalDate periodFrom = LocalDate.of(2026, 1, 1);
            LocalDate periodTo = LocalDate.of(2026, 1, 30);
            doReturn(new HashMap<>()).when(tariffService).searchTariffs(
                    anyString(), any(), eq(periodFrom), eq(periodTo), any());

            ResponseEntity<?> response = controller.searchTariffs(
                    null, 0, 40, "TRANSACTION_POID,DESC", periodFrom, periodTo);

            assertEquals(200, response.getStatusCode().value());
            verify(tariffService).searchTariffs(eq("100-060"), isNull(), eq(periodFrom), eq(periodTo), any());
        }
    }

    @Test
    void createPageable_EdgeCases() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            stubSearch(new HashMap<>());

            // Test with invalid sort format (more than 2 parts)
            ResponseEntity<?> response = controller.searchTariffs(null, 0, 20, "field,asc,extra", null, null);
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
        }
    }
}
