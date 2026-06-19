package com.asg.shipping.linetariffs.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.CopyTariffRequestDTO;
import com.asg.shipping.linetariffs.dto.LineTariffCreateDTO;
import com.asg.shipping.linetariffs.dto.LineTariffDto;
import com.asg.shipping.linetariffs.dto.LineTariffUpdateDTO;
import com.asg.shipping.linetariffs.service.LineTariffsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineTariffsControllerTest {

    private static final String DOC_ID = "100-050";

    @Mock
    private LineTariffsService lineTariffsService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private LineTariffsController controller;

    private LineTariffDto testDto;

    @BeforeEach
    void setUp() {
        testDto = LineTariffDto.builder()
                .transactionPoid(1L)
                .linePoid(10L)
                .description("Test")
                .periodFrom(LocalDate.of(2026, 1, 1))
                .periodTo(LocalDate.of(2026, 12, 31))
                .build();
    }

    @Test
    void searchLineTariffs_Success() {
        Map<String, Object> result = new HashMap<>();
        when(lineTariffsService.searchLineTariffs(anyString(), any(), any()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchLineTariffs(
                new FilterRequestDto(null, null, null), 0, 20, "description,asc");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).searchLineTariffs(eq(DOC_ID), any(), any());
    }

    @Test
    void searchLineTariffs_WhenServiceThrows_ReturnsInternalServerError() {
        when(lineTariffsService.searchLineTariffs(anyString(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.searchLineTariffs(null, 0, 20, null);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void searchLineTariffs_sortsDesc_whenDirectionIsDesc() {
        Map<String, Object> result = new HashMap<>();
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);

        when(lineTariffsService.searchLineTariffs(eq(DOC_ID), any(), pageableCaptor.capture()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchLineTariffs(
                new FilterRequestDto(null, null, null), 0, 20, " description , desc ");

        assertEquals(200, response.getStatusCode().value());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("description");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void searchLineTariffs_sortFieldOnly_defaultsToAsc() {
        Map<String, Object> result = new HashMap<>();
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);

        when(lineTariffsService.searchLineTariffs(eq(DOC_ID), any(), pageableCaptor.capture()))
                .thenReturn(result);

        controller.searchLineTariffs(new FilterRequestDto(null, null, null), 0, 20, "description");

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("description");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void searchLineTariffs_sortEmptyString_resultsInUnsorted() {
        Map<String, Object> result = new HashMap<>();
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);

        when(lineTariffsService.searchLineTariffs(eq(DOC_ID), any(), pageableCaptor.capture()))
                .thenReturn(result);

        controller.searchLineTariffs(new FilterRequestDto(null, null, null), 0, 20, "");

        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void searchLineTariffs_sortWithExtraParts_keepsUnsorted() {
        Map<String, Object> result = new HashMap<>();
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);

        when(lineTariffsService.searchLineTariffs(eq(DOC_ID), any(), pageableCaptor.capture()))
                .thenReturn(result);

        controller.searchLineTariffs(new FilterRequestDto(null, null, null), 0, 20, "description,desc,extra");

        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void getLineTariff_Success() {
        try (MockedStatic<com.asg.common.lib.security.util.UserContext> mockedUserContext =
                     mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getDocumentId).thenReturn("100-050");

            when(lineTariffsService.getLineTariff(1L)).thenReturn(testDto);

            ResponseEntity<?> response = controller.getLineTariff(1L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineTariffsService).getLineTariff(1L);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("100-050"), eq("1"));
        }
    }

    @Test
    void createLineTariff_Success() {
        try (MockedStatic<com.asg.common.lib.security.util.UserContext> mockedUserContext =
                     mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(2L);

            LineTariffCreateDTO createDTO = LineTariffCreateDTO.builder()
                    .linePoid(10L)
                    .periodFrom(LocalDate.of(2026, 1, 1))
                    .periodTo(LocalDate.of(2026, 12, 31))
                    .build();

            when(lineTariffsService.createLineTariff(any(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.createLineTariff(createDTO);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineTariffsService).createLineTariff(any(), eq(1L), eq(2L));
        }
    }

    @Test
    void updateLineTariff_Success() {
        try (MockedStatic<com.asg.common.lib.security.util.UserContext> mockedUserContext =
                     mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(2L);

            LineTariffUpdateDTO updateDTO = LineTariffUpdateDTO.builder().description("Updated").build();
            when(lineTariffsService.updateLineTariff(eq(1L), any(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.updateLineTariff(1L, updateDTO);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineTariffsService).updateLineTariff(eq(1L), any(), eq(1L), eq(2L));
        }
    }

    @Test
    void deleteLineTariff_Success() {
        doNothing().when(lineTariffsService).deleteLineTariff(eq(1L), any());

        ResponseEntity<?> response = controller.deleteLineTariff(1L, new DeleteReasonDto());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).deleteLineTariff(eq(1L), any());
    }

    @Test
    void deleteLineTariff_NotFound_Returns404() {
        doThrow(new com.asg.common.lib.exception.ResourceNotFoundException("Line Tariff", "transactionPoid", "1"))
                .when(lineTariffsService).deleteLineTariff(eq(1L), any());

        ResponseEntity<?> response = controller.deleteLineTariff(1L, null);

        assertNotNull(response);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteLineTariff_WhenServiceThrows_Returns500() {
        doThrow(new RuntimeException("boom")).when(lineTariffsService).deleteLineTariff(eq(1L), any());

        ResponseEntity<?> response = controller.deleteLineTariff(1L, null);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void copyLineTariff_Success() {
        try (MockedStatic<com.asg.common.lib.security.util.UserContext> mockedUserContext =
                     mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getUserPoid).thenReturn(2L);

            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 1, 1))
                    .periodTo(LocalDate.of(2027, 12, 31))
                    .description("Copied")
                    .build();

            when(lineTariffsService.copyLineTariff(eq(1L), any(), anyLong(), anyLong())).thenReturn(testDto);

            ResponseEntity<?> response = controller.copyLineTariff(1L, request);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineTariffsService).copyLineTariff(eq(1L), any(), eq(1L), eq(2L));
        }
    }

    @Test
    void copySlabsToPayable_DMG_Success() {
        doNothing().when(lineTariffsService).copySlabsToPayable(1L, "DMG");

        ResponseEntity<?> response = controller.copySlabsToPayable(1L, "DMG");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).copySlabsToPayable(1L, "DMG");
    }

    @Test
    void copySlabsToPayable_DTN_Success() {
        doNothing().when(lineTariffsService).copySlabsToPayable(1L, "DTN");

        ResponseEntity<?> response = controller.copySlabsToPayable(1L, "DTN");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).copySlabsToPayable(1L, "DTN");
    }

    @Test
    void loadContainerTypes_IMP_Success() {
        doNothing().when(lineTariffsService).loadContainerTypes(1L, "IMP");

        ResponseEntity<?> response = controller.loadContainerTypes(1L, "IMP");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).loadContainerTypes(1L, "IMP");
    }

    @Test
    void loadContainerTypes_EXP_Success() {
        doNothing().when(lineTariffsService).loadContainerTypes(1L, "EXP");

        ResponseEntity<?> response = controller.loadContainerTypes(1L, "EXP");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineTariffsService).loadContainerTypes(1L, "EXP");
    }

    @Test
    void loadContainerTypes_ReturnsEmptyList_WhenAllUsed() {
        doNothing().when(lineTariffsService).loadContainerTypes(1L, "IMP");

        ResponseEntity<?> response = controller.loadContainerTypes(1L, "IMP");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void print_Success() throws Exception {
        byte[] pdf = new byte[]{1, 2, 3};
        when(lineTariffsService.print(1L)).thenReturn(pdf);

        ResponseEntity<?> response = controller.print(1L);

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(pdf, (byte[]) response.getBody());
        verify(lineTariffsService).print(1L);
    }

    @Test
    void print_NotFound() throws Exception {
        when(lineTariffsService.print(1L))
                .thenThrow(new ResourceNotFoundException("Line Tariff", "transactionPoid", "1"));

        ResponseEntity<?> response = controller.print(1L);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void print_Error() throws Exception {
        when(lineTariffsService.print(1L)).thenThrow(new RuntimeException("jasper failed"));

        ResponseEntity<?> response = controller.print(1L);

        assertEquals(500, response.getStatusCode().value());
    }
}
