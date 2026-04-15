package com.asg.shipping.remuneration.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.repository.GlMasterRepository;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import com.asg.shipping.remuneration.repository.ShipRemunerationMasterRepository;
import com.asg.shipping.shippingffchargemaster.repository.ShipChargeMasterRepository;
import com.asg.common.lib.security.util.UserContext;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemunerationMasterServiceImplTest {

    @Mock
    private ShipRemunerationMasterRepository repository;

    @Mock
    private GlMasterRepository glMasterRepository;

    @Mock
    private ShipChargeMasterRepository shipChargeMasterRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private RemunerationMasterServiceImpl service;

    private ShipRemunerationMasterRequestDto validRequest;
    private ShipRemunerationMaster savedEntity;
    private DeleteReasonDto deleteReasonDto;
    private MockedStatic<UserContext> userContext;

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userContext = mockStatic(UserContext.class);
        userContext.when(UserContext::getDocumentId).thenReturn("DOC-001");

        validRequest = new ShipRemunerationMasterRequestDto();
        validRequest.setRemunCode("TEST001");
        validRequest.setRemunDescription("Test Description");
        validRequest.setRemunChargeCodePoid(1L);
        validRequest.setGlPoid(2L);
        validRequest.setActive("Y");
        validRequest.setImpExpType("IMP");
        validRequest.setSeqNo(1);
        validRequest.setRemunBasedOn("WEIGHT");
        validRequest.setRemunBookedByUsed("N");

        savedEntity = new ShipRemunerationMaster();
        savedEntity.setRemunerationPoid(1L);
        savedEntity.setRemunCode("TEST001");
        savedEntity.setRemunDescription("Test Description");
        savedEntity.setRemunChargeCodePoid("1");
        savedEntity.setGlPoid(2L);
        savedEntity.setActive("Y");
        savedEntity.setDeleted("N");
        savedEntity.setCreatedDate(LocalDateTime.now());

        deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion reason");
    }

    @AfterEach
    void tearDown() {
        userContext.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRemuneration — success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createRemuneration_Success() throws ValidationException {
        when(repository.existsByRemunCodeIgnoreCase(anyString())).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any(ShipRemunerationMaster.class))).thenReturn(savedEntity);
        when(repository.findById(anyLong())).thenReturn(Optional.of(savedEntity));

        ShipRemunerationMasterResponseDto result = service.createRemuneration(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.getRemunerationPoid());
        assertEquals("TEST001", result.getRemunCode());

        verify(repository).existsByRemunCodeIgnoreCase("TEST001");
        verify(shipChargeMasterRepository).existsByChargePoid(1L);
        verify(glMasterRepository).existsByGlPoid(2L);
        verify(repository).save(any(ShipRemunerationMaster.class));
        verify(repository).findById(1L);
        verify(loggingService).createLogSummaryEntry(any(com.asg.common.lib.enums.LogDetailsEnum.class), nullable(String.class), anyString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRemuneration — blank code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createRemuneration_BlankCode_ThrowsResourceAlreadyExistsException() {
        validRequest.setRemunCode("");
        when(repository.existsByRemunCodeIgnoreCase("")).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any())).thenReturn(savedEntity);
        when(repository.findById(anyLong())).thenReturn(Optional.of(savedEntity));

        assertDoesNotThrow(() -> service.createRemuneration(validRequest));
    }

    @Test
    void createRemuneration_NullCode_ThrowsNullPointerException() {
        validRequest.setRemunCode(null);
        when(repository.existsByRemunCodeIgnoreCase(null)).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any())).thenReturn(savedEntity);
        when(repository.findById(anyLong())).thenReturn(Optional.of(savedEntity));

        assertDoesNotThrow(() -> service.createRemuneration(validRequest));
    }

    @Test
    void createRemuneration_WhitespaceCode_Succeeds() {
        validRequest.setRemunCode("   ");
        when(repository.existsByRemunCodeIgnoreCase("   ")).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any())).thenReturn(savedEntity);
        when(repository.findById(anyLong())).thenReturn(Optional.of(savedEntity));

        assertDoesNotThrow(() -> service.createRemuneration(validRequest));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRemuneration — duplicate code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createRemuneration_DuplicateCode_ThrowsResourceAlreadyExistsException() {
        when(repository.existsByRemunCodeIgnoreCase("TEST001")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.createRemuneration(validRequest));

        verify(repository).existsByRemunCodeIgnoreCase("TEST001");
        verifyNoInteractions(shipChargeMasterRepository, glMasterRepository);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRemuneration — invalid charge code poid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createRemuneration_ChargeCodeNotFound_ThrowsResourceNotFoundException() {
        when(repository.existsByRemunCodeIgnoreCase(anyString())).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.createRemuneration(validRequest));

        verify(shipChargeMasterRepository).existsByChargePoid(1L);
        verifyNoInteractions(glMasterRepository);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRemuneration — invalid GL poid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createRemuneration_GlPoidNotFound_ThrowsResourceNotFoundException() {
        when(repository.existsByRemunCodeIgnoreCase(anyString())).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.createRemuneration(validRequest));

        verify(glMasterRepository).existsByGlPoid(2L);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRemuneration — success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateRemuneration_Success() {
        ShipRemunerationMasterRequestDto updateRequest = new ShipRemunerationMasterRequestDto();
        updateRequest.setRemunDescription("Updated Description");
        updateRequest.setRemunChargeCodePoid(1L);
        updateRequest.setGlPoid(2L);
        updateRequest.setActive("N");

        ShipRemunerationMaster updatedEntity = new ShipRemunerationMaster();
        updatedEntity.setRemunerationPoid(1L);
        updatedEntity.setRemunDescription("Updated Description");
        updatedEntity.setActive("N");

        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(shipChargeMasterRepository.existsByChargePoid(1L)).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(2L)).thenReturn(true);
        when(repository.save(any(ShipRemunerationMaster.class))).thenReturn(updatedEntity);

        ShipRemunerationMasterResponseDto result = service.updateRemuneration(1L, updateRequest);

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(shipChargeMasterRepository).existsByChargePoid(1L);
        verify(glMasterRepository).existsByGlPoid(2L);
        verify(repository).save(any(ShipRemunerationMaster.class));
        verify(loggingService).logChanges(any(), any(), any(), any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRemuneration — not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateRemuneration_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateRemuneration(99L, request));

        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRemuneration — invalid charge code poid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateRemuneration_ChargeCodeNotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(false);

        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunChargeCodePoid(1L);
        request.setGlPoid(2L);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateRemuneration(1L, request));

        verify(shipChargeMasterRepository).existsByChargePoid(1L);
        verifyNoInteractions(glMasterRepository);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRemuneration — invalid GL poid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateRemuneration_GlPoidNotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(shipChargeMasterRepository.existsByChargePoid(anyLong())).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(false);

        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunChargeCodePoid(1L);
        request.setGlPoid(2L);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateRemuneration(1L, request));

        verify(glMasterRepository).existsByGlPoid(2L);
        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRemunerationById — success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getRemunerationById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));

        ShipRemunerationMasterResponseDto result = service.getRemunerationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getRemunerationPoid());
        assertEquals("TEST001", result.getRemunCode());
        assertEquals("Test Description", result.getRemunDescription());

        verify(repository).findById(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRemunerationById — not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getRemunerationById_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRemunerationById(99L));

        verify(repository).findById(99L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // softDeleteRemuneration — success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteRemuneration_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(savedEntity));

        service.deleteRemuneration(1L, deleteReasonDto);

        verify(repository).findById(1L);
        verify(documentDeleteService).deleteDocument(
                eq(1L),
                eq("SHIP_REMUNERATION_MASTER"),
                eq("REMUNERATION_POID"),
                eq(deleteReasonDto),
                eq(LocalDate.from(savedEntity.getCreatedDate()))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // softDeleteRemuneration — not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteRemuneration_NotFound_ThrowsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteRemuneration(99L, deleteReasonDto));

        verify(repository).findById(99L);
        verifyNoInteractions(documentDeleteService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listRemunerations — success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void listRemunerations_Success() {
        String docId = "DOC_001";
        FilterRequestDto filterRequest = new FilterRequestDto(null, null, Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);

        List<Map<String, Object>> records = List.of(
                Map.of("REMUNERATION_POID", 1L, "REMUN_DESCRIPTION", "Test Description")
        );
        RawSearchResult rawSearchResult = new RawSearchResult(records, Collections.emptyMap(), 1L);

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(Collections.emptyList());
        when(documentService.search(
                eq(docId), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("REMUN_DESCRIPTION"), eq("REMUNERATION_POID"))
        ).thenReturn(rawSearchResult);

        Map<String, Object> result = service.listRemunerations(docId, filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).resolveOperator(filterRequest);
        verify(documentService).resolveIsDeleted(filterRequest);
        verify(documentService).resolveFilters(filterRequest);
        verify(documentService).search(
                eq(docId), anyList(), eq("AND"), eq(pageable), eq("N"),
                eq("REMUN_DESCRIPTION"), eq("REMUNERATION_POID"));
    }

    @Test
    void listRemunerations_EmptyResult() {
        String docId = "DOC_002";
        FilterRequestDto filterRequest = new FilterRequestDto(null, null, Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult emptyResult = new RawSearchResult(Collections.emptyList(), Collections.emptyMap(), 0L);

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(Collections.emptyList());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyResult);

        Map<String, Object> result = service.listRemunerations(docId, filterRequest, pageable);

        assertNotNull(result);
    }
}
