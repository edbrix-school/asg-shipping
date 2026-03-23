package com.asg.shipping.portmaster.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.asg.shipping.portmaster.dto.PortMasterRequest;
import com.asg.shipping.portmaster.dto.PortMasterResponse;
import com.asg.shipping.portmaster.service.PortMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PortMasterControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private MockedStatic<UserContext> mockedUserContext;

	@Mock
	private PortMasterService service;

	@Mock
	private LoggingService loggingService;

	@InjectMocks
	private PortMasterController controller;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();

		mockedUserContext = mockStatic(UserContext.class);
		mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

		PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(pageableResolver).build();
	}

	@AfterEach
	void tearDown() {
		if (mockedUserContext != null) {
			mockedUserContext.close();
		}
	}

	@Test
	void createPort_Success() throws Exception {

		PortMasterRequest request = createMockRequest();

		Map<String, Object> responseMap = Map.of("portPoid", 1L, "portCode", "PORT01");

		when(service.createPort( any())).thenReturn(responseMap);

		mockMvc.perform(post("/v1/port-master").param("groupPoid", "1001").param("userPoid", "admin")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(service).createPort(any());
	}

	@Test
	void updatePort_Success() throws Exception {

		PortMasterRequest request = createMockRequest();
		PortMasterResponse response = createMockResponse();

		when(service.updatePort( eq(1L), any())).thenReturn(response);

		mockMvc.perform(put("/v1/port-master/1").param("groupPoid", "1001").param("userPoid", "admin")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(service).updatePort( eq(1L), any());
	}

	@Test
	void getPortById_Success() throws Exception {

		when(service.getPortById(eq(1L))).thenReturn(createMockResponse());

		mockMvc.perform(get("/v1/port-master/1").param("groupPoid", "1001")).andExpect(status().isOk());

		verify(service).getPortById( eq(1L));
		verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("DOC123"), eq("1"));
	}

	@Test
	void getAllPorts_Success() throws Exception {

		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

		Map<String, Object> responseMap = Map.of("content", List.of(createMockResponse()), "totalElements", 1,
				"totalPages", 1);

		when(service.getAllPorts(eq("DOC123"), any(), any(Pageable.class))).thenReturn(responseMap);

		mockMvc.perform(post("/v1/port-master/list").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(filters))).andExpect(status().isOk());

		verify(service).getAllPorts(eq("DOC123"), any(), any(Pageable.class));
	}
	
	@Test
	void getAllPorts_NullFilters_Success() throws Exception {

		Map<String, Object> responseMap = Map.of("content", List.of(), "totalElements", 0, "totalPages", 0);

		when(service.getAllPorts(eq("DOC123"), eq(null), any(Pageable.class))).thenReturn(responseMap);

		mockMvc.perform(post("/v1/port-master/list").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		verify(service).getAllPorts(eq("DOC123"), eq(null), any(Pageable.class));
	}

	@Test
	void deletePort_Success() throws Exception {

		mockMvc.perform(delete("/v1/port-master/1").param("groupPoid", "1001").param("userPoid", "admin"))
				.andExpect(status().isOk());

		verify(service).deletePort(eq(1L));
		verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.DELETED), eq("DOC123"), eq("1"));
	}

	private PortMasterRequest createMockRequest() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("PORT01");
		request.setPortName("Test Port");
		request.setCountryPoid(101L);
		request.setActive("Y");
		request.setTradelanePoid(1L);
		request.setBerths("Berth 1");
		return request;
	}

	private PortMasterResponse createMockResponse() {
		PortMasterResponse response = new PortMasterResponse();
		response.setPortPoid(1L);
		response.setPortCode("PORT01");
		response.setPortName("Test Port");
		response.setActive("Y");
		return response;
	}
}
