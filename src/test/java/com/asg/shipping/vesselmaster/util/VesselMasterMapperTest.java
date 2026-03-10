package com.asg.shipping.vesselmaster.util;

import com.asg.shipping.vesselmaster.dto.VesselMasterCreateDTO;
import com.asg.shipping.vesselmaster.dto.VesselMasterDto;
import com.asg.shipping.vesselmaster.dto.VesselMasterUpdateDTO;
import com.asg.shipping.vesselmaster.entity.ShipVesselMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class VesselMasterMapperTest {

    private VesselMasterMapper mapper;
    private ShipVesselMaster vessel;
    private VesselMasterCreateDTO createDTO;
    private VesselMasterUpdateDTO updateDTO;
    private static final Long GROUP_POID = 100L;
    private static final Long USER_POID = 200L;
    private static final Long COMPANY_POID = 300L;

    @BeforeEach
    void setUp() {
        mapper = new VesselMasterMapper();

        vessel = ShipVesselMaster.builder()
                .vesselPoid(1L)
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
    void mapToDto_Success() {
        VesselMasterDto dto = mapper.mapToDto(vessel);

        assertNotNull(dto);
        assertEquals(vessel.getVesselPoid(), dto.getVesselPoid());
        assertEquals(vessel.getGroupPoid(), dto.getGroupPoid());
        assertEquals(vessel.getVesselCode(), dto.getVesselCode());
        assertEquals(vessel.getVesselName(), dto.getVesselName());
        assertEquals(vessel.getVesselName2(), dto.getVesselName2());
        assertEquals(vessel.getLinePoid(), dto.getLinePoid());
        assertEquals(vessel.getOwner(), dto.getOwner());
        assertEquals(vessel.getAgentPoid(), dto.getAgentPoid());
        assertEquals(vessel.getRegistrationNo(), dto.getRegistrationNo());
        assertEquals(vessel.getRegistrationDate(), dto.getRegistrationDate());
        assertEquals(vessel.getCountryOfRegistration(), dto.getCountryOfRegistration());
        assertEquals(vessel.getFlagOfCountry(), dto.getFlagOfCountry());
        assertEquals(vessel.getVesselTypePoid(), dto.getVesselTypePoid());
        assertEquals(vessel.getVesselTypeClass(), dto.getVesselTypeClass());
        assertEquals(vessel.getGrt(), dto.getGrt());
        assertEquals(vessel.getNrt(), dto.getNrt());
        assertEquals(vessel.getDwt(), dto.getDwt());
        assertEquals(vessel.getVesselLength(), dto.getVesselLength());
        assertEquals(vessel.getBeam(), dto.getBeam());
        assertEquals(vessel.getDraft(), dto.getDraft());
        assertEquals(vessel.getHatches(), dto.getHatches());
        assertEquals(vessel.getBayhatch(), dto.getBayhatch());
        assertEquals(vessel.getImoNumber(), dto.getImoNumber());
        assertEquals(vessel.getRemarks(), dto.getRemarks());
        assertEquals(vessel.getLineName(), dto.getLineName());
        assertEquals(vessel.getActive(), dto.getActive());
        assertEquals(vessel.getSeqno(), dto.getSeqno());
        assertEquals(vessel.getCreatedBy(), dto.getCreatedBy());
        assertEquals(vessel.getCreatedDate(), dto.getCreatedDate());
        assertEquals(vessel.getLastModifiedBy(), dto.getLastModifiedBy());
        assertEquals(vessel.getLastModifiedDate(), dto.getLastModifiedDate());
        assertEquals(vessel.getDeleted(), dto.getDeleted());
    }

    @Test
    void mapToDto_NullEntity_ReturnsNull() {
        VesselMasterDto dto = mapper.mapToDto(null);
        assertNull(dto);
    }

    @Test
    void mapToDto_WithNullFields_Success() {
        vessel.setVesselName2(null);
        vessel.setLinePoid(null);
        vessel.setOwner(null);
        vessel.setAgentPoid(null);
        vessel.setRegistrationNo(null);
        vessel.setRegistrationDate(null);
        vessel.setCountryOfRegistration(null);
        vessel.setFlagOfCountry(null);
        vessel.setVesselTypePoid(null);
        vessel.setVesselTypeClass(null);
        vessel.setGrt(null);
        vessel.setNrt(null);
        vessel.setDwt(null);
        vessel.setVesselLength(null);
        vessel.setBeam(null);
        vessel.setDraft(null);
        vessel.setHatches(null);
        vessel.setBayhatch(null);
        vessel.setImoNumber(null);
        vessel.setRemarks(null);
        vessel.setLineName(null);
        vessel.setSeqno(null);
        vessel.setCreatedBy(null);
        vessel.setCreatedDate(null);
        vessel.setLastModifiedBy(null);
        vessel.setLastModifiedDate(null);
        vessel.setDeleted(null);

        VesselMasterDto dto = mapper.mapToDto(vessel);

        assertNotNull(dto);
        assertNull(dto.getVesselName2());
        assertNull(dto.getLinePoid());
        assertNull(dto.getOwner());
    }

    @Test
    void mapCreateDTOToEntity_Success() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            ShipVesselMaster entity = new ShipVesselMaster();
            mapper.mapCreateDTOToEntity(createDTO, entity, GROUP_POID, USER_POID, COMPANY_POID);

            assertEquals(GROUP_POID, entity.getGroupPoid());
            assertEquals(createDTO.getVesselCode(), entity.getVesselCode());
            assertEquals(createDTO.getVesselName(), entity.getVesselName());
            assertEquals(createDTO.getVesselName2(), entity.getVesselName2());
            assertEquals(createDTO.getLinePoid(), entity.getLinePoid());
            assertEquals(createDTO.getOwner(), entity.getOwner());
            assertEquals(createDTO.getAgentPoid(), entity.getAgentPoid());
            assertEquals(createDTO.getRegistrationNo(), entity.getRegistrationNo());
            assertEquals(createDTO.getRegistrationDate(), entity.getRegistrationDate());
            assertEquals(createDTO.getCountryOfRegistration(), entity.getCountryOfRegistration());
            assertEquals(createDTO.getFlagOfCountry(), entity.getFlagOfCountry());
            assertEquals(createDTO.getVesselTypePoid(), entity.getVesselTypePoid());
            assertEquals(createDTO.getVesselTypeClass(), entity.getVesselTypeClass());
            assertEquals(createDTO.getGrt(), entity.getGrt());
            assertEquals(createDTO.getNrt(), entity.getNrt());
            assertEquals(createDTO.getDwt(), entity.getDwt());
            assertEquals(createDTO.getVesselLength(), entity.getVesselLength());
            assertEquals(createDTO.getBeam(), entity.getBeam());
            assertEquals(createDTO.getDraft(), entity.getDraft());
            assertEquals(createDTO.getHatches(), entity.getHatches());
            assertEquals(createDTO.getBayhatch(), entity.getBayhatch());
            assertEquals(createDTO.getImoNumber(), entity.getImoNumber());
            assertEquals(createDTO.getRemarks(), entity.getRemarks());
            assertEquals(createDTO.getActive(), entity.getActive());
            assertEquals(createDTO.getSeqno(), entity.getSeqno());
            assertEquals("N", entity.getDeleted());
        }
    }

    @Test
    void mapCreateDTOToEntity_WithNullActive_DefaultsToY() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            createDTO.setActive(null);
            ShipVesselMaster entity = new ShipVesselMaster();
            mapper.mapCreateDTOToEntity(createDTO, entity, GROUP_POID, USER_POID, COMPANY_POID);

            assertEquals("Y", entity.getActive());
        }
    }

    @Test
    void mapCreateDTOToEntity_WithEmptyActive_DefaultsToY() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            createDTO.setActive("");
            ShipVesselMaster entity = new ShipVesselMaster();
            mapper.mapCreateDTOToEntity(createDTO, entity, GROUP_POID, USER_POID, COMPANY_POID);

            assertEquals("Y", entity.getActive());
        }
    }

    @Test
    void mapCreateDTOToEntity_WithNullFields_Success() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            createDTO.setVesselName2(null);
            createDTO.setLinePoid(null);
            createDTO.setOwner(null);
            createDTO.setAgentPoid(null);
            createDTO.setRegistrationNo(null);
            createDTO.setRegistrationDate(null);
            createDTO.setCountryOfRegistration(null);
            createDTO.setFlagOfCountry(null);
            createDTO.setVesselTypePoid(null);
            createDTO.setVesselTypeClass(null);
            createDTO.setGrt(null);
            createDTO.setNrt(null);
            createDTO.setDwt(null);
            createDTO.setVesselLength(null);
            createDTO.setBeam(null);
            createDTO.setDraft(null);
            createDTO.setHatches(null);
            createDTO.setBayhatch(null);
            createDTO.setImoNumber(null);
            createDTO.setRemarks(null);
            createDTO.setSeqno(null);

            ShipVesselMaster entity = new ShipVesselMaster();
            mapper.mapCreateDTOToEntity(createDTO, entity, GROUP_POID, USER_POID, COMPANY_POID);

            assertNull(entity.getVesselName2());
            assertNull(entity.getLinePoid());
            assertNull(entity.getOwner());
        }
    }

    @Test
    void mapUpdateDTOToEntity_Success() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            String originalVesselCode = vessel.getVesselCode();
            LocalDateTime originalCreatedDate = vessel.getCreatedDate();
            String originalCreatedBy = vessel.getCreatedBy();

            mapper.mapUpdateDTOToEntity(updateDTO, vessel, GROUP_POID, USER_POID, COMPANY_POID);

            // VESSEL_CODE should not be updated
            assertEquals(originalVesselCode, vessel.getVesselCode());
            assertEquals(GROUP_POID, vessel.getGroupPoid());
            assertEquals(updateDTO.getVesselName(), vessel.getVesselName());
            assertEquals(updateDTO.getVesselName2(), vessel.getVesselName2());
            assertEquals(updateDTO.getLinePoid(), vessel.getLinePoid());
            assertEquals(updateDTO.getOwner(), vessel.getOwner());
            assertEquals(updateDTO.getAgentPoid(), vessel.getAgentPoid());
            assertEquals(updateDTO.getRegistrationNo(), vessel.getRegistrationNo());
            assertEquals(updateDTO.getRegistrationDate(), vessel.getRegistrationDate());
            assertEquals(updateDTO.getCountryOfRegistration(), vessel.getCountryOfRegistration());
            assertEquals(updateDTO.getFlagOfCountry(), vessel.getFlagOfCountry());
            assertEquals(updateDTO.getVesselTypePoid(), vessel.getVesselTypePoid());
            assertEquals(updateDTO.getVesselTypeClass(), vessel.getVesselTypeClass());
            assertEquals(updateDTO.getGrt(), vessel.getGrt());
            assertEquals(updateDTO.getNrt(), vessel.getNrt());
            assertEquals(updateDTO.getDwt(), vessel.getDwt());
            assertEquals(updateDTO.getVesselLength(), vessel.getVesselLength());
            assertEquals(updateDTO.getBeam(), vessel.getBeam());
            assertEquals(updateDTO.getDraft(), vessel.getDraft());
            assertEquals(updateDTO.getHatches(), vessel.getHatches());
            assertEquals(updateDTO.getBayhatch(), vessel.getBayhatch());
            assertEquals(updateDTO.getImoNumber(), vessel.getImoNumber());
            assertEquals(updateDTO.getRemarks(), vessel.getRemarks());
            assertEquals(updateDTO.getActive(), vessel.getActive());
            assertEquals(updateDTO.getSeqno(), vessel.getSeqno());
            // Created fields should not be updated
        }
    }

    @Test
    void mapUpdateDTOToEntity_WithNullActive_DoesNotUpdate() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            vessel.setActive("Y");
            updateDTO.setActive(null);
            mapper.mapUpdateDTOToEntity(updateDTO, vessel, GROUP_POID, USER_POID, COMPANY_POID);

            assertEquals("Y", vessel.getActive());
        }
    }

    @Test
    void mapUpdateDTOToEntity_WithEmptyActive_DoesNotUpdate() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            vessel.setActive("Y");
            updateDTO.setActive("");
            mapper.mapUpdateDTOToEntity(updateDTO, vessel, GROUP_POID, USER_POID, COMPANY_POID);

            assertEquals("Y", vessel.getActive());
        }
    }

    @Test
    void mapUpdateDTOToEntity_WithNullFields_Success() {
        try (MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedUtils = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUtils.when(com.asg.common.lib.utility.ASGHelperUtils::getCurrentUser).thenReturn("testuser");

            updateDTO.setVesselName2(null);
            updateDTO.setLinePoid(null);
            updateDTO.setOwner(null);
            updateDTO.setAgentPoid(null);
            updateDTO.setRegistrationNo(null);
            updateDTO.setRegistrationDate(null);
            updateDTO.setCountryOfRegistration(null);
            updateDTO.setFlagOfCountry(null);
            updateDTO.setVesselTypePoid(null);
            updateDTO.setVesselTypeClass(null);
            updateDTO.setGrt(null);
            updateDTO.setNrt(null);
            updateDTO.setDwt(null);
            updateDTO.setVesselLength(null);
            updateDTO.setBeam(null);
            updateDTO.setDraft(null);
            updateDTO.setHatches(null);
            updateDTO.setBayhatch(null);
            updateDTO.setImoNumber(null);
            updateDTO.setRemarks(null);
            updateDTO.setSeqno(null);

            mapper.mapUpdateDTOToEntity(updateDTO, vessel, GROUP_POID, USER_POID, COMPANY_POID);

            assertNull(vessel.getVesselName2());
            assertNull(vessel.getLinePoid());
            assertNull(vessel.getOwner());
        }
    }
}

