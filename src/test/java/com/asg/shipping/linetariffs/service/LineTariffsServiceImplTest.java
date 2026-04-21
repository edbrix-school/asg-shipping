package com.asg.shipping.linetariffs.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.repository.ShipContainerTypeMasterRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.CopyTariffRequestDTO;
import com.asg.shipping.linetariffs.dto.LineTariffCreateDTO;
import com.asg.shipping.linetariffs.dto.LineTariffDto;
import com.asg.shipping.linetariffs.dto.LineTariffUpdateDTO;
import com.asg.shipping.linetariffs.entity.*;
import com.asg.shipping.linetariffs.repository.*;
import com.asg.shipping.linetariffs.util.LineTariffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        verify(documentService).search(eq("100-050"), anyList(), eq("AND"), eq(pageable), eq("N"), eq("DESCRIPTION"), eq("TRANSACTION_POID"));
    }

    @Test
    void getLineTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(hdr));
            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.getLineTariff(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("100-050"), eq("1"));
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
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), eq(1L)))
                    .thenReturn(false);
            when(tariffHdrRepository.save(any(ShipLineTariffHdr.class))).thenReturn(hdr);
            doNothing().when(tariffHdrRepository).flush();

            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(impDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(impPayDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(expDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(expPayDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(mapper.mapToDto(eq(hdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.updateLineTariff(1L, updateDTO, 1L, 2L);

            assertNotNull(result);
            verify(loggingService).logChanges(any(), any(), eq(ShipLineTariffHdr.class), eq("100-050"), eq("1"),
                    eq(LogDetailsEnum.MODIFIED), eq("TRANSACTION_POID"));
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
    void copyLineTariff_Success_NoDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-050");

            ShipLineTariffHdr source = new ShipLineTariffHdr();
            source.setTransactionPoid(5L);
            source.setGroupPoid(1L);
            source.setCompanyPoid(1L);
            source.setLinePoid(10L);
            source.setDescription("Source");
            source.setPeriodFrom(LocalDate.of(2026, 1, 1));
            source.setPeriodTo(LocalDate.of(2026, 12, 31));

            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 1, 1))
                    .periodTo(LocalDate.of(2027, 12, 31))
                    .description("Copied")
                    .build();

            ShipLineTariffHdr newHdr = new ShipLineTariffHdr();
            newHdr.setTransactionPoid(99L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(5L, 1L)).thenReturn(Optional.of(source));
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), isNull()))
                    .thenReturn(false);
            lenient().when(tariffHdrRepository.save(any(ShipLineTariffHdr.class))).thenAnswer(invocation -> {
                ShipLineTariffHdr arg = invocation.getArgument(0);
                return arg.getTransactionPoid() == null ? newHdr : arg;
            });

            when(impDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong())).thenReturn(Collections.emptyList());
            when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong())).thenReturn(Collections.emptyList());
            when(expDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong())).thenReturn(Collections.emptyList());
            when(expPayDtlRepository.findByTransactionPoidOrderByDetRowId(anyLong())).thenReturn(Collections.emptyList());
            when(mapper.mapToDto(eq(newHdr), anyList(), anyList(), anyList(), anyList(), anyMap())).thenReturn(dto);

            LineTariffDto result = service.copyLineTariff(5L, request, 1L, 2L);

            assertNotNull(result);
            ArgumentCaptor<ShipLineTariffHdr> hdrCaptor = ArgumentCaptor.forClass(ShipLineTariffHdr.class);
            verify(tariffHdrRepository, atLeastOnce()).save(hdrCaptor.capture());
            ShipLineTariffHdr savedSource = hdrCaptor.getAllValues().stream()
                    .filter(h -> Long.valueOf(5L).equals(h.getTransactionPoid()))
                    .findFirst().orElseThrow();
            assertEquals(request.getPeriodFrom().minusDays(1), savedSource.getPeriodTo());
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("100-050"), eq("99"));
        }
    }

    @Test
    void copyLineTariff_PeriodFromAfterTo_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 12, 31))
                    .periodTo(LocalDate.of(2027, 1, 1))
                    .build();

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(5L, 1L)).thenReturn(Optional.of(hdr));

            assertThrows(ValidationException.class, () -> service.copyLineTariff(5L, request, 1L, 2L));
        }
    }

    @Test
    void copyLineTariff_OverlappingPeriod_ThrowsValidationException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            CopyTariffRequestDTO request = CopyTariffRequestDTO.builder()
                    .periodFrom(LocalDate.of(2027, 1, 1))
                    .periodTo(LocalDate.of(2027, 12, 31))
                    .build();

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(5L, 1L)).thenReturn(Optional.of(hdr));
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyLong(), anyLong(), any(), any(), isNull()))
                    .thenReturn(true);

            assertThrows(ValidationException.class, () -> service.copyLineTariff(5L, request, 1L, 2L));
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
    void copySlabsToPayable_NoMatchingContainerType_SkipsUpdate() {
        ShipLineTariffImpDtl col = new ShipLineTariffImpDtl();
        col.setContainerTypePoid(1L);
        col.setFreeDays(5);

        ShipLineTariffImpPayDtl pay = ShipLineTariffImpPayDtl.builder()
                .containerTypePoid(2L) // different container type
                .build();

        when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(col));
        when(impPayDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(pay));

        service.copySlabsToPayable(1L, "DMG");

        verify(impPayDtlRepository, never()).save(any());
    }

    @Test
    void copySlabsToPayable_InvalidType_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> service.copySlabsToPayable(1L, "INVALID"));
    }

    @Test
    void loadContainerTypes_IMP_ExcludesUsedPoids() {
        ShipLineTariffImpDtl used = new ShipLineTariffImpDtl();
        used.setContainerTypePoid(22L);

        ShipContainerTypeMaster ct = ShipContainerTypeMaster.builder()
                .containerTypePoid(42L).containerTypeCode("2250").containerTypeName("20' Open Top").build();

        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
        when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(used));
        when(lineMasterTypeRepository.findAvailableContainerTypePoids(eq(10L), anyList())).thenReturn(List.of(42L));
        when(containerTypeRepository.findAllById(List.of(42L))).thenReturn(List.of(ct));

        List<LovGetListDto> result = service.loadContainerTypes(1L, "IMP");

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getPoid());
        assertEquals("2250", result.get(0).getCode());
        assertEquals("20' Open Top", result.get(0).getLabel());
        verify(lineMasterTypeRepository).findAvailableContainerTypePoids(eq(10L), argThat(list -> list.contains(22L)));
    }

    @Test
    void loadContainerTypes_EXP_ExcludesUsedPoids() {
        ShipLineTariffExpDtl used = new ShipLineTariffExpDtl();
        used.setContainerTypePoid(66L);

        ShipContainerTypeMaster ct = ShipContainerTypeMaster.builder()
                .containerTypePoid(75L).containerTypeCode("4400").containerTypeName("40' DRY VAN").build();

        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
        when(expDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(List.of(used));
        when(lineMasterTypeRepository.findAvailableContainerTypePoids(eq(10L), anyList())).thenReturn(List.of(75L));
        when(containerTypeRepository.findAllById(List.of(75L))).thenReturn(List.of(ct));

        List<LovGetListDto> result = service.loadContainerTypes(1L, "EXP");

        assertEquals(1, result.size());
        assertEquals(75L, result.get(0).getPoid());
        verify(lineMasterTypeRepository).findAvailableContainerTypePoids(eq(10L), argThat(list -> list.contains(66L)));
    }

    @Test
    void loadContainerTypes_NoExistingDetails_UsesMinusOneFallback() {
        ShipContainerTypeMaster ct = ShipContainerTypeMaster.builder()
                .containerTypePoid(22L).containerTypeCode("2200").containerTypeName("20' DRY VAN").build();

        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.of(hdr));
        when(impDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
        when(lineMasterTypeRepository.findAvailableContainerTypePoids(eq(10L), eq(List.of(-1L)))).thenReturn(List.of(22L));
        when(containerTypeRepository.findAllById(List.of(22L))).thenReturn(List.of(ct));

        List<LovGetListDto> result = service.loadContainerTypes(1L, "IMP");

        assertEquals(1, result.size());
        verify(lineMasterTypeRepository).findAvailableContainerTypePoids(10L, List.of(-1L));
    }

    @Test
    void loadContainerTypes_TariffNotFound_ThrowsResourceNotFoundException() {
        when(tariffHdrRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.loadContainerTypes(1L, "IMP"));
    }
}
