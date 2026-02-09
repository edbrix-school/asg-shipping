package com.asg.shipping.dayCloseShiping.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseHdrDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjectionImpl;
import com.asg.shipping.dayCloseShiping.service.DayCloseService;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    /* -------------------- GET BY ID -------------------- */

    @Test
    void getDayClose_Success() throws Exception {
        DayCloseDto response = new DayCloseDto();

        when(dayCloseService.getDayClose(100L, 1001L, 2001L)).thenReturn(response);

        mockMvc.perform(get("/v1/day-close-shipping/100"))
                .andExpect(status().isOk());

        verify(dayCloseService).getDayClose(100L, 1001L, 2001L);
        verify(loggingService).createLogSummaryEntry(
                LogDetailsEnum.VIEWED, "DOC123", "100");
    }

    /* -------------------- NEW DAY CLOSE -------------------- */

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
                .andExpect(status().isOk());

        verify(dayCloseService)
                .getNewDayCloseData(1001L, 2001L, "2025-07-06");
    }

    /* -------------------- DENOMINATIONS -------------------- */

    @Test
    void getDenominations_Success() throws Exception {
        List<Map<String, Object>> response =
                List.of(Map.of("DENOMINATION", 10, "COUNT", 5));

        when(dayCloseService.getDenominations("BHD"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/v1/day-close-shipping/denominations")
                                .param("currencyCode", "BHD"))
                .andExpect(status().isOk());

        verify(dayCloseService).getDenominations("BHD");
    }

    /* -------------------- CREATE -------------------- */

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
                .andExpect(status().isOk());

        verify(dayCloseService)
                .createDayClose(any(), eq(1001L), eq(2001L), eq(9001L));
    }

    /* -------------------- UPDATE -------------------- */

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
                .andExpect(status().isOk());

        verify(dayCloseService)
                .updateDayClose(any(), eq(100L), eq(1001L), eq(2001L), eq(9001L));
    }

    /* -------------------- SEARCH -------------------- */

    @Test
    void searchDayClose_Success() throws Exception {
        FilterRequestDto filters =
                new FilterRequestDto("OR", "false", List.of());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("data", Map.of("content", List.of()));

        when(dayCloseService.searchDayClose(eq("DOC123"), eq(filters), any(Pageable.class)))
                .thenReturn(responseMap);

        mockMvc.perform(
                        post("/v1/day-close-shipping/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(dayCloseService)
                .searchDayClose(eq("DOC123"), eq(filters), any(Pageable.class));
    }

    /* -------------------- PRINT (SUCCESS ONLY) -------------------- */

    @Test
    void print_Success() throws Exception {
        byte[] pdfBytes = "PDF".getBytes();

        when(dayCloseService.print(100L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/v1/day-close-shipping/print/100"))
                .andExpect(status().isOk());

        verify(dayCloseService).print(100L);
    }
}
