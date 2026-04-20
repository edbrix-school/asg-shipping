package com.asg.shipping.daycloseshiping.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.daycloseshiping.dto.DayCloseDenominationDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseHdrDto;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseHdr;

class DayCloseMapperTest {

    /* ---------------- MAP CREATE DTO TO ENTITY ---------------- */

    @Test
    void mapCreateDTOToEntity_allFieldsProvided() {
        DayCloseHdrDto dto = DayCloseHdrDto.builder()
                .transactionDate(LocalDate.of(2024, 1, 15))
                .locationCode("WAREHOUSE")
                .cashAmount(BigDecimal.valueOf(1000))
                .chequeAmount(BigDecimal.valueOf(500))
                .outstandingAmount(BigDecimal.valueOf(200))
                .totalAmount(BigDecimal.valueOf(1500))
                .noOfCheques(5L)
                .locRemarks("Test remarks")
                .verifiedRcvd("Y")
                .mainOfcRemarks("Main office remarks")
                .docRef("DOC123")
                .build();

        ArShDayEndCloseHdr entity = new ArShDayEndCloseHdr();
        DayCloseMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);

        assertEquals(1L, entity.getGroupPoid());
        assertEquals(2L, entity.getCompanyPoid());
        assertEquals(LocalDate.of(2024, 1, 15), entity.getTransactionDate());
        assertEquals("WAREHOUSE", entity.getLocationCode());
        assertEquals(BigDecimal.valueOf(1000), entity.getCashAmount());
        assertEquals(BigDecimal.valueOf(500), entity.getChequeAmount());
        assertEquals(BigDecimal.valueOf(200), entity.getOutstandingAmount());
        assertEquals(BigDecimal.valueOf(1500), entity.getTotalAmount());
        assertEquals(5L, entity.getNoOfCheques());
        assertEquals("Test remarks", entity.getLocRemarks());
        assertEquals("Y", entity.getVerifiedRcvd());
        assertEquals("Main office remarks", entity.getMainOfcRemarks());
        assertEquals("DOC123", entity.getDocRef());
        assertEquals("N", entity.getDeleted());
    }

    @Test
    void mapCreateDTOToEntity_nullTransactionDate_usesCurrentDate() {
        LocalDate currentDate = LocalDate.of(2024, 1, 20);

        try (MockedStatic<DateUtil> mockedDateUtil = Mockito.mockStatic(DateUtil.class)) {
            mockedDateUtil.when(DateUtil::getCurrentDateInUserTimeZone).thenReturn(currentDate);

            DayCloseHdrDto dto = DayCloseHdrDto.builder()
                    .transactionDate(null)
                    .build();

            ArShDayEndCloseHdr entity = new ArShDayEndCloseHdr();
            DayCloseMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);

            assertEquals(currentDate, entity.getTransactionDate());
        }
    }

    @Test
    void mapCreateDTOToEntity_nullLocationCode_usesPORT() {
        DayCloseHdrDto dto = DayCloseHdrDto.builder()
                .locationCode(null)
                .build();

        ArShDayEndCloseHdr entity = new ArShDayEndCloseHdr();
        DayCloseMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);

        assertEquals("PORT", entity.getLocationCode());
    }

    @Test
    void mapCreateDTOToEntity_nullVerifiedRcvd_usesN() {
        DayCloseHdrDto dto = DayCloseHdrDto.builder()
                .verifiedRcvd(null)
                .build();

        ArShDayEndCloseHdr entity = new ArShDayEndCloseHdr();
        DayCloseMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);

        assertEquals("N", entity.getVerifiedRcvd());
    }

    @Test
    void mapCreateDTOToEntity_nullMainOfcRemarks_usesDot() {
        DayCloseHdrDto dto = DayCloseHdrDto.builder()
                .mainOfcRemarks(null)
                .build();

        ArShDayEndCloseHdr entity = new ArShDayEndCloseHdr();
        DayCloseMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);

        assertEquals(".", entity.getMainOfcRemarks());
    }

    /* ---------------- MAP ENTITY TO DTO ---------------- */

    @Test
    void mapToDto_allFields() {
        ArShDayEndCloseHdr entity = ArShDayEndCloseHdr.builder()
                .transactionPoid(100L)
                .groupPoid(1L)
                .companyPoid(2L)
                .docRef("DOC456")
                .transactionDate(LocalDate.of(2024, 2, 10))
                .locationCode("STORE")
                .cashAmount(BigDecimal.valueOf(2000))
                .chequeAmount(BigDecimal.valueOf(1000))
                .outstandingAmount(BigDecimal.valueOf(300))
                .totalAmount(BigDecimal.valueOf(3000))
                .noOfCheques(10L)
                .locRemarks("Location remarks")
                .verifiedRcvd("Y")
                .mainOfcRemarks("Office remarks")
                .build();
        entity.setCreatedBy("user123");
        entity.setCreatedDate(LocalDateTime.of(2024, 2, 10, 10, 30));

        DayCloseDto result = DayCloseMapper.mapToDto(entity);

        assertNotNull(result);
        assertNotNull(result.getHeader());
        assertEquals(100L, result.getHeader().getTransactionPoid());
        assertEquals(1L, result.getHeader().getGroupPoid());
        assertEquals(2L, result.getHeader().getCompanyPoid());
        assertEquals("DOC456", result.getHeader().getDocRef());
        assertEquals(LocalDate.of(2024, 2, 10), result.getHeader().getTransactionDate());
        assertEquals("STORE", result.getHeader().getLocationCode());
        assertEquals(BigDecimal.valueOf(2000), result.getHeader().getCashAmount());
        assertEquals(BigDecimal.valueOf(1000), result.getHeader().getChequeAmount());
        assertEquals(BigDecimal.valueOf(300), result.getHeader().getOutstandingAmount());
        assertEquals(BigDecimal.valueOf(3000), result.getHeader().getTotalAmount());
        assertEquals(10L, result.getHeader().getNoOfCheques());
        assertEquals("Location remarks", result.getHeader().getLocRemarks());
        assertEquals("Y", result.getHeader().getVerifiedRcvd());
        assertEquals("Office remarks", result.getHeader().getMainOfcRemarks());
        assertEquals("user123", result.getHeader().getCreatedBy());
        assertEquals(LocalDateTime.of(2024, 2, 10, 10, 30), result.getHeader().getCreatedDate());
    }

    @Test
    void mapToDto_nullEntity_returnsNull() {
        DayCloseDto result = DayCloseMapper.mapToDto(null);
        assertNull(result);
    }

    /* ---------------- MAP DETAIL TO DTO ---------------- */

    @Test
    void mapDtlToDto_allFields() {
        ArShDayEndCloseDtl entity = ArShDayEndCloseDtl.builder()
                .detRowId(5L)
                .currencyAmount(BigDecimal.valueOf(100))
                .currencyType("NOTE")
                .noOfTran(10L)
                .cashAmount(BigDecimal.valueOf(1000))
                .build();

        DayCloseDenominationDto result = DayCloseMapper.mapDtlToDto(entity);

        assertNotNull(result);
        assertEquals(5L, result.getDetRowId());
        assertEquals(BigDecimal.valueOf(100), result.getDenomination());
        assertEquals("NOTE", result.getCurrencyType());
        assertEquals(10L, result.getNoOfTran());
        assertEquals(BigDecimal.valueOf(1000), result.getCashAmount());
    }

    @Test
    void mapDtlToDto_nullEntity_returnsNull() {
        DayCloseDenominationDto result = DayCloseMapper.mapDtlToDto(null);
        assertNull(result);
    }

    /* ---------------- MAP DTO TO DETAIL ENTITY ---------------- */

    @Test
    void mapDtlFromDto_withCashAmount() {
        DayCloseDenominationDto dto = DayCloseDenominationDto.builder()
                .denomination(BigDecimal.valueOf(50))
                .currencyType("COIN")
                .noOfTran(20L)
                .cashAmount(BigDecimal.valueOf(1500))
                .build();

        ArShDayEndCloseDtl result = DayCloseMapper.mapDtlFromDto(dto, 100L, 10L);

        assertNotNull(result);
        assertEquals(100L, result.getTransactionPoid());
        assertEquals(10L, result.getDetRowId());
        assertEquals(BigDecimal.valueOf(50), result.getCurrencyAmount());
        assertEquals("COIN", result.getCurrencyType());
        assertEquals(20L, result.getNoOfTran());
        assertEquals(BigDecimal.valueOf(1500), result.getCashAmount());
    }

    @Test
    void mapDtlFromDto_nullCashAmount_calculatesFromDenomination() {
        DayCloseDenominationDto dto = DayCloseDenominationDto.builder()
                .denomination(BigDecimal.valueOf(100))
                .currencyType("NOTE")
                .noOfTran(5L)
                .cashAmount(null)
                .build();

        ArShDayEndCloseDtl result = DayCloseMapper.mapDtlFromDto(dto, 200L, 20L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(500), result.getCashAmount());
    }

    @Test
    void mapDtlFromDto_nullNoOfTran_cashAmountIsNull() {
        DayCloseDenominationDto dto = DayCloseDenominationDto.builder()
                .denomination(BigDecimal.valueOf(100))
                .currencyType("NOTE")
                .noOfTran(null)
                .cashAmount(null)
                .build();

        ArShDayEndCloseDtl result = DayCloseMapper.mapDtlFromDto(dto, 200L, 20L);

        assertNotNull(result);
        assertNull(result.getCashAmount());
    }

    @Test
    void mapDtlFromDto_nullDenomination_cashAmountIsNull() {
        DayCloseDenominationDto dto = DayCloseDenominationDto.builder()
                .denomination(null)
                .currencyType("NOTE")
                .noOfTran(5L)
                .cashAmount(null)
                .build();

        ArShDayEndCloseDtl result = DayCloseMapper.mapDtlFromDto(dto, 200L, 20L);

        assertNotNull(result);
        assertNull(result.getCashAmount());
    }

    @Test
    void mapDtlFromDto_nullDto_returnsNull() {
        ArShDayEndCloseDtl result = DayCloseMapper.mapDtlFromDto(null, 100L, 10L);
        assertNull(result);
    }

    /* ---------------- MAP DETAIL LIST TO DTO ---------------- */

    @Test
    void mapDtlListToDto_multipleEntities() {
        ArShDayEndCloseDtl entity1 = ArShDayEndCloseDtl.builder()
                .detRowId(1L)
                .currencyAmount(BigDecimal.valueOf(100))
                .currencyType("NOTE")
                .noOfTran(5L)
                .cashAmount(BigDecimal.valueOf(500))
                .build();

        ArShDayEndCloseDtl entity2 = ArShDayEndCloseDtl.builder()
                .detRowId(2L)
                .currencyAmount(BigDecimal.valueOf(50))
                .currencyType("COIN")
                .noOfTran(10L)
                .cashAmount(BigDecimal.valueOf(500))
                .build();

        List<ArShDayEndCloseDtl> entities = Arrays.asList(entity1, entity2);

        List<DayCloseDenominationDto> result = DayCloseMapper.mapDtlListToDto(entities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getDetRowId());
        assertEquals(BigDecimal.valueOf(100), result.get(0).getDenomination());
        assertEquals(2L, result.get(1).getDetRowId());
        assertEquals(BigDecimal.valueOf(50), result.get(1).getDenomination());
    }

    @Test
    void mapDtlListToDto_emptyList() {
        List<DayCloseDenominationDto> result = DayCloseMapper.mapDtlListToDto(Arrays.asList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void mapDtlListToDto_nullList_returnsNull() {
        List<DayCloseDenominationDto> result = DayCloseMapper.mapDtlListToDto(null);
        assertNull(result);
    }
}
