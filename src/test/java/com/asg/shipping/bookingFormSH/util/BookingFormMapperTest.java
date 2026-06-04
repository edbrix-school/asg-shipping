package com.asg.shipping.bookingFormSH.util;

import static org.junit.jupiter.api.Assertions.*;

import com.asg.shipping.bookingFormSH.dto.*;
import com.asg.shipping.bookingFormSH.entity.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

class BookingFormMapperTest {

    // --- mapToDto tests ---

    @Test
    void mapToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapToDto(null));
    }

    @Test
    void mapToDto_valid_mapsAllFields() {
        LocalDate date = LocalDate.now();
        ShipMateHdr entity = ShipMateHdr.builder()
                .transactionPoid(1L).groupPoid(2L).companyPoid(3L).docRef("REF")
                .transactionDate(date).vessalAgentName("Agent")
                .shipperPoid(4L).shipperAddressPoid(5L).consigneePoid(6L).consigneeAddressPoid(7L)
                .notifyPoid1(8L).notifyAddressPoid1(9L).notifyPoid2(10L).notifyAddressPoid2(11L)
                .quotationTransactionPoid(12L).vesselPoid(13L).vesselEtaDate(date)
                .linePoid(14L).salesmanPoid(15L).comodityPoid(16L)
                .totalVolume(1.0).totalWeight(10.0).unitPack("BOX")
                .totalNoOfPacks(0.0).placeOfRecieptPoid(17L).placeOfDelieveryPoid(18L)
                .portOfLoadingPoid(19L).portOfDischargePoid(20L).remarks("Remarks")
                .mateStatus("OPEN").voyageNo("V001").bookingIssueNo("B001")
                .mateLoadDate(date).mateLoadNo(21L).mateLoadVoyagePoid(22L)
                .issueType("FULL").deleted("N").consigneeName("CN").consigneeAddress("CA")
                .splitBookingNo(23L).finalDestination("FD").shipperDetailsManually("SDM")
                .build();

        BookingFormDto dto = BookingFormMapper.mapToDto(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getTransactionPoid());
        assertEquals(2L, dto.getGroupPoid());
        assertEquals("REF", dto.getDocRef());
        assertEquals(date, dto.getTransactionDate());
        assertEquals("Agent", dto.getVessalAgentName());
    }

    // --- mapCreateDTOToEntity tests ---

    @Test
    void mapCreateDTOToEntity_transactionDateNull_setsCurrentDate() {
        BookingFormCreateDTO dto = BookingFormCreateDTO.builder().build();
        ShipMateHdr entity = new ShipMateHdr();
        BookingFormMapper.mapCreateDTOToEntity(dto, entity, 1L, 2L);
        assertNotNull(entity.getTransactionDate());
        assertEquals(1L, entity.getGroupPoid());
        assertEquals(2L, entity.getCompanyPoid());
        assertEquals("N", entity.getDeleted());
    }

    @Test
    void mapCreateDTOToEntity_allFields_mapsCorrectly() {
        LocalDate date = LocalDate.now();
        BookingFormCreateDTO dto = BookingFormCreateDTO.builder()
                .transactionDate(date).vessalAgentName("Agent")
                .shipperPoid(1L).shipperAddressPoid(1L).consigneePoid(1L).consigneeAddressPoid(1L)
                .notifyPoid1(1L).notifyAddressPoid1(1L).notifyPoid2(1L).notifyAddressPoid2(1L)
                .quotationTransactionPoid(1L).vesselPoid(1L).vesselEtaDate(date).linePoid(1L)
                .salesmanPoid(1L).comodityPoid(1L).totalVolume(1.0).totalWeight(1.0)
                .unitPack("BOX").totalNoOfPacks(1.0).placeOfRecieptPoid(1L).placeOfDelieveryPoid(1L)
                .portOfLoadingPoid(1L).portOfDischargePoid(1L).remarks("R").mateStatus("S").voyageNo("V")
                .bookingIssueNo("B").mateLoadDate(date).mateLoadNo(1L).mateLoadVoyagePoid(1L)
                .issueType("T").consigneeName("N").consigneeAddress("A").splitBookingNo(1L)
                .finalDestination("D").shipperDetailsManually("M")
                .build();

        ShipMateHdr entity = new ShipMateHdr();
        BookingFormMapper.mapCreateDTOToEntity(dto, entity, 10L, 20L);

        assertEquals(date, entity.getTransactionDate());
        assertEquals("Agent", entity.getVessalAgentName());
        assertEquals(10L, entity.getGroupPoid());
    }

    // --- mapUpdateDTOToEntity tests ---

    @Test
    void mapUpdateDTOToEntity_allFields_updatesCorrectly() {
        LocalDate date = LocalDate.now();
        BookingFormUpdateDTO dto = BookingFormUpdateDTO.builder()
                .transactionDate(date).vessalAgentName("Agent")
                .shipperPoid(1L).shipperAddressPoid(1L).consigneePoid(1L).consigneeAddressPoid(1L)
                .notifyPoid1(1L).notifyAddressPoid1(1L).notifyPoid2(1L).notifyAddressPoid2(1L)
                .quotationTransactionPoid(1L).vesselPoid(1L).vesselEtaDate(date).linePoid(1L)
                .salesmanPoid(1L).comodityPoid(1L).totalVolume(1.0).totalWeight(1.0)
                .unitPack("BOX").totalNoOfPacks(1.0).placeOfRecieptPoid(1L).placeOfDelieveryPoid(1L)
                .portOfLoadingPoid(1L).portOfDischargePoid(1L).remarks("R").mateStatus("S").voyageNo("V")
                .bookingIssueNo("B").mateLoadDate(date).mateLoadNo(1L).mateLoadVoyagePoid(1L)
                .issueType("T").consigneeName("N").consigneeAddress("A").splitBookingNo(1L)
                .finalDestination("D").shipperDetailsManually("M")
                .build();

        ShipMateHdr entity = new ShipMateHdr();
        BookingFormMapper.mapUpdateDTOToEntity(dto, entity);

        assertEquals("Agent", entity.getVessalAgentName());
    }

    @Test
    void mapUpdateDTOToEntity_nullFields_doesNotUpdate() {
        ShipMateHdr entity = new ShipMateHdr();
        entity.setVessalAgentName("KeepMe");
        BookingFormMapper.mapUpdateDTOToEntity(new BookingFormUpdateDTO(), entity);
        assertEquals("KeepMe", entity.getVessalAgentName());
    }

    // --- Cargo Detail tests ---

    @Test
    void mapCargoDtlFromDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapCargoDtlFromDto(null, 1L));
    }

    @Test
    void mapCargoDtlFromDto_valid_mapsAllFields() {
        BookingFormCargoDetailDto dto = BookingFormCargoDetailDto.builder()
                .detRowId(1L).cargoDescription("Desc").equipmentType("Type").equipmentSize("Size")
                .quantity(BigDecimal.ONE).volume(BigDecimal.ONE).weight(BigDecimal.ONE)
                .equipmentIsoType("ISO").isImco("Y").imo("IMO").isOog("Y").oogL("L").oogB("B").oogH("H")
                .refferTemp("T").refferHum("H").refferVent("V").isRefer("Y").referType("RT")
                .oogLW("LW").oogRW("RW").oogF("F").oogA("A")
                .build();

        ShipMateCargoDtl entity = BookingFormMapper.mapCargoDtlFromDto(dto, 100L);
        assertNotNull(entity);
        assertEquals(100L, entity.getTransactionPoid());
        assertEquals("ISO", entity.getEquipmentIsoType());
        assertEquals("L", entity.getOogL());
    }

    // --- Charges Detail tests ---

    @Test
    void mapChargesDtlToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapChargesDtlToDto(null));
    }

    @Test
    void mapChargesDtlToDto_valid_mapsCorrectly() {
        ShipMateChargesDtl entity = ShipMateChargesDtl.builder()
                .detRowId(1L).chargePoid(2L).currencyExchange(BigDecimal.ONE).quantity(BigDecimal.TEN)
                .perQuantityAmount(BigDecimal.ONE).paidAtPortPoid(3L).buyPercharge(BigDecimal.ONE)
                .currencyCode("USD").build();
        BookingFormChargesDetailDto dto = BookingFormMapper.mapChargesDtlToDto(entity);
        assertNotNull(dto);
        assertEquals(2L, dto.getChargePoid());
    }

    @Test
    void mapChargesDtlFromDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapChargesDtlFromDto(null, 1L));
    }

    @Test
    void mapChargesDtlFromDto_valid_mapsCorrectly() {
        BookingFormChargesDetailDto dto = BookingFormChargesDetailDto.builder().detRowId(1L).chargePoid(2L).build();
        ShipMateChargesDtl entity = BookingFormMapper.mapChargesDtlFromDto(dto, 100L);
        assertNotNull(entity);
        assertEquals(2L, entity.getChargePoid());
    }

    // --- Container Detail tests ---

    @Test
    void mapContainerDtlToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapContainerDtlToDto(null));
    }

    @Test
    void mapContainerDtlToDto_valid_mapsAllFields() {
        LocalDate date = LocalDate.now();
        ShipMateContainerDtl entity = ShipMateContainerDtl.builder()
                .detRowId(1L).containerNo("C1").equipmentSealNo("S1").equipmentIsoType("I").equipmentType("T")
                .equipmentSize("S").quantity(BigDecimal.ONE).grsVolume(BigDecimal.ONE).grsWeight(BigDecimal.ONE)
                .netVolume(BigDecimal.ONE).netWeight(BigDecimal.ONE).noOfPacks(BigDecimal.ONE).packUnit("U")
                .comodityPoid(1L).destinationPortPoid(1L).imo("I").oogL("L").oogB("B").oogH("H")
                .refferTemp("T").refferHum("H").refferVent("V").cargoDescription("D").equipmentShipperOwn("Y")
                .issueToShipper(date).returnFromShipper(date).releaseAllocation("A").isImco("Y").isOog("Y")
                .isRefer("Y").referType("R").oogLW("LW").oogRW("RW").oogF("F").oogA("A").isSplit("N")
                .imcoClassType("C").oogType("O").vgmWeight(BigDecimal.ONE).vgmDocId("V").vgmDate(date).vgmEdi("Y")
                .build();

        BookingFormContainerDetailDto dto = BookingFormMapper.mapContainerDtlToDto(entity);
        assertNotNull(dto);
        assertEquals("C1", dto.getContainerNo());
        assertEquals("L", dto.getOogL());
    }

    @Test
    void mapContainerDtlFromDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapContainerDtlFromDto(null, 1L));
    }

    @Test
    void mapContainerDtlFromDto_containerNoWithSpaces_trimsCorrectly() {
        BookingFormContainerDetailDto dto = BookingFormContainerDetailDto.builder().containerNo(" C 1 ").build();
        ShipMateContainerDtl entity = BookingFormMapper.mapContainerDtlFromDto(dto, 1L);
        assertEquals("C1", entity.getContainerNo());
    }

    @Test
    void mapContainerDtlFromDto_containerNoNull_mapsNull() {
        BookingFormContainerDetailDto dto = BookingFormContainerDetailDto.builder().containerNo(null).build();
        ShipMateContainerDtl entity = BookingFormMapper.mapContainerDtlFromDto(dto, 1L);
        assertNull(entity.getContainerNo());
    }

    @Test
    void mapContainerDtlFromDto_valid_mapsAllFields() {
        LocalDate date = LocalDate.now();
        BookingFormContainerDetailDto dto = BookingFormContainerDetailDto.builder()
                .detRowId(1L).containerNo("C1").equipmentSealNo("S1").equipmentIsoType("I").equipmentType("T")
                .equipmentSize("S").quantity(BigDecimal.ONE).grsVolume(BigDecimal.ONE).grsWeight(BigDecimal.ONE)
                .netVolume(BigDecimal.ONE).netWeight(BigDecimal.ONE).noOfPacks(BigDecimal.ONE).packUnit("U")
                .comodityPoid(1L).destinationPortPoid(1L).imo("I").oogL("L").oogB("B").oogH("H")
                .refferTemp("T").refferHum("H").refferVent("V").cargoDescription("D").equipmentShipperOwn("Y")
                .issueToShipper(date).returnFromShipper(date).releaseAllocation("A").isImco("Y").isOog("Y")
                .isRefer("Y").referType("R").oogLW("LW").oogRW("RW").oogF("F").oogA("A").isSplit("N")
                .imcoClassType("C").oogType("O").vgmWeight(BigDecimal.ONE).vgmDocId("V").vgmDate(date).vgmEdi("Y")
                .build();

        ShipMateContainerDtl entity = BookingFormMapper.mapContainerDtlFromDto(dto, 100L);
        assertNotNull(entity);
        assertEquals(100L, entity.getTransactionPoid());
        assertEquals("L", entity.getOogL());
    }

    // --- List mapping tests ---

    @Test
    void mapCargoDtlListToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapCargoDtlListToDto(null));
    }

    @Test
    void mapCargoDtlListToDto_withNullItem_returnsNullInList() {
        List<BookingFormCargoDetailDto> result = BookingFormMapper.mapCargoDtlListToDto(Arrays.asList((ShipMateCargoDtl)null));
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    void mapChargesDtlListToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapChargesDtlListToDto(null));
    }

    @Test
    void mapChargesDtlListToDto_withNullItem_returnsNullInList() {
        List<BookingFormChargesDetailDto> result = BookingFormMapper.mapChargesDtlListToDto(Arrays.asList((ShipMateChargesDtl)null));
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    void mapContainerDtlListToDto_null_returnsNull() {
        assertNull(BookingFormMapper.mapContainerDtlListToDto(null));
    }

    @Test
    void mapContainerDtlListToDto_withNullItem_returnsNullInList() {
        List<BookingFormContainerDetailDto> result = BookingFormMapper.mapContainerDtlListToDto(Arrays.asList((ShipMateContainerDtl)null));
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }
}
