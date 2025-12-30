package com.asg.shipping.dayCloseShiping.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
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

	@InjectMocks
	private DayCloseController controller;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();

		mockedUserContext = mockStatic(UserContext.class);
		mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1001L);
		mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2001L);
		mockedUserContext.when(UserContext::getUserPoid).thenReturn(9001L); // ✅ Long
		mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

		PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(pageableResolver).build();
	}

	@AfterEach
	void tearDown() {
		mockedUserContext.close();
	}

	@Test
	void getDayClose_Success() throws Exception {

		DayCloseDto response = new DayCloseDto();

		when(dayCloseService.getDayClose(100L, 1001L, 2001L)).thenReturn(response);

		mockMvc.perform(get("/v1/day-close-shipping/100")).andExpect(status().isOk());

		verify(dayCloseService).getDayClose(100L, 1001L, 2001L);
	}

	@Test
	void getNewDayClose_Success() throws Exception {

		DayCloseSummaryProjectionImpl projection = DayCloseSummaryProjectionImpl.builder()
				.transactionDate(LocalDate.now()).cashAmount(BigDecimal.valueOf(1000))
				.chequeAmount(BigDecimal.valueOf(500)).totalAmount(BigDecimal.valueOf(1500)).chequeCount(2L).build();

		when(dayCloseService.getNewDayCloseData(1001L, 2001L, "2025-07-06")).thenReturn(projection);

		mockMvc.perform(get("/v1/day-close-shipping/new").param("transactionDate", "2025-07-06"))
				.andExpect(status().isOk());

		verify(dayCloseService).getNewDayCloseData(1001L, 2001L, "2025-07-06");
	}

	@Test
	void getDenominations_Success() throws Exception {

		List<Map<String, Object>> response = List.of(Map.of("DENOMINATION", 10, "COUNT", 5));

		when(dayCloseService.getDenominations("BHD")).thenReturn(response);

		mockMvc.perform(get("/v1/day-close-shipping/denominations").param("currencyCode", "BHD"))
				.andExpect(status().isOk());

		verify(dayCloseService).getDenominations("BHD");
	}

	@Test
	void createDayClose_Success() throws Exception {

		DayCloseDto request = new DayCloseDto();
		DayCloseDto response = new DayCloseDto();

		when(dayCloseService.createDayClose(any(), eq(1001L), eq(2001L), eq(9001L))).thenReturn(response);

		mockMvc.perform(post("/v1/day-close-shipping").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

		verify(dayCloseService).createDayClose(any(), eq(1001L), eq(2001L), eq(9001L));
	}

	@Test
	void updateDayClose_Success() throws Exception {

		DayCloseDto request = new DayCloseDto();
		DayCloseDto response = new DayCloseDto();

		when(dayCloseService.updateDayClose(any(), eq(100L), eq(1001L), eq(2001L), eq(9001L))).thenReturn(response);

		mockMvc.perform(put("/v1/day-close-shipping/update/100").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

		verify(dayCloseService).updateDayClose(any(), eq(100L), eq(1001L), eq(2001L), eq(9001L));
	}

	@Test
	void searchDayClose_Success() throws Exception {

		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

		Map<String, Object> contentItem = new HashMap<>();
		contentItem.put("TRANSACTION_POID", 575L);
		contentItem.put("TRANSACTION_DATE", "2016-05-24T00:00:00");
		contentItem.put("DOC_REF", "ASGSH2141");
		contentItem.put("LOCATION_CODE", "PORT");
		contentItem.put("TOTAL_AMOUNT", 12069.449);
		contentItem.put("LOC_REMARKS", "GRAND MART CHQ & CASH 93.895 FOR YER CHQ .");
		contentItem.put("VERIFIED_RCVD", "Y");
		contentItem.put("MAIN_OFC_REMARKS", "OK");
		contentItem.put("DELETED", null);
		contentItem.put("label", "PORT");
		contentItem.put("value", 575L);

		Map<String, Object> responseData = new HashMap<>();
		responseData.put("pageNumber", 0);
		responseData.put("pageSize", 10);
		responseData.put("last", false);
		responseData.put("totalPages", 241);
		responseData.put("totalElements", 2405);
		responseData.put("displayFields", Map.of("TRANSACTION_POID", "text", "DOC_REF", "text", "LOC_REMARKS", "text",
				"TOTAL_AMOUNT", "text", "TRANSACTION_DATE", "text"));
		responseData.put("content", List.of(contentItem));

		Map<String, Object> responseMap = Map.of("data", responseData);

		mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

		when(dayCloseService.searchDayClose(eq("DOC123"), eq(filters), any(Pageable.class))).thenReturn(responseMap);

		mockMvc.perform(post("/v1/day-close-shipping/search").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(filters))).andExpect(status().isOk());

		verify(dayCloseService).searchDayClose(eq("DOC123"), eq(filters), any(Pageable.class));
	}

}
