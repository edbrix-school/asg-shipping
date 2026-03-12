package com.asg.shipping.ffChargeMaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ShippingChargeLineResponseDto;
import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import com.asg.shipping.shippingFFChargeMaster.entity.ShippingChargeLineViewEntity;
import com.asg.shipping.shippingFFChargeMaster.repository.ShipChargeMasterRepository;
import com.asg.shipping.shippingFFChargeMaster.repository.ShippingChargeLineViewRepository;
import com.asg.shipping.shippingFFChargeMaster.service.ShippingFFChargeMasterServiceImpl;
import com.asg.shipping.shippingFFChargeMaster.util.ChargeMasterMapper;
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
    }

    @Test
    void createCharge_DuplicateCode() {
        when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                .thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void createCharge_DuplicateName() {
        when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                .thenReturn(false);
        when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                .thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }


    @Test
    void updateCharge_NotFound() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void getCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            when(shippingChargeLineViewRepository.findByChargePoid(1L)).thenReturn(List.of());
            doNothing().when(loggingService)
                    .createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());


            ChargeDto result = service.getCharge(1L);

            assertNotNull(result);
            assertEquals(1L, result.getChargePoid());
            verify(chargeRepository).findByChargePoid(1L);
        }
    }

    @Test
    void getCharge_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getCharge(1L));
        }
    }

    @Test
    void deleteCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(chargeRepository.save(entity)).thenReturn(entity);
            doNothing().when(loggingService)
                    .createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            service.deleteCharge(1L);

            assertEquals("Y", entity.getDeleted());
            assertEquals("N", entity.getActive());
            verify(chargeRepository).save(entity);
            verify(loggingService, times(2)).createLogDetailsEntry(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void deleteCharge_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);

            entity.setDeleted("Y");
            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));

            service.deleteCharge(1L);

            verify(chargeRepository, never()).save(any());
            verify(loggingService, never()).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
        }
    }

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
    void validateTaxMaster_Invalid() {
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
    }

    @Test
    void createCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            // Mock all validations to pass
            when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                    .thenReturn(false);
            when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                    .thenReturn(false);

            LovGetListDto validLov = new LovGetListDto();
            validLov.setPoid(1L);
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
            verify(chargeRepository).save(any(ShipChargeMaster.class));
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC001", "1");
        }
    }

    @Test
    void updateCharge_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(chargeRepository.existsByChargeNameAndDivisionCodeAndChargePoidNotAndDeletedNot(
                    "Updated Charge", "DIV001", 1L, "Y")).thenReturn(false);

            LovGetListDto validLov = new LovGetListDto();
            validLov.setPoid(1L);
            when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
            when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
            when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(validLov);

            doNothing().when(mapper).mapUpdateDTOToEntity(eq(updateDto), any(ShipChargeMaster.class), eq(100L), eq(123L));
            when(chargeRepository.save(any(ShipChargeMaster.class))).thenReturn(entity);
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
            doNothing().when(loggingService).logChanges(any(), any(), any(Class.class), anyString(), anyString(), any(LogDetailsEnum.class), anyString());

            ChargeDto result = service.updateCharge(1L, updateDto, 100L, 123L);

            assertNotNull(result);
            verify(chargeRepository).save(any(ShipChargeMaster.class));
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.MODIFIED, "DOC001", "1");
            verify(loggingService).logChanges(any(), any(), eq(ShipChargeMaster.class), eq("DOC001"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("CHARGE_POID"));
        }
    }

    @Test
    void updateCharge_DuplicateName() {
        when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
        when(chargeRepository.existsByChargeNameAndDivisionCodeAndChargePoidNotAndDeletedNot(
                "Updated Charge", "DIV001", 1L, "Y")).thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.updateCharge(1L, updateDto, 100L, 123L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void deleteCharge_NotFound() {
        when(chargeRepository.findByChargePoid(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteCharge(999L));
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void getCharge_WithShippingLines() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            // Create a mock entity since the real entity is immutable
            ShippingChargeLineViewEntity lineEntity = mock(ShippingChargeLineViewEntity.class);
            when(lineEntity.getChargePoid()).thenReturn(1L);
            when(lineEntity.getLinePoid()).thenReturn(10L);
            when(lineEntity.getLineName()).thenReturn("Test Line");
            when(lineEntity.getLineCode()).thenReturn("TL001");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            when(shippingChargeLineViewRepository.findByChargePoid(1L)).thenReturn(List.of(lineEntity));
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            ChargeDto result = service.getCharge(1L);

            assertNotNull(result);
            assertNotNull(result.getShippingChargeLineResponseDtoList());
            assertEquals(1, result.getShippingChargeLineResponseDtoList().size());
            assertEquals(10L, result.getShippingChargeLineResponseDtoList().get(0).getLinePoid());
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.VIEWED, "DOC001", "1");
        }
    }

    @Test
    void validateChargeGroupMaster_Invalid() {
        when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                .thenReturn(false);
        when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                .thenReturn(false);

        LovGetListDto validTax = new LovGetListDto();
        validTax.setPoid(1L);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validTax);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
    }

    @Test
    void validateChargeMasterFF_Invalid() {
        when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                .thenReturn(false);
        when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                .thenReturn(false);

        LovGetListDto validLov = new LovGetListDto();
        validLov.setPoid(1L);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
    }

    @Test
    void validateDivision_Invalid() {
        when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                .thenReturn(false);
        when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                .thenReturn(false);

        LovGetListDto validLov = new LovGetListDto();
        validLov.setPoid(1L);
        when(lovService.getDetailsByPoidAndLovName(1L, "TAX_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(2L, "CHARGE_GROUP_MASTER")).thenReturn(validLov);
        when(lovService.getDetailsByPoidAndLovName(3L, "CHARGE_MASTER_FF")).thenReturn(validLov);
        when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(null);

        assertThrows(ValidationException.class,
                () -> service.createCharge(createDto, 100L, 123L));
    }

    @Test
    void validateTaxMaster_Null() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");

            ChargeCreateDTO dtoWithoutTax = ChargeCreateDTO.builder()
                    .chargeCode("TEST001")
                    .chargeName("Test Charge")
                    .divisionCode("DIV001")
                    .taxPoid(null)
                    .chargeGroupPoid(null)
                    .shFfChargeMap(null)
                    .active("Y")
                    .build();

            when(chargeRepository.existsByChargeCodeAndDivisionCodeAndDeletedNot("TEST001", "DIV001", "Y"))
                    .thenReturn(false);
            when(chargeRepository.existsByChargeNameAndDivisionCodeAndDeletedNot("Test Charge", "DIV001", "Y"))
                    .thenReturn(false);

            LovGetListDto validLov = new LovGetListDto();
            validLov.setPoid(1L);
            when(lovService.getDetailsByCodeAndLovName("DIV001", "SHIP_DIVISION_PRINT")).thenReturn(validLov);

            doNothing().when(mapper).mapCreateDTOToEntity(eq(dtoWithoutTax), any(ShipChargeMaster.class), eq(100L), eq(123L));
            when(chargeRepository.save(any(ShipChargeMaster.class))).thenReturn(entity);
            when(mapper.mapToDto(entity)).thenReturn(responseDto);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());

            ChargeDto result = service.createCharge(dtoWithoutTax, 100L, 123L);

            assertNotNull(result);
            verify(lovService, never()).getDetailsByPoidAndLovName(any(), eq("TAX_MASTER"));
        }
    }

    @Test
    void deleteCharge_WithLogging() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<ASGHelperUtils> mockedHelper = mockStatic(ASGHelperUtils.class)) {

            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC001");
            mockedHelper.when(ASGHelperUtils::getCurrentUser).thenReturn("testUser");

            when(chargeRepository.findByChargePoid(1L)).thenReturn(Optional.of(entity));
            when(chargeRepository.save(entity)).thenReturn(entity);
            doNothing().when(loggingService).createLogSummaryEntry(any(LogDetailsEnum.class), anyString(), anyString());
            doNothing().when(loggingService).createLogDetailsEntry(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

            service.deleteCharge(1L);

            assertEquals("Y", entity.getDeleted());
            assertEquals("N", entity.getActive());
            assertEquals("testUser", entity.getLastModifiedBy());
            assertNotNull(entity.getLastModifiedDate());
            verify(chargeRepository).save(entity);
            verify(loggingService).createLogSummaryEntry(LogDetailsEnum.DELETED, "DOC001", "1");
            verify(loggingService, times(2)).createLogDetailsEntry(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }
}
