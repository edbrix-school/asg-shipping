package com.asg.shipping.shipcommisiontransferTest.util;

import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionDetailDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferCreateDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferUpdateDTO;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import com.asg.shipping.shipcommisiontransfer.util.ShipCommissionTransferMapper;
import com.asg.common.lib.service.LovDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShipCommissionTransferMapperTest {

    @Mock
    private LovDataService lovService;

    @InjectMocks
    private ShipCommissionTransferMapper mapper;

    private ShipBlCommissionHdr hdrEntity;
    private ShipBlCommissionDtl dtlEntity;
    private ShipCommissionTransferCreateDTO createDTO;
    private ShipCommissionTransferUpdateDTO updateDTO;
    private ShipCommissionDetailDto detailDto;

    @BeforeEach
    void setup() {
        hdrEntity = ShipBlCommissionHdr.builder()
                .transactionPoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .docRef("COM-001")
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .remarks("Test remarks")
                .deleted("N")
                .currencyExchange(BigDecimal.ONE)
                .currencyCode("USD")
                .fdaTransactionPoid(200L)
                .build();

        dtlEntity = ShipBlCommissionDtl.builder()
                .transactionPoid(1L)
                .detRowId(1L)
                .description("Test detail")
                .blTransactionPoid(300L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .quantity20(BigDecimal.valueOf(2))
                .quantity40(BigDecimal.valueOf(1))
                .buyPercharge(BigDecimal.valueOf(100))
                .sellAmount(BigDecimal.valueOf(1000))
                .commissionAmt(BigDecimal.valueOf(50))
                .freightType("PREPAID")
                .selected("Y")
                .commitionOnAmount(BigDecimal.valueOf(1000))
                .buyPerchargeFrt(BigDecimal.valueOf(50))
                .sellAmountFrt(BigDecimal.valueOf(500))
                .drilldownLinkInfo("LINK-001")
                .blType("IMPORT")
                .blStatus("ACTIVE")
                .thcAmount(BigDecimal.valueOf(25))
                .commissionHandAmt(BigDecimal.valueOf(10))
                .commissionAdjAmt(BigDecimal.valueOf(5))
                .shortLegSelected("N")
                .build();

        createDTO = ShipCommissionTransferCreateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .remarks("Test remarks")
                .currencyExchange(BigDecimal.ONE)
                .currencyCode("USD")
                .fdaTransactionPoid(200L)
                .commissionDetails(List.of())
                .build();

        updateDTO = ShipCommissionTransferUpdateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .remarks("Updated remarks")
                .currencyExchange(BigDecimal.valueOf(1.5))
                .currencyCode("EUR")
                .fdaTransactionPoid(250L)
                .commissionDetails(List.of())
                .build();

        detailDto = ShipCommissionDetailDto.builder()
                .detRowId(1L)
                .description("Test detail")
                .blTransactionPoid(300L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .quantity20(BigDecimal.valueOf(2))
                .quantity40(BigDecimal.valueOf(1))
                .buyPercharge(BigDecimal.valueOf(100))
                .sellAmount(BigDecimal.valueOf(1000))
                .commissionAmt(BigDecimal.valueOf(50))
                .freightType("PREPAID")
                .selected("Y")
                .commitionOnAmount(BigDecimal.valueOf(1000))
                .buyPerchargeFrt(BigDecimal.valueOf(50))
                .sellAmountFrt(BigDecimal.valueOf(500))
                .drilldownLinkInfo("LINK-001")
                .blType("IMPORT")
                .blStatus("ACTIVE")
                .thcAmount(BigDecimal.valueOf(25))
                .commissionHandAmt(BigDecimal.valueOf(10))
                .commissionAdjAmt(BigDecimal.valueOf(5))
                .shortLegSelected("N")
                .build();
    }

    @Test
    @Disabled
    void testMapToDto_Success() {
        var result = mapper.mapToDto(hdrEntity);

        assertNotNull(result);
        assertEquals(hdrEntity.getTransactionPoid(), result.getTransactionPoid());
        assertEquals(hdrEntity.getGroupPoid(), result.getGroupPoid());
        assertEquals(hdrEntity.getCompanyPoid(), result.getCompanyPoid());
        assertEquals(hdrEntity.getDocRef(), result.getDocRef());
        assertEquals(hdrEntity.getTransactionDate(), result.getTransactionDate());
        assertEquals(hdrEntity.getVoyageTransactionPoid(), result.getVoyageTransactionPoid());
        assertEquals(hdrEntity.getRemarks(), result.getRemarks());
        assertEquals(hdrEntity.getDeleted(), result.getDeleted());
        assertEquals(hdrEntity.getCurrencyExchange(), result.getCurrencyExchange());
        assertEquals(hdrEntity.getCurrencyCode(), result.getCurrencyCode());
        assertEquals(hdrEntity.getFdaTransactionPoid(), result.getFdaTransactionPoid());
        assertEquals(hdrEntity.getCreatedBy(), result.getCreatedBy());
        assertEquals(hdrEntity.getCreatedDate(), result.getCreatedDate());
        assertEquals(hdrEntity.getLastModifiedBy(), result.getLastModifiedBy());
        assertEquals(hdrEntity.getLastModifiedDate(), result.getLastModifiedDate());
    }

    @Test
    void testMapToDto_NullEntity() {
        var result = mapper.mapToDto(null);
        assertNull(result);
    }

    @Test
    void testMapCreateDTOToEntity_Success() {
        ShipBlCommissionHdr entity = new ShipBlCommissionHdr();
        
        mapper.mapCreateDTOToEntity(createDTO, entity, 10L, 20L);

        assertEquals(10L, entity.getGroupPoid());
        assertEquals(20L, entity.getCompanyPoid());
        assertEquals(createDTO.getVoyageTransactionPoid(), entity.getVoyageTransactionPoid());
        assertEquals(createDTO.getRemarks(), entity.getRemarks());
        assertEquals(createDTO.getCurrencyExchange(), entity.getCurrencyExchange());
        assertEquals(createDTO.getCurrencyCode(), entity.getCurrencyCode());
        assertEquals(createDTO.getFdaTransactionPoid(), entity.getFdaTransactionPoid());
        assertEquals("N", entity.getDeleted());
    }

    @Test
    void testMapCreateDTOToEntity_NullTransactionDate() {
        ShipBlCommissionHdr entity = new ShipBlCommissionHdr();
        createDTO.setTransactionDate(null);
        
        mapper.mapCreateDTOToEntity(createDTO, entity, 10L, 20L);

        assertNotNull(entity.getTransactionDate());
        assertEquals("N", entity.getDeleted());
    }

    @Test
    void testMapUpdateDTOToEntity_Success() {
        mapper.mapUpdateDTOToEntity(updateDTO, hdrEntity);

        assertEquals(updateDTO.getTransactionDate(), hdrEntity.getTransactionDate());
        assertEquals(updateDTO.getVoyageTransactionPoid(), hdrEntity.getVoyageTransactionPoid());
        assertEquals(updateDTO.getRemarks(), hdrEntity.getRemarks());
        assertEquals(updateDTO.getCurrencyExchange(), hdrEntity.getCurrencyExchange());
        assertEquals(updateDTO.getCurrencyCode(), hdrEntity.getCurrencyCode());
        assertEquals(updateDTO.getFdaTransactionPoid(), hdrEntity.getFdaTransactionPoid());
    }

    @Test
    void testMapUpdateDTOToEntity_NullValues() {
        updateDTO.setTransactionDate(null);
        updateDTO.setVoyageTransactionPoid(null);
        updateDTO.setRemarks(null);
        updateDTO.setCurrencyExchange(null);
        updateDTO.setCurrencyCode(null);
        updateDTO.setFdaTransactionPoid(null);

        LocalDate originalDate = hdrEntity.getTransactionDate();
        String originalRemarks = hdrEntity.getRemarks();

        mapper.mapUpdateDTOToEntity(updateDTO, hdrEntity);

        assertEquals(originalDate, hdrEntity.getTransactionDate());
        assertEquals(originalRemarks, hdrEntity.getRemarks());
    }

    @Test
    @Disabled
    void testMapDtlToDto_Success() {
        var result = mapper.mapDtlToDto(dtlEntity);

        assertNotNull(result);
        assertEquals(dtlEntity.getDetRowId(), result.getDetRowId());
        assertEquals(dtlEntity.getDescription(), result.getDescription());
        assertEquals(dtlEntity.getBlTransactionPoid(), result.getBlTransactionPoid());
        assertEquals(dtlEntity.getCurrencyCode(), result.getCurrencyCode());
        assertEquals(dtlEntity.getCurrencyExchange(), result.getCurrencyExchange());
        assertEquals(dtlEntity.getQuantity20(), result.getQuantity20());
        assertEquals(dtlEntity.getQuantity40(), result.getQuantity40());
        assertEquals(dtlEntity.getBuyPercharge(), result.getBuyPercharge());
        assertEquals(dtlEntity.getSellAmount(), result.getSellAmount());
        assertEquals(dtlEntity.getCommissionAmt(), result.getCommissionAmt());
        assertEquals(dtlEntity.getFreightType(), result.getFreightType());
        assertEquals(dtlEntity.getSelected(), result.getSelected());
        assertEquals(dtlEntity.getCommitionOnAmount(), result.getCommitionOnAmount());
        assertEquals(dtlEntity.getBuyPerchargeFrt(), result.getBuyPerchargeFrt());
        assertEquals(dtlEntity.getSellAmountFrt(), result.getSellAmountFrt());
        assertEquals(dtlEntity.getDrilldownLinkInfo(), result.getDrilldownLinkInfo());
        assertEquals(dtlEntity.getBlType(), result.getBlType());
        assertEquals(dtlEntity.getBlStatus(), result.getBlStatus());
        assertEquals(dtlEntity.getThcAmount(), result.getThcAmount());
        assertEquals(dtlEntity.getCommissionHandAmt(), result.getCommissionHandAmt());
        assertEquals(dtlEntity.getCommissionAdjAmt(), result.getCommissionAdjAmt());
        assertEquals(dtlEntity.getShortLegSelected(), result.getShortLegSelected());
    }

    @Test
    void testMapDtlToDto_NullEntity() {
        var result = mapper.mapDtlToDto(null);
        assertNull(result);
    }

    @Test
    void testMapDtlFromDto_Success() {
        var result = mapper.mapDtlFromDto(detailDto, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        assertEquals(detailDto.getDetRowId(), result.getDetRowId());
        assertEquals(detailDto.getDescription(), result.getDescription());
        assertEquals(detailDto.getBlTransactionPoid(), result.getBlTransactionPoid());
        assertEquals(detailDto.getCurrencyCode(), result.getCurrencyCode());
        assertEquals(detailDto.getCurrencyExchange(), result.getCurrencyExchange());
        assertEquals(detailDto.getQuantity20(), result.getQuantity20());
        assertEquals(detailDto.getQuantity40(), result.getQuantity40());
        assertEquals(detailDto.getBuyPercharge(), result.getBuyPercharge());
        assertEquals(detailDto.getSellAmount(), result.getSellAmount());
        assertEquals(detailDto.getCommissionAmt(), result.getCommissionAmt());
        assertEquals(detailDto.getFreightType(), result.getFreightType());
        assertEquals(detailDto.getSelected(), result.getSelected());
        assertEquals(detailDto.getCommitionOnAmount(), result.getCommitionOnAmount());
        assertEquals(detailDto.getBuyPerchargeFrt(), result.getBuyPerchargeFrt());
        assertEquals(detailDto.getSellAmountFrt(), result.getSellAmountFrt());
        assertEquals(detailDto.getDrilldownLinkInfo(), result.getDrilldownLinkInfo());
        assertEquals(detailDto.getBlType(), result.getBlType());
        assertEquals(detailDto.getBlStatus(), result.getBlStatus());
        assertEquals(detailDto.getThcAmount(), result.getThcAmount());
        assertEquals(detailDto.getCommissionHandAmt(), result.getCommissionHandAmt());
        assertEquals(detailDto.getCommissionAdjAmt(), result.getCommissionAdjAmt());
        assertEquals(detailDto.getShortLegSelected(), result.getShortLegSelected());
    }

    @Test
    void testMapDtlFromDto_NullDto() {
        var result = mapper.mapDtlFromDto(null, 1L);
        assertNull(result);
    }

    @Test
    @Disabled
    void testMapDtlListToDto_Success() {
        List<ShipBlCommissionDtl> entities = List.of(dtlEntity);
        
        var result = mapper.mapDtlListToDto(entities);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dtlEntity.getDetRowId(), result.get(0).getDetRowId());
        assertEquals(dtlEntity.getDescription(), result.get(0).getDescription());
    }

    @Test
    void testMapDtlListToDto_NullList() {
        var result = mapper.mapDtlListToDto(null);
        assertNull(result);
    }

    @Test
    void testMapDtlListToDto_EmptyList() {
        var result = mapper.mapDtlListToDto(List.of());
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}