package com.asg.shipping.collectionhandover.controller;

import com.asg.common.lib.service.DocumentDownloadHeaderService;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;
import com.asg.shipping.collectionhandover.service.CollectionHandoverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionHandoverControllerTest {

    private static final String DOC_ID = "300-106";

    @Mock
    private CollectionHandoverService collectionHandoverService;

    @Spy
    private DocumentDownloadHeaderService downloadHeaderService =
            new DocumentDownloadHeaderService(mock(JdbcTemplate.class));

    @InjectMocks
    private CollectionHandoverController controller;

    @BeforeEach
    void setUp() {
        // No-op; just ensuring controller/service injection happens.
    }

    @Test
    void searchCollectionHandovers_success_noDates_noSort() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                any(FilterRequestDto.class),
                any(Pageable.class),
                isNull(),
                isNull()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                new FilterRequestDto("OR", "false", List.of()),
                0,
                20,
                null,
                null,
                null);

        assertEquals(200, response.getStatusCode().value());
        verify(collectionHandoverService).searchCollectionHandovers(eq(DOC_ID), any(), any(Pageable.class), isNull(), isNull());
    }

    @Test
    void searchCollectionHandovers_badRequest_whenOnlyStartDateProvided() {
        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                null,
                LocalDate.of(2026, 1, 1),
                null);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(collectionHandoverService);
    }

    @Test
    void searchCollectionHandovers_badRequest_whenOnlyEndDateProvided() {
        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                null,
                null,
                LocalDate.of(2026, 12, 31));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(collectionHandoverService);
    }

    @Test
    void searchCollectionHandovers_success_whenBothDatesProvided() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                nullable(FilterRequestDto.class),
                any(Pageable.class),
                eq(startDate),
                eq(endDate)))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                null,
                startDate,
                endDate);

        assertEquals(200, response.getStatusCode().value());
        verify(collectionHandoverService).searchCollectionHandovers(
                eq(DOC_ID),
                isNull(),
                any(Pageable.class),
                eq(startDate),
                eq(endDate));
    }

    @Test
    void searchCollectionHandovers_sortsAsc_whenOnlyFieldProvided() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                nullable(FilterRequestDto.class),
                pageableCaptor.capture(),
                isNull(),
                isNull()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                "docRef",
                null,
                null);

        assertEquals(200, response.getStatusCode().value());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order order = pageable.getSort().iterator().next();
        assertEquals("docRef", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void searchCollectionHandovers_sortsDesc_whenDirectionIsDesc() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                nullable(FilterRequestDto.class),
                pageableCaptor.capture(),
                isNull(),
                isNull()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                "docRef,desc",
                null,
                null);

        assertEquals(200, response.getStatusCode().value());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order order = pageable.getSort().iterator().next();
        assertEquals("docRef", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void searchCollectionHandovers_sortsAsc_whenDirectionIsNotDesc() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                nullable(FilterRequestDto.class),
                pageableCaptor.capture(),
                isNull(),
                isNull()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                "docRef,asc",
                null,
                null);

        assertEquals(200, response.getStatusCode().value());

        Pageable pageable = pageableCaptor.getValue();
        Sort.Order order = pageable.getSort().iterator().next();
        assertEquals("docRef", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void searchCollectionHandovers_sortEmptyString_resultsInUnsorted() {
        Map<String, Object> result = Map.of("content", List.of(), "totalElements", 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                nullable(FilterRequestDto.class),
                pageableCaptor.capture(),
                isNull(),
                isNull()))
                .thenReturn(result);

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                "",
                null,
                null);

        assertEquals(200, response.getStatusCode().value());

        Pageable pageable = pageableCaptor.getValue();
        assertTrue(pageable.getSort().isUnsorted());
        verify(collectionHandoverService).searchCollectionHandovers(eq(DOC_ID), any(), any(Pageable.class), isNull(), isNull());
    }

    @Test
    void searchCollectionHandovers_serviceThrows_returnsInternalServerError() {
        when(collectionHandoverService.searchCollectionHandovers(
                eq(DOC_ID),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.searchCollectionHandovers(
                null,
                0,
                20,
                null,
                null,
                null);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertTrue(((Map<?, ?>) response.getBody()).get("message").toString().contains("boom"));
    }

    @Test
    void getCollectionHandover_success() {
        CollectionHandoverDto dto = new CollectionHandoverDto();
        dto.setTransactionPoid(1L);
        when(collectionHandoverService.getCollectionHandover(1L)).thenReturn(dto);

        ResponseEntity<?> response = controller.getCollectionHandover(1L);

        assertEquals(200, response.getStatusCode().value());
        verify(collectionHandoverService).getCollectionHandover(1L);
    }

    @Test
    void createCollectionHandover_success() {
        CollectionHandoverCreateDTO request = new CollectionHandoverCreateDTO();
        CollectionHandoverDto created = new CollectionHandoverDto();
        created.setTransactionPoid(10L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(2L);

            when(collectionHandoverService.createCollectionHandover(same(request), eq(1L), eq(2L)))
                    .thenReturn(created);

            ResponseEntity<?> response = controller.createCollectionHandover(request);

            assertEquals(200, response.getStatusCode().value());
            verify(collectionHandoverService).createCollectionHandover(same(request), eq(1L), eq(2L));
        }
    }

    @Test
    void updateCollectionHandover_success() {
        CollectionHandoverUpdateDTO request = new CollectionHandoverUpdateDTO();
        CollectionHandoverDto updated = new CollectionHandoverDto();
        updated.setTransactionPoid(11L);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(3L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(4L);

            when(collectionHandoverService.updateCollectionHandover(eq(1L), same(request), eq(3L), eq(4L)))
                    .thenReturn(updated);

            ResponseEntity<?> response = controller.updateCollectionHandover(1L, request);

            assertEquals(200, response.getStatusCode().value());
            verify(collectionHandoverService).updateCollectionHandover(eq(1L), same(request), eq(3L), eq(4L));
        }
    }

    @Test
    void deleteCollectionHandover_success() {
        doNothing().when(collectionHandoverService).deleteCollectionHandover(1L);

        ResponseEntity<?> response = controller.deleteCollectionHandover(1L);

        assertEquals(200, response.getStatusCode().value());
        verify(collectionHandoverService).deleteCollectionHandover(1L);
    }

    @Test
    void toggleVerifyStatus_success() {
        doNothing().when(collectionHandoverService).toggleVerifyStatus(1L, "Y", "remarks");

        ResponseEntity<?> response = controller.toggleVerifyStatus(1L, "Y", "remarks");

        assertEquals(200, response.getStatusCode().value());
        verify(collectionHandoverService).toggleVerifyStatus(1L, "Y", "remarks");
    }

    @Test
    void print_success() throws Exception {
        byte[] pdfBytes = new byte[] {1, 2, 3};
        when(collectionHandoverService.print(10L)).thenReturn(pdfBytes);

        ResponseEntity<?> response = controller.print(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=\"Collection-Handover-10.pdf\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(pdfBytes, (byte[]) response.getBody());
        verify(collectionHandoverService).print(10L);
    }

    @Test
    void print_exception_returnsErrorWithMessage() throws Exception {
        when(collectionHandoverService.print(10L))
                .thenThrow(new RuntimeException("PDF generation failed"));

        ResponseEntity<?> response = controller.print(10L);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(
                "Failed to generate PDF: PDF generation failed",
                ((Map<?, ?>) response.getBody()).get("message"));
    }
}

