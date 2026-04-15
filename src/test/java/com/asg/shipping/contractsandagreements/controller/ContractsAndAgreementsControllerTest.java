package com.asg.shipping.contractsandagreements.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractsAndAgreementsControllerTest {

    @Mock
    private ContractsAndAgreementsService service;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ContractsAndAgreementsController controller;

    private MockedStatic<UserContext> userContextMockedStatic;

    @BeforeEach
    void setUp() {
        userContextMockedStatic = mockStatic(UserContext.class);
        userContextMockedStatic.when(UserContext::getDocumentId).thenReturn("1001");
    }

    @AfterEach
    void tearDown() {
        if (userContextMockedStatic != null) {
            userContextMockedStatic.close();
        }
    }

    @Test
    void testCreate() {
        AdminContractsAgreementHdrDto request = new AdminContractsAgreementHdrDto();
        AdminContractsAgreementHdrDto responseDto = new AdminContractsAgreementHdrDto();
        
        when(service.createContractsAndAgreements(request)).thenReturn(responseDto);

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).createContractsAndAgreements(request);
    }

    @Test
    void testGetById() {
        AdminContractsAgreementHdrDto responseDto = new AdminContractsAgreementHdrDto();
        when(service.getContractsAndAgreementsById(1L)).thenReturn(responseDto);

        ResponseEntity<?> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).getContractsAndAgreementsById(1L);
        verify(loggingService).createLogSummaryEntry(any(com.asg.common.lib.enums.LogDetailsEnum.class), anyString(), anyString());
    }

    @Test
    void testDelete() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        ResponseEntity<?> response = controller.delete(1L, deleteReasonDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).deleteContractsAndAgreements(1L, deleteReasonDto);
    }

    @Test
    void testList() {
        FilterRequestDto filter = new FilterRequestDto("AND", "N", java.util.List.of());
        Pageable pageable = mock(Pageable.class);
        Map<String, Object> mapResponse = Map.of("data", "value");

        when(service.list(filter, pageable)).thenReturn(mapResponse);

        ResponseEntity<?> response = controller.list(pageable, filter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).list(filter, pageable);
    }

    @Test
    void testUpdate() {
        AdminContractsAgreementHdrDto request = new AdminContractsAgreementHdrDto();
        AdminContractsAgreementHdrDto responseDto = new AdminContractsAgreementHdrDto();
        
        when(service.updateContractsAndAgreements(1L, request)).thenReturn(responseDto);

        ResponseEntity<?> response = controller.update(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).updateContractsAndAgreements(1L, request);
    }
}
