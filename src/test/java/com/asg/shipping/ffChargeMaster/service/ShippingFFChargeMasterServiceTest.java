package com.asg.shipping.ffChargeMaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shippingffchargemaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingffchargemaster.dto.ChargeDto;
import com.asg.shipping.shippingffchargemaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingffchargemaster.entity.ShipChargeMaster;
import com.asg.shipping.shippingffchargemaster.entity.ShippingChargeLineViewEntity;
import com.asg.shipping.shippingffchargemaster.repository.ShipChargeMasterRepository;
import com.asg.shipping.shippingffchargemaster.repository.ShippingChargeLineViewRepository;
import com.asg.shipping.shippingffchargemaster.service.ShippingFFChargeMasterServiceImpl;
import com.asg.shipping.shippingffchargemaster.util.ChargeMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShippingFFChargeMasterServiceTest {

    @Mock
    private ShipChargeMasterRepository chargeRepository;

    @Mock
    private ChargeMasterMapper mapper;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private ShippingChargeLineViewRepository shippingChargeLineViewRepository;

    @Mock
    private LoggingService loggingService;

    @Mock
    private LovDataService lovService;

    @InjectMocks
    private ShippingFFChargeMasterServiceImpl service;

    private ChargeCreateDTO createDto;
    private ChargeUpdateDTO updateDto;
    private ShipChargeMaster entity;
    private ChargeDto responseDto;
    private LovGetListDto validLov;

    @BeforeEach
    void setUp() {
        createDto = ChargeCreateDTO.builder()
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .chargeRevenueType("REVENUE")
                .chargeType("FIXED")
                .divisionCode("DIV001")
                .taxPoid(1L)
                .chargeGroupPoid(2L)
                .shFfChargeMap(3L)
                .active("Y")
                .seqno(1)
                .build();

        updateDto = ChargeUpdateDTO.builder()
                .chargeName("Updated Charge")
                .chargeRevenueType("REVENUE")
                .chargeType("FIXED")
                .divisionCode("DIV001")
                .taxPoid(1L)
                .chargeGroupPoid(2L)
                .shFfChargeMap(3L)
                .active("Y")
                .seqno(1)
                .build();

        entity = ShipChargeMaster.builder()
                .chargePoid(1L)
                .groupPoid(100L)
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .divisionCode("DIV001")
                .active("Y")
                .deleted("N")
                .build();

        responseDto = ChargeDto.builder()
                .chargePoid(1L)
                .groupPoid(100L)
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .active("Y")
                .createdBy("testUser")
                .createdDate(LocalDateTime.now())
                .build();

        validLov = new LovGetListDto();
        validLov.setPoid(1L);
    }

    // ─── createCharge ────────────────────────────────────────────────────────

    @Test
    void createCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
            when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
            when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
            when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(validLov);
            doNothing().when(mapper).mapCreateDTOToEntity(eq(createDto), any(ShipChargeMaster.class), eq(100L), eq(123L));
            when(chargeRepository.save(any(ShipChargeMaster.class))).thenReturn(entity);
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            ChargeDto result = service.createCharge(createDto, 100L, 123L);

            assertNotNull(result);
            assertEquals(1L, result.getChargePoid());
            verify(chargeRepository).save(any(ShipChargeMaster.class));
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC001", "1");
        }
    }

    @Test
    void createCharge_DuplicateCode_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_DuplicateName_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_InvalidTaxMaster_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_InvalidChargeGroupMaster_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_InvalidChargeMasterFF_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_InvalidDivision_ThrowsValidationException() {
        when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
        when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_NullOptionalFields_SkipsLovValidations() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            ChargeCreateDTO dtoNullOptionals = ChargeCreateDTO.builder()
                    .chargeCode("TEST001")
                    .chargeName("Test Charge")
                    .divisionCode("DIV001")
                    .taxPoid(null)
                    .chargeGroupPoid(null)
                    .shFfChargeMap(null)
                    .active("Y")
                    .build();

            when(chargeRepository.existsByChargeCodeAndDivisionCode("TEST001", "DIV001")).thenReturn(false);
            when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode("Test Charge", "DIV001")).thenReturn(false);
            when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(validLov);
            doNothing().when(mapper).mapCreateDTOToEntity(eq(dtoNullOptionals), any(ShipChargeMaster.class), eq(100L), eq(123L));
            when(chargeRepository.save(any(ShipChargeMaster.class))).thenReturn(entity);
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            ChargeDto result = service.createCharge(dtoNullOptionals, 100L, 123L);

            assertNotNull(result);
            verify(lovService, never()).getDetailsByPoidAndLovName(any(), eq("TAX_MASTER"));
            verify(lovService, never()).getDetailsByPoidAndLovName(any(), eq("CHARGE_GROUP_MASTER"));
            verify(lovService, never()).getDetailsByPoidAndLovName(any(), eq("CHARGE_MASTER_FF"));
        }
    }

    // ─── updateCharge ────────────────────────────────────────────────────────

    @Test
    void updateCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
                    "Updated Charge", "DIV001", 1L)).thenReturn(false);
            when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
            when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(validLov);
            doNothing().when(mapper).mapUpdateDTOToEntity(eq(updateDto), any(ShipChargeMaster.class), eq(100L), eq(123L));
            when(chargeRepository.save(any(ShipChargeMaster.class))).thenReturn(entity);
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            doNothing().when(loggingService).logChanges(any(), any(), eq(ShipChargeMaster.class), anyString(), anyString(), any(LogDetailsEnum.class), anyString());

            ChargeDto result = service.updateCharge(1L, updateDto, 100L, 123L);

            assertNotNull(result);
            verify(chargeRepository).save(any(ShipChargeMaster.class));
            verify(loggingService).logChanges(any(), any(), eq(ShipChargeMaster.class), eq("DOC001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("CHARGE_POID"));
        }
    }

    @Test
    void updateCharge_NotFound_ThrowsResourceNotFoundException() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void updateCharge_DuplicateName_ThrowsValidationException() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
                "Updated Charge", "DIV001", 1L)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void updateCharge_InvalidTaxMaster_ThrowsValidationException() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
                "Updated Charge", "DIV001", 1L)).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void updateCharge_InvalidDivision_ThrowsValidationException() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
                "Updated Charge", "DIV001", 1L)).thenReturn(false);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
        when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    // ─── getCharge ───────────────────────────────────────────────────────────

    @Test
    void getCharge_Success_NoLines() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(mapper.mapToDto(entity)).thenReturn(responseDto);
        when(shippingChargeLineViewRepository.findByChargePoid(1L)).thenReturn(List.of());

        ChargeDto result = service.getCharge(1L);

        assertNotNull(result);
        assertEquals(1L, result.getChargePoid());
        assertTrue(result.getShippingChargeLineResponseDtoList().isEmpty());
        verify(chargeRepository).findByChargePoid(1L);
    }

    @Test
    void getCharge_Success_WithLines() {
        ShippingChargeLineViewEntity lineEntity = mock(ShippingChargeLineViewEntity.class);
        when(lineEntity.getChargePoid()).thenReturn(1L);
        when(lineEntity.getLinePoid()).thenReturn(10L);
        when(lineEntity.getLineName()).thenReturn("Test Line");
        when(lineEntity.getLineCode()).thenReturn("TL001");

        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(mapper.mapToDto(entity)).thenReturn(responseDto);
        when(shippingChargeLineViewRepository.findByChargePoid(1L)).thenReturn(List.of(lineEntity));

        ChargeDto result = service.getCharge(1L);

        assertNotNull(result);
        assertNotNull(result.getShippingChargeLineResponseDtoList());
        assertEquals(1, result.getShippingChargeLineResponseDtoList().size());
        assertEquals(10L, result.getShippingChargeLineResponseDtoList().get(0).getLinePoid());
        assertEquals("Test Line", result.getShippingChargeLineResponseDtoList().get(0).getLineName());
        assertEquals("TL001", result.getShippingChargeLineResponseDtoList().get(0).getLineCode());
    }

    @Test
    void getCharge_NotFound_ThrowsResourceNotFoundException() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCharge(1L));
    }

    // ─── deleteCharge ────────────────────────────────────────────────────────

    @Test
    void deleteCharge_Success() {
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(documentDeleteService.deleteDocument(
                eq(1L), eq("SHIP_CHARGE_MASTER"), eq("CHARGE_POID"),
                eq(deleteReasonDto), isNull())).thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.deleteCharge(1L, deleteReasonDto));

        verify(documentDeleteService).deleteDocument(1L, "SHIP_CHARGE_MASTER", "CHARGE_POID", deleteReasonDto, null);
    }

    @Test
    void deleteCharge_NotFound_ThrowsResourceNotFoundException() {
        when(chargeRepository.findByChargePoid(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteCharge(999L, null));
        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }

    @Test
    void deleteCharge_NullReason_Success() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(documentDeleteService.deleteDocument(
                eq(1L), eq("SHIP_CHARGE_MASTER"), eq("CHARGE_POID"), isNull(), isNull())).thenReturn("SUCCESS");

        assertDoesNotThrow(() -> service.deleteCharge(1L, null));

        verify(documentDeleteService).deleteDocument(1L, "SHIP_CHARGE_MASTER", "CHARGE_POID", null, null);
    }

    // ─── searchCharges ───────────────────────────────────────────────────────

    @Test
    void searchCharges_Success() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(
                List.of(Map.of("CHARGE_NAME", "Test Charge")),
                Map.of("CHARGE_NAME", "Charge Name"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.searchCharges("DOC001", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("DOC001"), anyList(), eq("AND"),
                eq(pageable), eq("N"), eq("CHARGE_NAME"), eq("CHARGE_POID"));
    }

    @Test
    void searchCharges_EmptyResult() {
        FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(filterRequest)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(),
                any(Pageable.class), anyString(), anyString(), anyString()))
                .thenReturn(rawResult);

        Map<String, Object> result = service.searchCharges("DOC001", filterRequest, pageable);

        assertNotNull(result);
    }
}
