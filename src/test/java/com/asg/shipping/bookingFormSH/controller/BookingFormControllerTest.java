package com.asg.shipping.bookingFormSH.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
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
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.service.BookingFormService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BookingFormControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private MockedStatic<UserContext> mockedUserContext;

	@Mock
	private BookingFormService bookingFormService;

	@Mock
	private ExcelExportService excelExportService;

	@Mock
	private LoggingService loggingService;

	@InjectMocks
	private BookingFormController controller;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

		mockedUserContext = mockStatic(UserContext.class);
		mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

		PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(pageableResolver).build();
	}

	@AfterEach
	void tearDown() {
		mockedUserContext.close();
	}

	/* ---------------- SEARCH ---------------- */

	@Test
	void searchBookingForm_Success() throws Exception {
		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

		Map<String, Object> response = Map.of("content", List.of(createMockDto()), "totalElements", 1);

		when(bookingFormService.searchBookingForm(eq("DOC123"), eq(filters), any(Pageable.class))).thenReturn(response);

		mockMvc.perform(post("/v1/booking-form-sh/search").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(filters))).andExpect(status().isOk());

		verify(bookingFormService).searchBookingForm(eq("DOC123"), eq(filters), any(Pageable.class));
	}

	/* ---------------- GET BY ID ---------------- */

	@Test
	void getBookingFormById_Success_WithLogging() throws Exception {
		when(bookingFormService.getBookingForm(1L)).thenReturn(createMockDto());

		mockMvc.perform(get("/v1/booking-form-sh/1")).andExpect(status().isOk());

		verify(bookingFormService).getBookingForm(1L);
		verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC123", "1");
	}

	/* ---------------- CREATE ---------------- */

	@Test
	void createBookingForm_Success() throws Exception {
		when(bookingFormService.createBookingForm(any())).thenReturn(createMockDto());

		mockMvc.perform(post("/v1/booking-form-sh").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createMockCreateDto()))).andExpect(status().isOk());

		verify(bookingFormService).createBookingForm(any());
	}

	/* ---------------- UPDATE ---------------- */

	@Test
	void updateBookingForm_Success() throws Exception {
		when(bookingFormService.getBookingForm(1L)).thenReturn(createMockDto());
		doNothing().when(bookingFormService).updateBookingForm(eq(1L), any());

		mockMvc.perform(put("/v1/booking-form-sh/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createMockUpdateDto()))).andExpect(status().isOk());

		verify(bookingFormService).updateBookingForm(eq(1L), any());
		verify(bookingFormService).getBookingForm(1L);
	}

	/* ---------------- DELETE ---------------- */

	@Test
	void deleteBookingForm_Success_WithLogging() throws Exception {
		doNothing().when(bookingFormService).deleteBookingForm(1L);

		mockMvc.perform(delete("/v1/booking-form-sh/1")).andExpect(status().isOk());

		verify(bookingFormService).deleteBookingForm(1L);
		verify(loggingService).createLogSummaryEntry(LogDetailsEnum.DELETED, "DOC123", "1");
	}

	/* ---------------- COPRAR ---------------- */

	@Test
	void generateCoprarBooking_Success() throws Exception {
		when(bookingFormService.generateCoprarBooking(1L)).thenReturn("COPRAR generated successfully");

		mockMvc.perform(post("/v1/booking-form-sh/1/generate-coprar")).andExpect(status().isOk());

		verify(bookingFormService).generateCoprarBooking(1L);
	}

	@Test
	void generateCoprarBooking_Error() throws Exception {
		when(bookingFormService.generateCoprarBooking(1L)).thenReturn("ERROR: COPRAR failed");

		mockMvc.perform(post("/v1/booking-form-sh/1/generate-coprar")).andExpect(status().isBadRequest());

		verify(bookingFormService).generateCoprarBooking(1L);
	}

	/* ---------------- EMPTY SHIPPER ---------------- */

	@Test
	void getEmptyShipper_Success() throws Exception {
		when(bookingFormService.getEmptyShipper(2001L)).thenReturn("SHIPPER.1");

		mockMvc.perform(get("/v1/booking-form-sh/empty-shipper").param("companyPoid", "2001"))
				.andExpect(status().isOk());

		verify(bookingFormService).getEmptyShipper(2001L);
	}

	/* ---------------- EMPTY CONTAINER LOAD ---------------- */

	@Test
	void processEmptyContainerLoad_Success() throws Exception {
		when(bookingFormService.processEmptyContainerLoad(1L)).thenReturn("Success");

		mockMvc.perform(post("/v1/booking-form-sh/1/process-empty-container-load")).andExpect(status().isOk());

		verify(bookingFormService).processEmptyContainerLoad(1L);
	}

	@Test
	void processEmptyContainerLoad_Error() throws Exception {
		when(bookingFormService.processEmptyContainerLoad(1L)).thenReturn("ERROR: Failed");

		mockMvc.perform(post("/v1/booking-form-sh/1/process-empty-container-load"))
				.andExpect(status().isInternalServerError());

		verify(bookingFormService).processEmptyContainerLoad(1L);
	}


	private BookingFormDto createMockDto() {
		BookingFormDto dto = new BookingFormDto();
		dto.setTransactionPoid(1L);
		dto.setBookingIssueNo("BK001");
		return dto;
	}

	private BookingFormCreateDTO createMockCreateDto() {
		BookingFormCreateDTO dto = new BookingFormCreateDTO();
		dto.setBookingIssueNo("BK001");
		dto.setSalesmanPoid(1L);
		dto.setShipperPoid(1L);
		dto.setLinePoid(1L);
		dto.setVesselPoid(1L);
		dto.setVesselEtaDate(java.time.LocalDate.now());
		return dto;
	}

	private BookingFormUpdateDTO createMockUpdateDto() {
		BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
		dto.setBookingIssueNo("BK001-UPDATED");
		return dto;
	}
}
