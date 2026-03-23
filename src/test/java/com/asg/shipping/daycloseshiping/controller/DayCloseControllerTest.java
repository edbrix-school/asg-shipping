package com.asg.shipping.daycloseshiping.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.daycloseshiping.dto.DayCloseDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseHdrDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjectionImpl;
import com.asg.shipping.daycloseshiping.service.DayCloseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DayCloseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private DayCloseService dayCloseService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private DayCloseController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockedUserContext = mockStatic(UserContext.class);
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1001L);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2001L);
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(9001L);
        mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        PageableHandlerMethodArgumentResolver pageableResolver =
                new PageableHandlerMethodArgumentResolver();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(pageableResolver)
                .build();
    }

    @AfterEach
    void tearDown() {
        mockedUserContext.close();
    }

    @Test
    void getDayClose_Success() throws Exception {
        DayCloseDto response = new DayCloseDto();

        when(dayCloseService.getDayClose(100L, 1001L, 2001L)).thenReturn(response);

        mockMvc.perform(get("/v1/day-close-shipping/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day close fetched successfully"));

        verify(dayCloseService).getDayClose(100L, 1001L, 2001L);
        verify(loggingService).createLogSummaryEntry(
                LogDetailsEnum.VIEWED, "DOC123", "100");
    }

    @Test
    void getNewDayClose_Success() throws Exception {
        DayCloseSummaryProjectionImpl projection =
                DayCloseSummaryProjectionImpl.builder()
                        .transactionDate(LocalDate.now())
                        .cashAmount(BigDecimal.valueOf(1000))
                        .chequeAmount(BigDecimal.valueOf(500))
                        .totalAmount(BigDecimal.valueOf(1500))
                        .chequeCount(2L)
                        .build();

        when(dayCloseService.getNewDayCloseData(1001L, 2001L, "2025-07-06"))
                .thenReturn(projection);

        mockMvc.perform(
                        get("/v1/day-close-shipping/new")
                                .param("transactionDate", "2025-07-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("New day Close data fetched successfully"));

        verify(dayCloseService)
                .getNewDayCloseData(1001L, 2001L, "2025-07-06");
    }

    @Test
    void getDenominations_Success() throws Exception {
        List<Map<String, Object>> response =
                List.of(Map.of("DENOMINATION", 10, "COUNT", 5));

        when(dayCloseService.getDenominations("BHD"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/v1/day-close-shipping/denominations")
                                .param("currencyCode", "BHD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Denominations fetched successfully"));

        verify(dayCloseService).getDenominations("BHD");
    }

    @Test
    void createDayClose_Success() throws Exception {
        DayCloseHdrDto header = DayCloseHdrDto.builder()
                .groupPoid(1001L)
                .companyPoid(2001L)
                .transactionDate(LocalDate.now())
                .build();

        DayCloseDto request = DayCloseDto.builder()
                .header(header)
                .build();

        DayCloseDto response = new DayCloseDto();

        when(dayCloseService.createDayClose(any(), eq(1001L), eq(2001L), eq(9001L)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/v1/day-close-shipping")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day Close created successfully"));

        verify(dayCloseService)
                .createDayClose(any(), eq(1001L), eq(2001L), eq(9001L));
    }

    @Test
    void updateDayClose_Success() throws Exception {
        DayCloseDto request = new DayCloseDto();
        DayCloseDto response = new DayCloseDto();

        when(dayCloseService.updateDayClose(
                any(), eq(100L), eq(1001L), eq(2001L), eq(9001L)))
                .thenReturn(response);

        mockMvc.perform(
                        put("/v1/day-close-shipping/update/100")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day Close updated successfully"));

        verify(dayCloseService)
                .updateDayClose(any(), eq(100L), eq(1001L), eq(2001L), eq(9001L));
    }

    @Test
    void deleteDayClose_Success() throws Exception {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test reason");

        doNothing().when(dayCloseService).deleteDayClose(eq(100L), any());

        mockMvc.perform(
                        delete("/v1/day-close-shipping/100")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deleteReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DayClose Shipping deleted successfully"));

        verify(dayCloseService).deleteDayClose(eq(100L), any());
    }

    @Test
    void deleteDayClose_WithoutDeleteReason_Success() throws Exception {
        doNothing().when(dayCloseService).deleteDayClose(eq(100L), eq(null));

        mockMvc.perform(
                        delete("/v1/day-close-shipping/100")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DayClose Shipping deleted successfully"));

        verify(dayCloseService).deleteDayClose(eq(100L), eq(null));
    }

    @Test
    void searchDayClose_WithBothDates_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "false", List.of());
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of());
        response.put("totalElements", 0);

        when(dayCloseService.searchDayClose(
                eq("DOC123"), any(), any(Pageable.class),
                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(response);

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .param("startDate", "2025-01-01")
                                .param("endDate", "2025-12-31")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day Close list fetched successfully"));

        verify(dayCloseService).searchDayClose(
                eq("DOC123"), any(), any(Pageable.class),
                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31)));
    }

    @Test
    void searchDayClose_WithoutDates_Success() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of());

        when(dayCloseService.searchDayClose(
                eq("DOC123"), any(), any(Pageable.class), any(), any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day Close list fetched successfully"));

        verify(dayCloseService).searchDayClose(
                eq("DOC123"), any(), any(Pageable.class), any(), any());
    }

    @Test
    void searchDayClose_WithOnlyStartDate_ReturnsBadRequest() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "false", List.of());

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .param("startDate", "2025-01-01")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isBadRequest());

        verify(dayCloseService, never()).searchDayClose(any(), any(), any(), any(), any());
    }

    @Test
    void searchDayClose_WithOnlyEndDate_ReturnsBadRequest() throws Exception {
        FilterRequestDto filters = new FilterRequestDto("AND", "false", List.of());

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .param("endDate", "2025-12-31")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isBadRequest());

        verify(dayCloseService, never()).searchDayClose(any(), any(), any(), any(), any());
    }

    @Test
    void searchDayClose_WithoutFilters_Success() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of());

        when(dayCloseService.searchDayClose(
                eq("DOC123"), eq(null), any(Pageable.class), any(), any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Day Close list fetched successfully"));

        verify(dayCloseService).searchDayClose(
                eq("DOC123"), eq(null), any(Pageable.class), any(), any());
    }

    @Test
    void print_Success() throws Exception {
        byte[] pdfBytes = "PDF".getBytes();

        when(dayCloseService.print(100L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/v1/day-close-shipping/print/100"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=day-close-shipping-100.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(dayCloseService).print(100L);
    }

    @Test
    void print_ThrowsException_ReturnsError() throws Exception {
        when(dayCloseService.print(100L))
                .thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(get("/v1/day-close-shipping/print/100"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to generate PDF: PDF generation failed"));

        verify(dayCloseService).print(100L);
    }
}
