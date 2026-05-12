package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorCreateDTO;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorUpdateDTO;
import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintHdr;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintChargeDtlRepository;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintContainerDtlRepository;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintHdrRepository;
import com.asg.shipping.shippingmanifestcorrector.util.ManifestCorrectorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManifestCorrectorServiceImplTest {

    @Mock
    private ShipBlReprintHdrRepository hdrRepository;
    @Mock
    private ShipBlReprintChargeDtlRepository chargeDtlRepository;
    @Mock
    private ShipBlReprintContainerDtlRepository containerDtlRepository;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ManifestCorrectorMapper mapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ManifestCorrectorServiceImpl service;

    private ShipBlReprintHdr testEntity;
    private ManifestCorrectorCreateDTO createDTO;
    private ManifestCorrectorUpdateDTO updateDTO;
    private ManifestCorrectorDto responseDTO;

    @BeforeEach
    void setup() {
        testEntity = new ShipBlReprintHdr();
        testEntity.setTransactionPoid(1L);
        testEntity.setDeleted("N");
        testEntity.setBlNumber("12345");

        createDTO = new ManifestCorrectorCreateDTO();
        createDTO.setBlNumber("12345");
        createDTO.setTransactionDate(LocalDate.now());
        createDTO.setBlReprint("Y");

        updateDTO = new ManifestCorrectorUpdateDTO();
        updateDTO.setBlNumber("12345");

        responseDTO = new ManifestCorrectorDto();
        responseDTO.setTransactionPoid(1L);
    }

    @Test
    void searchManifestCorrector_Success() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "TEST");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "MC001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null))
                .thenReturn(List.of(filter));
        when(documentSearchService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result = service.searchManifestCorrector("100-143", filterRequest, null, null, pageable);

        assertNotNull(result);
        verify(documentSearchService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getManifestCorrectorById_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(mapper.mapToDto(any())).thenReturn(responseDTO);

        ManifestCorrectorDto result = service.getManifestCorrectorById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
    }

    @Test
    void getManifestCorrectorById_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getManifestCorrectorById(1L));
    }

    @Test
    @Disabled
    void createManifestCorrector_Success() {
        when(hdrRepository.saveAndFlush(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn("COMP");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);

        ManifestCorrectorDto result = service.createManifestCorrector(createDTO);

        assertNotNull(result);
        verify(hdrRepository).saveAndFlush(any());
    }

    @Test
    void updateManifestCorrector_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateManifestCorrector(1L, updateDTO));
    }

    @Test
    void deleteManifestCorrector_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(documentDeleteService.deleteDocument(any(), any(), any(), any(), any())).thenReturn(null);

        assertDoesNotThrow(() -> service.deleteManifestCorrector(1L, null));

        verify(hdrRepository).saveAndFlush(argThat(e -> "Y".equals(e.getDeleted())));
        verify(chargeDtlRepository).deleteByTransactionPoid(1L);
        verify(containerDtlRepository).deleteByTransactionPoid(1L);
    }

    @Test
    void deleteManifestCorrector_NotFound() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteManifestCorrector(1L, null));
    }

    @Test
    void updateManifestCorrector_Success() {
        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(hdrRepository.saveAndFlush(any())).thenReturn(testEntity);
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);

        ManifestCorrectorDto result = service.updateManifestCorrector(1L, updateDTO);

        assertNotNull(result);
        verify(hdrRepository).saveAndFlush(any());
    }

    @Test
    void validateRefundAmounts_Success() {
        var request = com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest.builder()
                .blPoid(1L)
                .containerNumber("CONT123")
                .revPayable(java.math.BigDecimal.valueOf(100))
                .revIncome(java.math.BigDecimal.valueOf(50))
                .perQuantityAmount(java.math.BigDecimal.valueOf(200))
                .build();

        when(jdbcTemplate.execute(anyString(), any(org.springframework.jdbc.core.CallableStatementCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.CallableStatementCallback<?> callback = invocation.getArgument(1);
                    return null;
                });

        var result = service.validateRefundAmounts(request);

        assertNotNull(result);
    }

    @Test
    void validateRefundAmounts_InvalidAmount() {
        var request = com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest.builder()
                .blPoid(1L)
                .containerNumber("CONT123")
                .revPayable(java.math.BigDecimal.valueOf(150))
                .revIncome(java.math.BigDecimal.valueOf(100))
                .perQuantityAmount(java.math.BigDecimal.valueOf(200))
                .build();

        var result = service.validateRefundAmounts(request);

        assertNotNull(result);
    }

    @Test
    void searchManifestCorrector_EmptyResult() {
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(),
                Map.of(),
                0L
        );

        when(documentSearchService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentSearchService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", null, null))
                .thenReturn(List.of());
        when(documentSearchService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()))
                .thenReturn(raw);

        Map<String, Object> result = service.searchManifestCorrector("100-143", filterRequest, null, null, pageable);

        assertNotNull(result);
    }

    @Test
    void getManifestCorrectorById_WithDetails() {
        var chargeDtl = new com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintChargeDtl();
        var containerDtl = new com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintContainerDtl();

        when(hdrRepository.findActiveByTransactionPoid(1L)).thenReturn(Optional.of(testEntity));
        when(chargeDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(chargeDtl));
        when(containerDtlRepository.findByTransactionPoid(1L)).thenReturn(List.of(containerDtl));
        when(mapper.mapToDto(any())).thenReturn(responseDTO);
        when(mapper.mapChargeDtlListToDto(any())).thenReturn(List.of());
        when(mapper.mapContainerDtlListToDto(any())).thenReturn(List.of());

        ManifestCorrectorDto result = service.getManifestCorrectorById(1L);

        assertNotNull(result);
        verify(chargeDtlRepository).findByTransactionPoid(1L);
        verify(containerDtlRepository).findByTransactionPoid(1L);
    }
}
