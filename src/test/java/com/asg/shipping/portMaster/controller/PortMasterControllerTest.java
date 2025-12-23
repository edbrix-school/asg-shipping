package com.asg.shipping.portMaster.controller;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;
import com.asg.shipping.portMaster.service.PortMasterService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PortMasterControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private MockedStatic<UserContext> mockedUserContext;

	@Mock
	private PortMasterService service;

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

		doNothing().when(service).createPort(eq(1001L), any(), eq("admin"));

		mockMvc.perform(post("/v1/port-master").param("groupPoid", "1001").param("userPoid", "admin")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(service).createPort(eq(1001L), any(), eq("admin"));
	}

	@Test
	void updatePort_Success() throws Exception {
		PortMasterRequest request = createMockRequest();
		PortMasterResponse response = createMockResponse();

		when(service.updatePort(eq(1001L), eq(1L), any(), eq("admin"))).thenReturn(response);

		mockMvc.perform(put("/v1/port-master/1").param("groupPoid", "1001").param("userPoid", "admin")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(service).updatePort(eq(1001L), eq(1L), any(), eq("admin"));
	}

	@Test
	void getPortById_Success() throws Exception {
		when(service.getPortById(eq(1001L), eq(1L))).thenReturn(createMockResponse());

		mockMvc.perform(get("/v1/port-master/1").param("groupPoid", "1001")).andExpect(status().isOk());

		verify(service).getPortById(eq(1001L), eq(1L));
	}

	@Test
	void getAllPorts_Success() throws Exception {
		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());

		Map<String, Object> responseMap = Map.of("content", List.of(createMockResponse()), "totalElements", 1,
				"totalPages", 1);

		when(service.getAllPorts(eq("DOC123"), eq(filters), any(Pageable.class))).thenReturn(responseMap);

		mockMvc.perform(get("/v1/port-master/list").param("page", "0").param("size", "10").param("sort", "portName,asc")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(filters)))
				.andExpect(status().isOk());

		verify(service).getAllPorts(eq("DOC123"), eq(filters), any(Pageable.class));
	}

	@Test
	void deletePort_Success() throws Exception {
		doNothing().when(service).deletePort(eq(1001L), eq(1L), eq("admin"));

		mockMvc.perform(delete("/v1/port-master/1").param("groupPoid", "1001").param("userPoid", "admin"))
				.andExpect(status().isOk());

		verify(service).deletePort(eq(1001L), eq(1L), eq("admin"));
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
