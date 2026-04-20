package com.asg.shipping.contractsandagreements.util.mapper;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementPicDtlDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class ContractsAndAgreementsMapperTest {

    private MockedStatic<UserContext> userContextMockedStatic;

    @BeforeEach
    void setUp() {
        userContextMockedStatic = mockStatic(UserContext.class);
        userContextMockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
        userContextMockedStatic.when(UserContext::getGroupPoid).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        userContextMockedStatic.close();
    }

    @Test
    void testMapToExportDto() {
        AdminContractsAgreementHdr hdr = new AdminContractsAgreementHdr();
        hdr.setTransactionPoid(100L);
        hdr.setPartyAddress("Address");
        
        AdminContractsAgreementPicDtl pic = new AdminContractsAgreementPicDtl();
        pic.setId(new AdminContractsAgreementDtlId(100L, 1L));
        pic.setRemarks("rem");

        AdminContractsAgreementRenewalEntity ren = new AdminContractsAgreementRenewalEntity();
        ren.setId(new AdminContractsAgreementDtlId(100L, 2L));

        AdminContractsAgreementHdrDto dto = ContractsAndAgreementsMapper.mapToExportDto(
                hdr, List.of(pic), List.of(ren));

        assertNotNull(dto);
        assertEquals(100L, dto.getTransactionPoid());
        assertEquals("Address", dto.getPartyAddress());
        assertEquals(1, dto.getAgreementContentDetails().size());
        assertEquals("rem", dto.getAgreementContentDetails().get(0).getRemarks());
        assertEquals(1, dto.getRenewalDetails().size());
    }

    @Test
    void testMapToExportDto_NullLists() {
        AdminContractsAgreementHdr hdr = new AdminContractsAgreementHdr();
        hdr.setTransactionPoid(100L);

        AdminContractsAgreementHdrDto dto = ContractsAndAgreementsMapper.mapToExportDto(
                hdr, null, null);

        assertNotNull(dto);
        assertTrue(dto.getAgreementContentDetails().isEmpty());
        assertTrue(dto.getRenewalDetails().isEmpty());
    }

    @Test
    void testUpdateHdrEntity() {
        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setAgreementName("AgName");
        AdminContractsAgreementHdr entity = new AdminContractsAgreementHdr();

        ContractsAndAgreementsMapper.updateHdrEntity(dto, entity);
        assertEquals(1L, entity.getCompanyPoid());
        assertEquals("AgName", entity.getAgreementName());
        assertEquals("N", entity.getDeleted());
    }
    
    @Test
    void testUpdateHdrEntity_Null() {
        assertDoesNotThrow(() -> ContractsAndAgreementsMapper.updateHdrEntity(null, null));
    }

    @Test
    void testUpdatePicDtlEntity() {
        AdminContractsAgreementPicDtlDto dto = new AdminContractsAgreementPicDtlDto();
        dto.setRemarks("test");
        AdminContractsAgreementPicDtl entity = new AdminContractsAgreementPicDtl();

        ContractsAndAgreementsMapper.updatePicDtlEntity(dto, entity);
        assertEquals("test", entity.getRemarks());
        
        assertDoesNotThrow(() -> ContractsAndAgreementsMapper.updatePicDtlEntity(null, null));
    }

    @Test
    void testUpdateRenewalDtlEntity() {
        AdminContractsAgreementRenewalDto dto = new AdminContractsAgreementRenewalDto();
        java.time.LocalDate now = java.time.LocalDate.now();
        dto.setRenewalDate(now);
        AdminContractsAgreementRenewalEntity entity = new AdminContractsAgreementRenewalEntity();

        ContractsAndAgreementsMapper.updateRenewalDtlEntity(dto, entity);
        assertEquals(now, entity.getRenewalDate());
        
        assertDoesNotThrow(() -> ContractsAndAgreementsMapper.updateRenewalDtlEntity(null, null));
    }

    @Test
    void testMapRenewalDtlDto() {
        AdminContractsAgreementRenewalDto dto = new AdminContractsAgreementRenewalDto();
        AdminContractsAgreementRenewalEntity entity = ContractsAndAgreementsMapper.mapRenewalDtlDto(dto);
        assertNotNull(entity);
        assertEquals("N", entity.getDeleted());
        assertNull(ContractsAndAgreementsMapper.mapRenewalDtlDto(null));
    }

    @Test
    void testMapPicDtlDtoToEntity() {
        AdminContractsAgreementPicDtlDto dto = new AdminContractsAgreementPicDtlDto();
        AdminContractsAgreementPicDtl entity = ContractsAndAgreementsMapper.mapPicDtlDtoToEntity(dto);
        assertNotNull(entity);
        assertNull(ContractsAndAgreementsMapper.mapPicDtlDtoToEntity(null));
    }
}
