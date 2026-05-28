package com.asg.shipping.bookingFormSH.controller;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ExcelExportService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.service.BookingFormService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        when(bookingFormService.searchBookingForm(eq("DOC123"), eq(filters), any(Pageable.class), isNull(), isNull())).thenReturn(response);

        mockMvc.perform(post("/v1/booking-form-sh/search").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters))).andExpect(status().isOk());

        verify(bookingFormService).searchBookingForm(eq("DOC123"), eq(filters), any(Pageable.class), isNull(), isNull());
    }

    @Test
    void searchBookingForm_WithoutFilters() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of());
        response.put("totalElements", 0);

        when(bookingFormService.searchBookingForm(eq("DOC123"), isNull(), any(Pageable.class), isNull(), isNull())).thenReturn(response);

        mockMvc.perform(post("/v1/booking-form-sh/search").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(bookingFormService).searchBookingForm(eq("DOC123"), isNull(), any(Pageable.class), isNull(), isNull());
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
        doNothing().when(bookingFormService).deleteBookingForm(eq(1L), any());

        mockMvc.perform(delete("/v1/booking-form-sh/1")).andExpect(status().isOk());

        verify(bookingFormService).deleteBookingForm(eq(1L), any());
        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.DELETED, "DOC123", "1");
    }

    /* ---------------- COPRAR ---------------- */

    @Test
    void generateCoprarBooking_Success() throws Exception {
        when(bookingFormService.generateCoprarBooking(1L)).thenReturn("COPRAR generated successfully");

        mockMvc.perform(post("/v1/booking-form-sh/1/generate-copran")).andExpect(status().isOk());

        verify(bookingFormService).generateCoprarBooking(1L);
    }

    @Test
    void generateCoprarBooking_Error() throws Exception {
        when(bookingFormService.generateCoprarBooking(1L)).thenReturn("ERROR: COPRAR failed");

        mockMvc.perform(post("/v1/booking-form-sh/1/generate-copran")).andExpect(status().isBadRequest());

        verify(bookingFormService).generateCoprarBooking(1L);
    }

    @Test
    void generateCoprarBooking_ErrorLowerCase() throws Exception {
        when(bookingFormService.generateCoprarBooking(1L)).thenReturn("error: coprar failed");

        mockMvc.perform(post("/v1/booking-form-sh/1/generate-copran")).andExpect(status().isBadRequest());

        verify(bookingFormService).generateCoprarBooking(1L);
    }

    /* ---------------- EMPTY SHIPPER ---------------- */

    @Test
    void getEmptyShipper_Success() throws Exception {
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(2001L);
        when(bookingFormService.getEmptyShipper(2001L)).thenReturn("SHIPPER.1");

        mockMvc.perform(get("/v1/booking-form-sh/empty-shipper"))
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

    @Test
    void processEmptyContainerLoad_ErrorLowerCase() throws Exception {
        when(bookingFormService.processEmptyContainerLoad(1L)).thenReturn("error: failed");

        mockMvc.perform(post("/v1/booking-form-sh/1/process-empty-container-load"))
                .andExpect(status().isInternalServerError());

        verify(bookingFormService).processEmptyContainerLoad(1L);
    }

    /* ---------------- EXCEL EXPORT ---------------- */

    @Test
    void exportExcel_Success() throws Exception {
        byte[] excelContent = "Excel Content".getBytes();
        ExcelFileData excelData = new ExcelFileData(excelContent, "VGMCustXLFile.xlsx");

        when(excelExportService.generateExcel(eq("100-311"), eq("476"), eq(null), eq("VGMCustXLFile.xlsx")))
                .thenReturn(excelData);

        mockMvc.perform(get("/v1/booking-form-sh/excel/vgmCustXLGenerateXL/476"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=VGMCustXLFile_476.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(excelExportService).generateExcel(eq("100-311"), eq("476"), eq(null), eq("VGMCustXLFile.xlsx"));
    }

    @Test
    void exportExcel_Exception() throws Exception {
        when(excelExportService.generateExcel(eq("100-311"), eq("476"), eq(null), eq("VGMCustXLFile.xlsx")))
                .thenThrow(new RuntimeException("Excel generation failed"));

        mockMvc.perform(get("/v1/booking-form-sh/excel/vgmCustXLGenerateXL/476"))
                .andExpect(status().isInternalServerError());

        verify(excelExportService).generateExcel(eq("100-311"), eq("476"), eq(null), eq("VGMCustXLFile.xlsx"));
    }

    /* ---------------- PRINT FORMS ---------------- */

    @Test
    void mateBookingPrintForm_Success() throws Exception {
        byte[] pdfContent = "PDF Content".getBytes();

        when(bookingFormService.mateBookingPrintForm(21L)).thenReturn(pdfContent);

        mockMvc.perform(get("/v1/booking-form-sh/mateBookingPrintForm/21"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=container-mate-receipts-21.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(bookingFormService).mateBookingPrintForm(21L);
    }

    @Test
    void mateBookingPrintForm_Exception() throws Exception {
        when(bookingFormService.mateBookingPrintForm(21L)).thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(get("/v1/booking-form-sh/mateBookingPrintForm/21"))
                .andExpect(status().isInternalServerError());

        verify(bookingFormService).mateBookingPrintForm(21L);
    }

    @Test
    void cntEmptyBookingPrintForm_Success() throws Exception {
        byte[] pdfContent = "PDF Content".getBytes();

        when(bookingFormService.cntEmptyBookingPrintForm(21L)).thenReturn(pdfContent);

        mockMvc.perform(get("/v1/booking-form-sh/empty-release/21"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=container-empty-release-21.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(bookingFormService).cntEmptyBookingPrintForm(21L);
    }

    @Test
    void cntEmptyBookingPrintForm_Exception() throws Exception {
        when(bookingFormService.cntEmptyBookingPrintForm(21L)).thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(get("/v1/booking-form-sh/empty-release/21"))
                .andExpect(status().isInternalServerError());

        verify(bookingFormService).cntEmptyBookingPrintForm(21L);
    }

    @Test
    void cntReturnBookingPrintFormAll_Success() throws Exception {
        byte[] pdfContent = "PDF Content".getBytes();

        when(bookingFormService.cntReturnBookingPrintFormAll(21L, "Y")).thenReturn(pdfContent);

        mockMvc.perform(get("/v1/booking-form-sh/all-container/21").param("printStamp", "Y"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=all-container-21.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(bookingFormService).cntReturnBookingPrintFormAll(21L, "Y");
    }

    @Test
    void cntReturnBookingPrintFormAll_Exception() throws Exception {
        when(bookingFormService.cntReturnBookingPrintFormAll(21L, "Y"))
                .thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(get("/v1/booking-form-sh/all-container/21").param("printStamp", "Y"))
                .andExpect(status().isInternalServerError());

        verify(bookingFormService).cntReturnBookingPrintFormAll(21L, "Y");
    }

    /* ---------------- STUFFING ADVICE DOWNLOAD ---------------- */

    @Test
    void downloadStuffingAdvice_Success() throws Exception {
        byte[] excelContent = "Excel Content".getBytes();
        when(bookingFormService.exportStuffingAdviceExcel(21L)).thenReturn(excelContent);

        mockMvc.perform(get("/v1/booking-form-sh/download-stuffing-advice/21"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=Stuffing_Advice_21.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(bookingFormService).exportStuffingAdviceExcel(21L);
    }

    /* ---------------- CONTAINER INVENTORY SEARCH ---------------- */

    @Test
    void searchContainerInventory_Success() throws Exception {
        Map<String, Object> response = Map.of("content", List.of(), "totalElements", 5);
        when(bookingFormService.searchContainerInventory(eq("DOC123"), isNull(), isNull(), eq(103L), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/booking-form-sh/container-inventory/search")
                        .param("linePoid", "103"))
                .andExpect(status().isOk());

        verify(bookingFormService).searchContainerInventory(eq("DOC123"), isNull(), isNull(), eq(103L), any(Pageable.class));
    }

    @Test
    void searchContainerInventory_WithFilters() throws Exception {
        Map<String, Object> response = Map.of("content", List.of(), "totalElements", 2);
        when(bookingFormService.searchContainerInventory(eq("DOC123"), eq("TEMU123"), eq("20DV"), eq(103L), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/booking-form-sh/container-inventory/search")
                        .param("linePoid", "103")
                        .param("containerNo", "TEMU123")
                        .param("isoType", "20DV"))
                .andExpect(status().isOk());

        verify(bookingFormService).searchContainerInventory(eq("DOC123"), eq("TEMU123"), eq("20DV"), eq(103L), any(Pageable.class));
    }

    /* ---------------- HELPER METHODS ---------------- */

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
