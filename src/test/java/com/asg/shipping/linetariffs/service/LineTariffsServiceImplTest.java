package com.asg.shipping.linetariffs.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.repository.ShipContainerTypeMasterRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import com.asg.shipping.linetariffs.repository.*;
import com.asg.shipping.linetariffs.util.LineTariffMapper;
import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineTariffsServiceImplTest {

    @Mock private ShipLineTariffHdrRepository tariffHdrRepository;
    @Mock private ShipLineTariffImpDtlRepository impDtlRepository;
    @Mock private ShipLineTariffImpPayDtlRepository impPayDtlRepository;
    @Mock private ShipLineTariffExpDtlRepository expDtlRepository;
    @Mock private ShipLineTariffExpPayDtlRepository expPayDtlRepository;
    @Mock private DocumentSearchService documentService;
    @Mock private LineTariffMapper mapper;
    @Mock private LoggingService loggingService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private ShipContainerTypeMasterRepository containerTypeRepository;
    @Mock private ShipLineMasterTypeRepository lineMasterTypeRepository;
    @Mock private EntityManager entityManager;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;

    @InjectMocks
    private LineTariffsServiceImpl service;

    private ShipLineTariffHdr hdr;
    private LineTariffDto dto;
    private LineTariffCreateDTO createDTO;
    private LineTariffUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        hdr = new ShipLineTariffHdr();
        hdr.setTransactionPoid(1L);
        hdr.setGroupPoid(1L);
        hdr.setCompanyPoid(1L);
        hdr.setLinePoid(10L);
        hdr.setPeriodFrom(LocalDate.of(2026, 1, 1));
        hdr.setPeriodTo(LocalDate.of(2026, 12, 31));
        hdr.setDmgFromSameday("N");
        hdr.setDmgFromNextday("Y");
        hdr.setDtnFromSameday("N");
        hdr.setDtnFromNextday("Y");

        dto = LineTariffDto.builder()
                .transactionPoid(1L)
                .linePoid(10L)
                .description("Test")
                .periodFrom(LocalDate.of(2026, 1, 1))
                .periodTo(LocalDate.of(2026, 12, 31))
                .build();

        createDTO = LineTariffCreateDTO.builder()
                .linePoid(10L)
                .periodFrom(LocalDate.of(2026, 1, 1))
                .periodTo(LocalDate.of(2026, 12, 31))
                .dmgFromSameday("N")
                .dmgFromNextday("Y")
                .dtnFromSameday("N")
                .dtnFromNextday("Y")
                .build();

        updateDTO = LineTariffUpdateDTO.builder()
                .description("Updated")
                .build();
    }

    @Test
    void searchLineTariffs_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 20);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

        Map<String, Object> result = service.searchLineTariffs("100-050", filterRequest, pageable, null, null);

        assertNotNull(result);
        verify(documentService).resolveIsDeleted(filterRequest);
        verify(documentService).search(eq("100-050"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID"));
    }

    @Test
    void searchLineTariffs_RemovesDeletedFilterFromRequest() {
        FilterRequestDto filterRequest = new FilterRequestDto(
                "AND",
                "N",
                List.of(new FilterDto("DESCRIPTION", "test"), new FilterDto("DELETED", "Y")));
        Pageable pageable = PageRequest.of(0, 20);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveFilters(any())).thenReturn(filterRequest.filters());
        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

        service.searchLineTariffs("100-050", filterRequest, pageable, null, null);

        verify(documentService).search(
                eq("100-050"),
                argThat(filters -> filters.size() == 1
                        && "DESCRIPTION".equals(filters.get(0).searchField())
                        && "test".equals(filters.get(0).searchValue())),
                eq("AND"),
                eq(pageable),
                eq("N"),
                eq("DESCRIPTION"),
                eq("TRANSACTION_POID"));
    }

    @Test
    void searchLineTariffs_UsesIsDeletedFromRequest() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "Y", Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 20);

        when(documentService.resolveOperator(any())).thenReturn("AND");
        when(documentService.resolveIsDeleted(any())).thenReturn("Y");
        when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

        service.searchLineTariffs("100-050", filterRequest, pageable, null, null);

        verify(documentService).resolveIsDeleted(filterRequest);
        verify(documentService).search(eq("100-050"), anyList(), eq("AND"), eq(pageable), eq("Y"), eq("DESCRIPTION"), eq("TRANSACTION_POID"));
    }

    @Test
    void getLineTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.getLineTariff(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            verify(loggingService, never()).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), anyString(), anyString());
        }
    }

    @Test
    void getLineTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.getLineTariff(1L));
        }
    }

    @Test
    void createLineTariff_Success_NoDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), isNull()))
                    .thenReturn(false);
            when(tariffHdrRepository.save(any(ShipLineTariffHdr.class))).thenReturn(hdr);
            doNothing().when(tariffHdrRepository).flush();

            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.createLineTariff(createDTO, 1L, 2L);

            assertNotNull(result);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("100-050"), eq("1"));
            verify(tariffHdrRepository).flush();
        }
    }

    @Test
    void createLineTariff_InvalidPeriod_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            createDTO.setPeriodFrom(LocalDate.of(2026, 12, 31));
            createDTO.setPeriodTo(LocalDate.of(2026, 1, 1));
            assertThrows(ValidationException.class, () -> service.createLineTariff(createDTO, 1L, 2L));
        }
    }

    @Test
    void createLineTariff_OverlappingPeriod_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), isNull()))
                    .thenReturn(true);
            assertThrows(ValidationException.class, () -> service.createLineTariff(createDTO, 1L, 2L));
        }
    }

    @Test
    void createLineTariff_DocRefExists_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            createDTO.setDocRef("DOC1");
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), isNull()))
                    .thenReturn(false);
            when(tariffHdrRepository.existsByDocRef("DOC1")).thenReturn(true);
            assertThrows(ValidationException.class, () -> service.createLineTariff(createDTO, 1L, 2L));
        }
    }

    @Test
    void updateLineTariff_Success_NoDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.save(any(ShipLineTariffHdr.class))).thenReturn(hdr);
            doNothing().when(tariffHdrRepository).flush();

            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.updateLineTariff(1L, updateDTO, 1L, 2L);

            assertNotNull(result);
            verify(loggingService).logChanges(any(), any(), eq(ShipLineTariffHdr.class), eq("100-050"), eq("1"),
                    eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
        }
    }

    @Test
    void updateLineTariff_SkipsOverlapCheckWhenPeriodAndLineUnchanged() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            updateDTO.setLinePoid(10L);
            updateDTO.setPeriodFrom(LocalDate.of(2026, 1, 1));
            updateDTO.setPeriodTo(LocalDate.of(2026, 12, 31));

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.save(any(ShipLineTariffHdr.class))).thenReturn(hdr);
            doNothing().when(tariffHdrRepository).flush();

            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.updateLineTariff(1L, updateDTO, 1L, 2L);

            assertNotNull(result);
            verify(tariffHdrRepository, never()).existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), any());
        }
    }

    @Test
    void updateLineTariff_OverlappingPeriodWhenPeriodChanged_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            updateDTO.setPeriodFrom(LocalDate.of(2026, 6, 1));
            updateDTO.setPeriodTo(LocalDate.of(2027, 5, 31));

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), eq(1L)))
                    .thenReturn(true);

            assertThrows(ValidationException.class, () -> service.updateLineTariff(1L, updateDTO, 1L, 2L));
        }
    }

    @Test
    void updateLineTariff_NotFound() {
        when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateLineTariff(1L, updateDTO, 1L, 2L));
    }

    @Test
    void deleteLineTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));

            service.deleteLineTariff(1L, null);

            verify(documentDeleteService).deleteDocument(eq(1L), eq("SHIP_LINE_TARIFF_HDR"), eq("TRANSACTION_POID"), isNull(), isNull());
        }
    }

    @Test
    void deleteLineTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.deleteLineTariff(1L, null));
        }
    }

    @Test
    @Disabled
    void copyLineTariff_Success_NoDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            ShipLineTariffHdr source = new ShipLineTariffHdr();
            source.setTransactionPoid(5L);
            source.setGroupPoid(1L);
            source.setLinePoid(10L);

            ShipLineTariffHdr newHdr = new ShipLineTariffHdr();
            newHdr.setTransactionPoid(99L);
            newHdr.setGroupPoid(1L);
            newHdr.setLinePoid(10L);

            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 1, 1))
                    .periodTo(LocalDate.of(2027, 12, 31))
                    .description("Copied")
                    .build();

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(5L, 1L)).thenReturn(Optional.of(source));
            doNothing().when(tariffHdrRepository).callCopyLineTariff(5L);
            when(tariffHdrRepository.findById(5L)).thenReturn(Optional.of(source));
            when(tariffHdrRepository.findLatestByLinePoidAndGroupPoid(10L, 1L)).thenReturn(List.of(newHdr, source));
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(99L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(newHdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.copyLineTariff(5L, request, 1L, 2L);

            assertNotNull(result);
            verify(tariffHdrRepository).callCopyLineTariff(5L);
            verify(entityManager).flush();
            verify(entityManager).clear();
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("100-050"), eq("99"));
        }
    }

    @Test
    void copyLineTariff_NotFound_ThrowsResourceNotFoundException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(5L, 1L)).thenReturn(Optional.empty());
            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 1, 1))
                    .periodTo(LocalDate.of(2027, 12, 31))
                    .build();
            assertThrows(ResourceNotFoundException.class, () -> service.copyLineTariff(5L, request, 1L, 2L));
        }
    }

    @Test
    void copySlabsToPayable_DMG_CopiesMatchingContainerType() {
        ShipLineTariffImpDtl col = new ShipLineTariffImpDtl();
        col.setContainerTypePoid(1L);
        col.setFreeDays(5);
        col.setSlab1Tilldays(10);
        col.setSlab1Rate(BigDecimal.valueOf(100));

        ShipLineTariffImpPayDtl pay = ShipLineTariffImpPayDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .containerTypePoid(1L)
                .freeDays(0)
                .build();

        when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(col));
        when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(pay));

        service.copySlabsToPayable(1L, "DMG");

        verify(impPayDtlRepository).save(pay);
        assertEquals(5, pay.getFreeDays());
        assertEquals(10, pay.getSlab1Tilldays());
        assertEquals(BigDecimal.valueOf(100), pay.getSlab1Rate());
    }

    @Test
    void copySlabsToPayable_DTN_CopiesMatchingContainerType() {
        ShipLineTariffExpDtl col = new ShipLineTariffExpDtl();
        col.setContainerTypePoid(1L);
        col.setFreeDays(7);
        col.setSlab1Tilldays(14);
        col.setSlab1Rate(BigDecimal.valueOf(80));

        ShipLineTariffExpPayDtl pay = new ShipLineTariffExpPayDtl();
        pay.setTransactionPoid(1L);
        pay.setDetRowId(1L);
        pay.setContainerTypePoid(1L);
        pay.setFreeDays(0);

        when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(col));
        when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(pay));

        service.copySlabsToPayable(1L, "DTN");

        verify(expPayDtlRepository).save(pay);
        assertEquals(7, pay.getFreeDays());
        assertEquals(14, pay.getSlab1Tilldays());
        assertEquals(BigDecimal.valueOf(80), pay.getSlab1Rate());
    }

    @Test
    void copySlabsToPayable_NoMatchingContainerType_CreatesNewPayableRow() {
        ShipLineTariffImpDtl col = new ShipLineTariffImpDtl();
        col.setContainerTypePoid(1L);
        col.setFreeDays(5);

        ShipLineTariffImpPayDtl pay = ShipLineTariffImpPayDtl.builder()
                .containerTypePoid(2L) // different container type
                .detRowId(1L)
                .build();

        when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(col));
        when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(pay));

        service.copySlabsToPayable(1L, "DMG");

        // new payable row created for containerTypePoid=1 since no match existed
        verify(impPayDtlRepository).save(argThat(p ->
                p.getContainerTypePoid().equals(1L) && p.getFreeDays() == 5));
    }

    @Test
    void copySlabsToPayable_InvalidType_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> service.copySlabsToPayable(1L, "INVALID"));
    }

    @Test
    void updateLineTariff_DeletesRowWithIsDeletedActionType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            TariffDetailUpdateDTO deleteRow = TariffDetailUpdateDTO.builder()
                    .detRowId(3L).containerTypePoid(149L).actionType("isDeleted").build();
            TariffDetailUpdateDTO keepRow = TariffDetailUpdateDTO.builder()
                    .detRowId(1L).containerTypePoid(147L).actionType("").freeDays(10).build();

            updateDTO.setImportDemurrageCollectable(List.of(keepRow, deleteRow));

            ShipLineTariffImpDtl deleteExisting = new ShipLineTariffImpDtl();
            deleteExisting.setDetRowId(3L);
            deleteExisting.setContainerTypePoid(149L);

            ShipLineTariffImpDtl keepExisting = new ShipLineTariffImpDtl();
            keepExisting.setDetRowId(1L);
            keepExisting.setContainerTypePoid(147L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.save(any())).thenReturn(hdr);
            doNothing().when(tariffHdrRepository).flush();
            // bulk fetch returns both rows for the map
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of(keepExisting, deleteExisting));
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(any(), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            service.updateLineTariff(1L, updateDTO, 1L, 2L);

            // deleteAllInBatch called with the isDeleted row
            verify(impDtlRepository).deleteAllInBatch(argThat(list ->
                    ((List<?>) list).size() == 1));
            // saveAll called with the keep row
            verify(impDtlRepository).saveAll(argThat(list ->
                    ((List<?>) list).size() == 1));
        }
    }

    @Test
    void loadContainerTypes_IMP_InsertsIntoCollectableAndPayable() {
        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));

        service.loadContainerTypes(1L, "IMP");

        verify(impDtlRepository).bulkInsertFromLine(1L, 10L);
        verify(impPayDtlRepository).bulkInsertFromLine(1L, 10L);
        verify(expDtlRepository, never()).bulkInsertFromLine(anyLong(), anyLong());
        verify(expPayDtlRepository, never()).bulkInsertFromLine(anyLong(), anyLong());
    }

    @Test
    void loadContainerTypes_EXP_InsertsIntoCollectableAndPayable() {
        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));

        service.loadContainerTypes(1L, "EXP");

        verify(expDtlRepository).bulkInsertFromLine(1L, 10L);
        verify(expPayDtlRepository).bulkInsertFromLine(1L, 10L);
        verify(impDtlRepository, never()).bulkInsertFromLine(anyLong(), anyLong());
        verify(impPayDtlRepository, never()).bulkInsertFromLine(anyLong(), anyLong());
    }

    @Test
    void loadContainerTypes_TariffNotFound_ThrowsResourceNotFoundException() {
        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.loadContainerTypes(1L, "IMP"));
    }

    @Test
    void print_Success() throws Exception {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(new ShipLineTariffImpDtl()));
            when(printService.buildBaseParams(1L, "100-050")).thenReturn(new HashMap<>());
            when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
            when(printService.fillReportToPdf(any(), any(), eq(dataSource))).thenReturn(new byte[]{1, 2, 3});

            byte[] result = service.print(1L);

            assertArrayEquals(new byte[]{1, 2, 3}, result);
            verify(printService).load("Shipping/SH/NOTICE2TRADE_subreport2.jrxml");
            verify(printService).load("Shipping/SH/NOTICE2TRADE.jrxml");
        }
    }

    @Test
    void print_TariffNotFound_ThrowsResourceNotFoundException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.print(1L));
        }
    }

    @Test
    void print_NoImpCollectableData_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of());

            assertThrows(ValidationException.class, () -> service.print(1L));
        }
    }
}
