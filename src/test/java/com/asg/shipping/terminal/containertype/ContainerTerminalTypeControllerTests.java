package com.asg.shipping.terminal.containertype;

import com.asg.shipping.terminal.containertype.controller.ContainerTerminalTypeController;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import com.asg.shipping.terminal.containertype.service.ContainerTerminalTypeService;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContainerTerminalTypeControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ContainerTerminalTypeService service;

    @InjectMocks
    private ContainerTerminalTypeController controller;
    private MockedStatic<UserContext> userContextMock;

    private ContainerTerminalTypeRequest requestDTO;
    private ContainerTerminalTypeResponse responseDTO;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(
                new PageableHandlerMethodArgumentResolver()
        ).build();

        requestDTO = new ContainerTerminalTypeRequest();
        requestDTO.setContainerTerminalTypeCode("ISO20");
        requestDTO.setContainerTerminalTypeName("Twenty Feet");
        requestDTO.setContainerTerminalTypeSize(20L);
        requestDTO.setSeqNo(null);
        requestDTO.setActive("Y");

        responseDTO = new ContainerTerminalTypeResponse();
        responseDTO.setContainerTerminalTypePoid(1L);
        responseDTO.setContainerTerminalTypeCode("ISO20");
        responseDTO.setContainerTerminalTypeName("Twenty Feet");
        responseDTO.setContainerTerminalTypeSize(20L);
        responseDTO.setSeqNo(BigInteger.valueOf(10));
        responseDTO.setActive("Y");
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }


    // ---------------- LIST ----------------

    private FilterRequestDto buildFilterRequest() {
        FilterRequestDto dto = new FilterRequestDto("OR","N", List.of());
        return dto;
    }


    @Test
    void testListContainerTerminalTypes() throws Exception {

        Map<String, Object> result = new HashMap<>();
        result.put("records", new Object[]{});
        result.put("totalElements", 0);

        when(service.listContainerTerminalTypes(any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(post("/v1/container-terminal-types/list")
                        .header("X-Document-Id", "000-000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFilterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container Terminal Type list fetched successfully"));
    }

    @Test
    void testListContainerTerminalTypes_WhenServiceThrows_ReturnsInternalServerError() throws Exception {
        when(service.listContainerTerminalTypes(any(), any(), any()))
                .thenThrow(new RuntimeException("list-fail"));

        mockMvc.perform(post("/v1/container-terminal-types/list")
                        .header("X-Document-Id", "000-000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildFilterRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Unable to fetch container terminal types: list-fail"));
    }

    // ---------------- GET BY ID ----------------

    @Test
    void testGetById() throws Exception {

        when(service.getById(eq(1L), eq(100L)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/v1/container-terminal-types/1")
                        .header("X-Group-Poid", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container terminal type retrieved successfully"))
                .andExpect(jsonPath("$.result.data.containerTerminalTypeCode")
                        .value("ISO20"));
    }


    // ---------------- CREATE ----------------

    @Test
    void testCreateContainerTerminalType() throws Exception {

        when(service.create(any(ContainerTerminalTypeRequest.class), eq(100L), eq("admin"), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/v1/container-terminal-types")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "000-000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container terminal type created successfully"));
    }

    // ---------------- UPDATE ----------------

    @Test
    void testUpdateContainerTerminalType() throws Exception {

        when(service.update(eq(1L), any(ContainerTerminalTypeRequest.class), eq(100L), eq("admin"), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/v1/container-terminal-types/1")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin")
                        .header("X-Document-Id", "000-000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container terminal type updated successfully"));
    }

    // ---------------- DELETE (soft) ----------------

    @Test
    void testSoftDeleteContainerTerminalType() throws Exception {

        doNothing().when(service)
                .delete(eq(1L), eq(100L), eq("admin"));

        mockMvc.perform(delete("/v1/container-terminal-types/1")
                        .header("X-Group-Poid", 100L)
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container terminal type deleted successfully"));
    }

    @Test
    void testToggleActiveStatus() throws Exception {
        userContextMock = org.mockito.Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(100L);
        userContextMock.when(UserContext::getUserId).thenReturn("admin");

        doNothing().when(service).toggleActiveStatus(1L, 100L, "admin");

        mockMvc.perform(put("/v1/container-terminal-types/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Container terminal type status toggled successfully"));
    }
}

