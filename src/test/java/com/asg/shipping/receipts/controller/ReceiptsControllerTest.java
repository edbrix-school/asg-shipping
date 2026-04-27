package com.asg.shipping.receipts.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.enums.ButtonType;
import com.asg.shipping.receipts.service.ReceiptsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiptsControllerTest {

    @Mock
    private ReceiptsService receiptsService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private ReceiptsController receiptsController;

    private ReceiptsBlDetailsDto blDetailsDto;
    private ReceiptSaveResponseDto saveResponseDto;
    private ReceiptsCreateDto createDto;
    private ReceiptsUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        blDetailsDto = ReceiptsBlDetailsDto.builder()
                .docRef("RCP-2025-001")
                .blPoid(1001L)
                .companyPoid(100L)
                .build();

        saveResponseDto = ReceiptSaveResponseDto.builder()
                .docRef("RCP-2025-001")
                .transactionPoid(1L)
                .message("Success")
                .build();

        createDto = ReceiptsCreateDto.builder()
                .docRef("RCP-2025-001")
                .blPoid(1001L)
                .companyPoid(100L)
                .build();

        updateDto = ReceiptsUpdateDto.builder()
                .docRef("RCP-2025-001-UPD")
                .blPoid(1001L)
                .companyPoid(100L)
                .build();
    }

    @Test
    void getById_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC-001");
            when(receiptsService.getReceipt(1L)).thenReturn(blDetailsDto);

            ResponseEntity<?> response = receiptsController.getById(1L);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(receiptsService).getReceipt(1L);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("DOC-001"), eq("1"));
        }
    }

    @Test
    void create_Success() {
        when(receiptsService.createReceipt(any(ReceiptsCreateDto.class))).thenReturn(saveResponseDto);

        ResponseEntity<?> response = receiptsController.create(createDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(receiptsService).createReceipt(any(ReceiptsCreateDto.class));
    }

    @Test
    void update_Success() {
        when(receiptsService.updateReceipt(eq(1L), any(ReceiptsUpdateDto.class))).thenReturn(saveResponseDto);

        ResponseEntity<?> response = receiptsController.update(1L, updateDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(receiptsService).updateReceipt(eq(1L), any(ReceiptsUpdateDto.class));
    }

    @Test
    void delete_Success() {
        DeleteReasonDto deleteReason = new DeleteReasonDto();
        doNothing().when(receiptsService).deleteReceipt(eq(1L), any(DeleteReasonDto.class));

        ResponseEntity<?> response = receiptsController.delete(1L, deleteReason);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(receiptsService).deleteReceipt(eq(1L), any(DeleteReasonDto.class));
    }

    @Test
    void list_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto(
                "AND",
                "N",
                Collections.emptyList()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Map<String, Object> result = Collections.singletonMap("data", "test");
        when(receiptsService.list(any(), any())).thenReturn(result);

        ResponseEntity<?> response = receiptsController.list(pageable, filterRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(receiptsService).list(any(), any());
    }



    @Test
    void calculateDemurrage_Success() {
        ReceiptCalculateDemurrageRequestDto request = ReceiptCalculateDemurrageRequestDto.builder().build();
        ReceiptCalculateDemurrageResponseDto responseDto = ReceiptCalculateDemurrageResponseDto.builder().build();
        when(receiptsService.calculateDemurrage(any())).thenReturn(responseDto);

        ResponseEntity<?> response = receiptsController.calculateDemurrage(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(receiptsService).calculateDemurrage(any());
    }

    @Test
    void receiptAndInvoicePrint_Success() throws Exception {
        byte[] pdf = new byte[] { 1, 2, 3 };
        when(receiptsService.receiptAndInvoicePrint(eq(1L), eq(1001L), eq(ButtonType.Receipts))).thenReturn(pdf);

        ResponseEntity<?> response = receiptsController.receiptAndInvoicePrint(1L, 1001L, ButtonType.Receipts);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pdf, response.getBody());
    }

    @Test
    void receiptAndInvoicePrint_Exception() throws Exception {

        when(receiptsService.receiptAndInvoicePrint(
                eq(1L),
                eq(1001L),
                eq(ButtonType.Receipts)
        )).thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> response =
                receiptsController.receiptAndInvoicePrint(
                        1L,
                        1001L,
                        ButtonType.Receipts
                );

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Failed to generate PDF"));

        verify(receiptsService, times(1))
                .receiptAndInvoicePrint(1L, 1001L, ButtonType.Receipts);
    }
}
