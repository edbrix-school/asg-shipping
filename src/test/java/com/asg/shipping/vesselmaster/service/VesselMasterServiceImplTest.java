package com.asg.shipping.vesselmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.vesselmaster.dto.VesselMasterCreateDTO;
import com.asg.shipping.vesselmaster.dto.VesselMasterDto;
import com.asg.shipping.vesselmaster.dto.VesselMasterUpdateDTO;
import com.asg.shipping.vesselmaster.entity.ShipVesselMaster;
import com.asg.shipping.vesselmaster.repository.ShipVesselMasterRepository;
import com.asg.shipping.vesselmaster.util.VesselMasterMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.asg.common.lib.security.util.UserContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VesselMasterServiceImplTest {

    @Mock
    private ShipVesselMasterRepository vesselRepository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private VesselMasterLovService vesselMasterLovService;

    @Mock
    private VesselMasterMapper mapper;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private VesselMasterServiceImpl vesselService;

    private ShipVesselMaster vessel;
    private VesselMasterDto vesselDto;
    private VesselMasterCreateDTO createDTO;
    private VesselMasterUpdateDTO updateDTO;
    private static final Long GROUP_POID = 100L;
    private static final Long USER_POID = 200L;
    private static final Long COMPANY_POID = 300L;
    private static final Long VESSEL_POID = 1L;

    @BeforeEach
    void setUp() {
        vessel = ShipVesselMaster.builder()
                .vesselPoid(VESSEL_POID)
                .groupPoid(GROUP_POID)
                .vesselCode("VSL001")
                .vesselName("Test Vessel")
                .vesselName2("Test Vessel 2")
                .linePoid(10L)
                .owner("Test Owner")
                .agentPoid(20L)
                .registrationNo("REG123")
                .registrationDate(LocalDate.of(2020, 1, 1))
                .countryOfRegistration("Panama")
                .flagOfCountry("Panama")
                .vesselTypePoid(30L)
                .vesselTypeClass("CLASS1")
                .grt(new BigDecimal("50000.00"))
                .nrt(new BigDecimal("30000.00"))
                .dwt(new BigDecimal("60000.00"))
                .vesselLength(new BigDecimal("300.50"))
                .beam(new BigDecimal("40.20"))
                .draft(new BigDecimal("12.50"))
                .hatches(5)
                .bayhatch(10)
                .imoNumber("IMO1234567")
                .remarks("Test Remarks")
                .lineName("Test Line")
                .active("Y")
                .seqno(1)
                .deleted("N")
                .build();

        vesselDto = VesselMasterDto.builder()
                .vesselPoid(VESSEL_POID)
                .groupPoid(GROUP_POID)
                .vesselCode("VSL001")
                .vesselName("Test Vessel")
                .build();

        createDTO = VesselMasterCreateDTO.builder()
                .vesselCode("VSL002")
                .vesselName("New Vessel")
                .vesselName2("New Vessel 2")
                .linePoid(10L)
                .owner("New Owner")
                .agentPoid(20L)
                .registrationNo("REG456")
                .registrationDate(LocalDate.of(2021, 1, 1))
                .countryOfRegistration("Liberia")
                .flagOfCountry("Liberia")
                .vesselTypePoid(30L)
                .vesselTypeClass("CLASS2")
                .grt(new BigDecimal("60000.00"))
                .nrt(new BigDecimal("40000.00"))
                .dwt(new BigDecimal("70000.00"))
                .vesselLength(new BigDecimal("350.50"))
                .beam(new BigDecimal("45.20"))
                .draft(new BigDecimal("13.50"))
                .hatches(6)
                .bayhatch(12)
                .imoNumber("IMO7654321")
                .remarks("New Remarks")
                .active("Y")
                .seqno(2)
                .build();

        updateDTO = VesselMasterUpdateDTO.builder()
                .vesselName("Updated Vessel")
                .vesselName2("Updated Vessel 2")
                .linePoid(11L)
                .owner("Updated Owner")
                .agentPoid(21L)
                .registrationNo("REG789")
                .registrationDate(LocalDate.of(2022, 1, 1))
                .countryOfRegistration("Marshall Islands")
                .flagOfCountry("Marshall Islands")
                .vesselTypePoid(31L)
                .vesselTypeClass("CLASS3")
                .grt(new BigDecimal("70000.00"))
                .nrt(new BigDecimal("50000.00"))
                .dwt(new BigDecimal("80000.00"))
                .vesselLength(new BigDecimal("400.50"))
                .beam(new BigDecimal("50.20"))
                .draft(new BigDecimal("14.50"))
                .hatches(7)
                .bayhatch(14)
                .imoNumber("IMO9876543")
                .remarks("Updated Remarks")
                .active("N")
                .seqno(3)
                .build();
    }

    @Test
    void searchVessels_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            FilterRequestDto request = new FilterRequestDto("AND", "N", new ArrayList<>());
            Pageable pageable = PageRequest.of(0, 20);
            List<FilterDto> filters = new ArrayList<>();
            List<Map<String, Object>> records = List.of(Map.of("VESSEL_POID", 1L, "VESSEL_NAME", "Test Vessel"));
            Map<String, String> displayFields = Map.of("VESSEL_POID", "Vessel ID", "VESSEL_NAME", "Vessel Name");
            RawSearchResult rawResult = new RawSearchResult(records, displayFields, 1L);

            when(documentSearchService.resolveOperator(request)).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(request)).thenReturn("N");
            when(documentSearchService.resolveFilters(request)).thenReturn(filters);
            when(documentSearchService.search(eq("100-008"), eq(filters), eq("AND"), eq(pageable), eq("N"),
                    eq("VESSEL_NAME"), eq("VESSEL_POID"))).thenReturn(rawResult);

            Map<String, Object> result = vesselService.searchVessels(request, pageable);

            assertNotNull(result);
            verify(documentSearchService).resolveOperator(request);
            verify(documentSearchService).resolveIsDeleted(request);
            verify(documentSearchService).resolveFilters(request);
            verify(documentSearchService).search(eq("100-008"), eq(filters), eq("AND"), eq(pageable), eq("N"),
                    eq("VESSEL_NAME"), eq("VESSEL_POID"));
        }
    }

    @Test
    void getVessel_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(10L)).thenReturn(List.of(createLovItem(10L, "LINE001", "Test Line")));
            when(vesselMasterLovService.getVesselTypeLov(30L)).thenReturn(List.of(createLovItem(30L, "VT001", "Test Type")));
            when(vesselMasterLovService.getAgentMasterLov(20L)).thenReturn(List.of(createLovItem(20L, "AG001", "Test Agent")));

            VesselMasterDto result = vesselService.getVessel(VESSEL_POID);

            assertNotNull(result);
            assertEquals(VESSEL_POID, result.getVesselPoid());
            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(mapper).mapToDto(vessel);
        }
    }

    @Test
    void getVessel_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselService.getVessel(VESSEL_POID));
            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(mapper, never()).mapToDto(any());
        }
    }

    @Test
    void getVessel_WithNullLinePoid_NoLovEnrichment() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            vessel.setLinePoid(null);
            vessel.setVesselTypePoid(null);
            vessel.setAgentPoid(null);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);

            VesselMasterDto result = vesselService.getVessel(VESSEL_POID);

            assertNotNull(result);
            verify(vesselMasterLovService, never()).getLineMasterLov(any());
            verify(vesselMasterLovService, never()).getVesselTypeLov(any());
            verify(vesselMasterLovService, never()).getAgentMasterLov(any());
        }
    }

    @Test
    void getVessel_WithEmptyLovList_NoEnrichment() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(10L)).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getVesselTypeLov(30L)).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getAgentMasterLov(20L)).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.getVessel(VESSEL_POID);

            assertNotNull(result);
            verify(vesselMasterLovService).getLineMasterLov(10L);
            verify(vesselMasterLovService).getVesselTypeLov(30L);
            verify(vesselMasterLovService).getAgentMasterLov(20L);
        }
    }

    @Test
    void getVessel_WithLovException_ContinuesGracefully() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            lenient().when(vesselMasterLovService.getLineMasterLov(10L)).thenThrow(new RuntimeException("LOV Error"));
            lenient().when(vesselMasterLovService.getVesselTypeLov(30L)).thenReturn(List.of(createLovItem(30L, "VT001", "Test Type")));

            VesselMasterDto result = vesselService.getVessel(VESSEL_POID);

            assertNotNull(result);
            verify(vesselMasterLovService).getLineMasterLov(10L);
        }
    }

    @Test
    void getVessel_WithAgentLovException_ContinuesGracefully() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(10L)).thenReturn(List.of(createLovItem(10L, "LINE001", "Test Line")));
            when(vesselMasterLovService.getVesselTypeLov(30L)).thenReturn(List.of(createLovItem(30L, "VT001", "Test Type")));
            when(vesselMasterLovService.getAgentMasterLov(20L)).thenThrow(new RuntimeException("Agent LOV Error"));

            VesselMasterDto result = vesselService.getVessel(VESSEL_POID);

            assertNotNull(result);
            verify(vesselMasterLovService).getAgentMasterLov(20L);
        }
    }

    @Test
    void createVessel_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            ShipVesselMaster savedVessel = ShipVesselMaster.builder()
                    .vesselPoid(2L)
                    .vesselCode("VSL002")
                    .vesselName("New Vessel")
                    .build();

            when(vesselRepository.existsByImoNumberAndGroupPoid("IMO7654321", GROUP_POID)).thenReturn(false);
            when(vesselRepository.save(any(ShipVesselMaster.class))).thenReturn(savedVessel);
            when(mapper.mapToDto(savedVessel)).thenReturn(vesselDto);
            lenient().when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.createVessel(createDTO);

            assertNotNull(result);
            verify(vesselRepository).existsByImoNumberAndGroupPoid("IMO7654321", GROUP_POID);
            verify(vesselRepository).save(any(ShipVesselMaster.class));
            verify(mapper).mapCreateDTOToEntity(eq(createDTO), any(ShipVesselMaster.class), eq(GROUP_POID), eq(USER_POID), eq(COMPANY_POID));
            verify(mapper).mapToDto(savedVessel);
        }
    }

    @Test
    void createVessel_WithNullImoNumber_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            createDTO.setImoNumber(null);
            ShipVesselMaster savedVessel = ShipVesselMaster.builder()
                    .vesselPoid(2L)
                    .vesselCode("VSL002")
                    .build();

            when(vesselRepository.save(any(ShipVesselMaster.class))).thenReturn(savedVessel);
            when(mapper.mapToDto(savedVessel)).thenReturn(vesselDto);
            lenient().when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.createVessel(createDTO);

            assertNotNull(result);
            verify(vesselRepository, never()).existsByImoNumberAndGroupPoid(anyString(), anyLong());
            verify(vesselRepository).save(any(ShipVesselMaster.class));
        }
    }

    @Test
    void createVessel_WithEmptyImoNumber_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            createDTO.setImoNumber("   ");
            ShipVesselMaster savedVessel = ShipVesselMaster.builder()
                    .vesselPoid(2L)
                    .vesselCode("VSL002")
                    .build();

            when(vesselRepository.save(any(ShipVesselMaster.class))).thenReturn(savedVessel);
            when(mapper.mapToDto(savedVessel)).thenReturn(vesselDto);
            lenient().when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            lenient().when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.createVessel(createDTO);

            assertNotNull(result);
            verify(vesselRepository, never()).existsByImoNumberAndGroupPoid(anyString(), anyLong());
        }
    }

    @Test
    void createVessel_WithDuplicateImoNumber_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.existsByImoNumberAndGroupPoid("IMO7654321", GROUP_POID)).thenReturn(true);

            assertThrows(ValidationException.class, () -> vesselService.createVessel(createDTO));
            verify(vesselRepository).existsByImoNumberAndGroupPoid("IMO7654321", GROUP_POID);
            verify(vesselRepository, never()).save(any());
        }
    }

    @Test
    void updateVessel_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.existsByImoNumberAndGroupPoidExcluding("IMO9876543", GROUP_POID, VESSEL_POID))
                    .thenReturn(false);
            when(vesselRepository.save(vessel)).thenReturn(vessel);
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.updateVessel(VESSEL_POID, updateDTO);

            assertNotNull(result);
            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(vesselRepository).existsByImoNumberAndGroupPoidExcluding("IMO9876543", GROUP_POID, VESSEL_POID);
            verify(vesselRepository).save(vessel);
            verify(mapper).mapUpdateDTOToEntity(eq(updateDTO), eq(vessel), eq(GROUP_POID), eq(USER_POID), eq(COMPANY_POID));
            verify(mapper).mapToDto(vessel);
        }
    }

    @Test
    void updateVessel_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselService.updateVessel(VESSEL_POID, updateDTO));
            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(vesselRepository, never()).save(any());
        }
    }

    @Test
    void updateVessel_WithDuplicateImoNumber_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.existsByImoNumberAndGroupPoidExcluding("IMO9876543", GROUP_POID, VESSEL_POID))
                    .thenReturn(true);

            assertThrows(ValidationException.class, () -> vesselService.updateVessel(VESSEL_POID, updateDTO));
            verify(vesselRepository).existsByImoNumberAndGroupPoidExcluding("IMO9876543", GROUP_POID, VESSEL_POID);
            verify(vesselRepository, never()).save(any());
        }
    }

    @Test
    void updateVessel_WithNullImoNumber_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            updateDTO.setImoNumber(null);
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.save(vessel)).thenReturn(vessel);
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.updateVessel(VESSEL_POID, updateDTO);

            assertNotNull(result);
            verify(vesselRepository, never()).existsByImoNumberAndGroupPoidExcluding(anyString(), anyLong(), anyLong());
        }
    }

    @Test
    void updateVessel_WithEmptyImoNumber_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);

            updateDTO.setImoNumber("   ");
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.save(vessel)).thenReturn(vessel);
            when(mapper.mapToDto(vessel)).thenReturn(vesselDto);
            when(vesselMasterLovService.getLineMasterLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getVesselTypeLov(any())).thenReturn(new ArrayList<>());
            when(vesselMasterLovService.getAgentMasterLov(any())).thenReturn(new ArrayList<>());

            VesselMasterDto result = vesselService.updateVessel(VESSEL_POID, updateDTO);

            assertNotNull(result);
            verify(vesselRepository, never()).existsByImoNumberAndGroupPoidExcluding(anyString(), anyLong(), anyLong());
        }
    }

    @Test
    void toggleActive_FromYToN_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            vessel.setActive("Y");
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.save(vessel)).thenReturn(vessel);

            vesselService.toggleActive(VESSEL_POID);

            assertEquals("N", vessel.getActive());
            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(vesselRepository).save(vessel);
        }
    }

    @Test
    void toggleActive_FromNToY_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            vessel.setActive("N");
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.save(vessel)).thenReturn(vessel);

            vesselService.toggleActive(VESSEL_POID);

            assertEquals("Y", vessel.getActive());
            verify(vesselRepository).save(vessel);
        }
    }

    @Test
    void toggleActive_FromNullToY_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            vessel.setActive(null);
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(vesselRepository.save(vessel)).thenReturn(vessel);

            vesselService.toggleActive(VESSEL_POID);

            assertEquals("Y", vessel.getActive());
            verify(vesselRepository).save(vessel);
        }
    }

    @Test
    void toggleActive_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselService.toggleActive(VESSEL_POID));
            verify(vesselRepository, never()).save(any());
        }
    }

    @Test
    void deleteVessel_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            deleteReasonDto.setDeleteReason("Test deletion");
            
            vessel.setDeleted("N");
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));
            when(documentDeleteService.deleteDocument(anyLong(), anyString(), anyString(), any(), any())).thenReturn("Success");

            vesselService.deleteVessel(VESSEL_POID, deleteReasonDto);

            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(documentDeleteService).deleteDocument(VESSEL_POID, "SHIP_VESSEL_MASTER", "VESSEL_POID", deleteReasonDto, null);
        }
    }

    @Test
    void deleteVessel_AlreadyDeleted_NoAction() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            vessel.setDeleted("Y");
            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.of(vessel));

            vesselService.deleteVessel(VESSEL_POID, null);

            verify(vesselRepository).findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID);
            verify(documentDeleteService, never()).deleteDocument(anyLong(), anyString(), anyString(), any(), any());
        }
    }

    @Test
    void deleteVessel_NotFound_ThrowsException() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);

            when(vesselRepository.findByVesselPoidAndGroupPoid(VESSEL_POID, GROUP_POID))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> vesselService.deleteVessel(VESSEL_POID, null));
            verify(documentDeleteService, never()).deleteDocument(anyLong(), anyString(), anyString(), any(), any());
        }
    }

    private LovItem createLovItem(Long poid, String code, String description) {
        LovItem item = new LovItem();
        item.setPoid(poid);
        item.setCode(code);
        item.setDescription(description);
        return item;
    }
}

