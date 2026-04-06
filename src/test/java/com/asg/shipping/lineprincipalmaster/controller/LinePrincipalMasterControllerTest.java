package com.asg.shipping.lineprincipalmaster.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.lineprincipalmaster.dto.LinePrincipalMasterDto;
import com.asg.shipping.lineprincipalmaster.service.LinePrincipalMasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinePrincipalMasterControllerTest {

    @Mock
    private LinePrincipalMasterService lineService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private LinePrincipalMasterController controller;

    @Test
    void getLine_LogsViewedSummary() {
        LinePrincipalMasterDto dto = LinePrincipalMasterDto.builder()
                .linePoid(1L)
                .lineCode("LINE001")
                .lineName("Line One")
                .build();

        when(lineService.getLine(1L)).thenReturn(dto);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-007");

            ResponseEntity<?> response = controller.getLine(1L);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            verify(lineService).getLine(1L);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "100-007", "1");
        }
    }

    @Test
    void deleteLine_ForwardsDeleteReason() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("cleanup");

        doNothing().when(lineService).deleteLine(1L, deleteReasonDto);

        ResponseEntity<?> response = controller.deleteLine(1L, deleteReasonDto);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(lineService).deleteLine(1L, deleteReasonDto);
    }
}