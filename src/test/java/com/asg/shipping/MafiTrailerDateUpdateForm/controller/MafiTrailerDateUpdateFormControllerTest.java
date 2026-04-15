package com.asg.shipping.MafiTrailerDateUpdateForm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import com.asg.shipping.mafitrailerdateupdateform.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.mafitrailerdateupdateform.dto.MafiTrailerDateUpdateFormResponse;
import com.asg.shipping.mafitrailerdateupdateform.service.MafiTrailerDateUpdateFormService;
import com.asg.shipping.mafitrailerdateupdateform.controller.MafiTrailerDateUpdateFormController;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MafiTrailerDateUpdateFormControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<UserContext> mockedUserContext;

    @Mock
    private MafiTrailerDateUpdateFormService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private MafiTrailerDateUpdateFormController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockedUserContext = mockStatic(UserContext.class);
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
        if (mockedUserContext != null) {
            mockedUserContext.close();
        }
    }

    // ------------------------------------------------------
    // LIST
    // ------------------------------------------------------
    @Test
    void getAll_Success() throws Exception {

        FilterRequestDto filters =
                new FilterRequestDto("OR", "false", List.of());

        Map<String, Object> response =
                Map.of("content", List.of(), "totalElements", 0);

        when(service.getAll(eq("DOC123"), eq(filters), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/mafi-trailer-date-update/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk());

        verify(service)
                .getAll(eq("DOC123"), eq(filters), any(Pageable.class));
    }

    // ------------------------------------------------------
    // GET BY ID
    // ------------------------------------------------------
    @Test
    void getById_Success() throws Exception {

        when(service.getById(1001L, 2001L, 3001L))
                .thenReturn(new MafiTrailerDateUpdateFormResponse());

        mockMvc.perform(get("/v1/mafi-trailer-date-update/1001")
                .param("groupPoid", "2001")
                .param("companyPoid", "3001"))
                .andExpect(status().isOk());

        verify(service).getById(1001L, 2001L, 3001L);
        verify(loggingService).createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                "DOC123",
                "1001");
    }

    // ------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------
    @Test
    void update_Success() throws Exception {

        MafiTrailerDateUpdateFormRequest request =
                new MafiTrailerDateUpdateFormRequest();

        doNothing().when(service)
                .update(eq(1001L),
                        any(MafiTrailerDateUpdateFormRequest.class),
                        eq(2001L),
                        eq(3001L),
                        eq("admin"));

        mockMvc.perform(put("/v1/mafi-trailer-date-update/1001")
                .param("groupPoid", "2001")
                .param("companyPoid", "3001")
                .param("userPoid", "admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).update(eq(1001L),
                any(MafiTrailerDateUpdateFormRequest.class),
                eq(2001L),
                eq(3001L),
                eq("admin"));
    }
}
