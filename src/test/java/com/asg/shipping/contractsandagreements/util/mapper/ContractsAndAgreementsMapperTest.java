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

import java.time.LocalDate;
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
    void testMapToExportDto_RenewalDueDateMatchesExpiry_WhenNoticePeriodNullOrZero() {
        LocalDate expiry = LocalDate.of(2027, 1, 1);
        LocalDate storedRenewalDue = LocalDate.of(2026, 12, 1);

        AdminContractsAgreementHdr nullNoticeHdr = new AdminContractsAgreementHdr();
        nullNoticeHdr.setTransactionPoid(100L);
        nullNoticeHdr.setNoticePeriodDays(null);
        nullNoticeHdr.setExpiryDate(expiry);
        nullNoticeHdr.setRenewalDueDate(storedRenewalDue);

        AdminContractsAgreementHdrDto nullNoticeDto = ContractsAndAgreementsMapper.mapToExportDto(
                nullNoticeHdr, null, null);
        assertEquals(expiry, nullNoticeDto.getExpiryDate());
        assertEquals(expiry, nullNoticeDto.getRenewalDueDate());

        AdminContractsAgreementHdr zeroNoticeHdr = new AdminContractsAgreementHdr();
        zeroNoticeHdr.setTransactionPoid(101L);
        zeroNoticeHdr.setNoticePeriodDays(0);
        zeroNoticeHdr.setExpiryDate(expiry);
        zeroNoticeHdr.setRenewalDueDate(storedRenewalDue);

        AdminContractsAgreementHdrDto zeroNoticeDto = ContractsAndAgreementsMapper.mapToExportDto(
                zeroNoticeHdr, null, null);
        assertEquals(expiry, zeroNoticeDto.getExpiryDate());
        assertEquals(expiry, zeroNoticeDto.getRenewalDueDate());
    }

    @Test
    void testMapToExportDto_KeepsRenewalDueDate_WhenNoticePeriodPositive() {
        LocalDate expiry = LocalDate.of(2027, 1, 1);
        LocalDate renewalDue = LocalDate.of(2026, 12, 1);

        AdminContractsAgreementHdr hdr = new AdminContractsAgreementHdr();
        hdr.setTransactionPoid(100L);
        hdr.setNoticePeriodDays(30);
        hdr.setExpiryDate(expiry);
        hdr.setRenewalDueDate(renewalDue);

        AdminContractsAgreementHdrDto dto = ContractsAndAgreementsMapper.mapToExportDto(hdr, null, null);

        assertEquals(expiry, dto.getExpiryDate());
        assertEquals(renewalDue, dto.getRenewalDueDate());
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
    void testUpdateHdrEntity_SetsRenewalDueDateToExpiry_WhenNoticePeriodNullOrZero() {
        LocalDate expiry = LocalDate.of(2027, 1, 1);

        AdminContractsAgreementHdrDto nullNoticeDto = new AdminContractsAgreementHdrDto();
        nullNoticeDto.setExpiryDate(expiry);
        nullNoticeDto.setNoticePeriodDays(null);
        nullNoticeDto.setRenewalDueDate(LocalDate.of(2026, 12, 1));
        AdminContractsAgreementHdr nullNoticeEntity = new AdminContractsAgreementHdr();

        ContractsAndAgreementsMapper.updateHdrEntity(nullNoticeDto, nullNoticeEntity);
        assertEquals(expiry, nullNoticeEntity.getExpiryDate());
        assertEquals(expiry, nullNoticeEntity.getRenewalDueDate());

        AdminContractsAgreementHdrDto zeroNoticeDto = new AdminContractsAgreementHdrDto();
        zeroNoticeDto.setExpiryDate(expiry);
        zeroNoticeDto.setNoticePeriodDays(0);
        zeroNoticeDto.setRenewalDueDate(LocalDate.of(2026, 12, 1));
        AdminContractsAgreementHdr zeroNoticeEntity = new AdminContractsAgreementHdr();

        ContractsAndAgreementsMapper.updateHdrEntity(zeroNoticeDto, zeroNoticeEntity);
        assertEquals(expiry, zeroNoticeEntity.getExpiryDate());
        assertEquals(expiry, zeroNoticeEntity.getRenewalDueDate());
    }

    @Test
    void testUpdateHdrEntity_SavesProvidedRenewalDueDate_WhenNoticePeriodPositive() {
        LocalDate expiry = LocalDate.of(2027, 1, 1);
        LocalDate renewalDue = LocalDate.of(2026, 12, 1);

        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setExpiryDate(expiry);
        dto.setNoticePeriodDays(30);
        dto.setRenewalDueDate(renewalDue);
        AdminContractsAgreementHdr entity = new AdminContractsAgreementHdr();

        ContractsAndAgreementsMapper.updateHdrEntity(dto, entity);
        assertEquals(expiry, entity.getExpiryDate());
        assertEquals(renewalDue, entity.getRenewalDueDate());
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
