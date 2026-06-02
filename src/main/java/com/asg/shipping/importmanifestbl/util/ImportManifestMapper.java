package com.asg.shipping.importmanifestbl.util;

import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestupdate.entity.*;

import java.util.List;

public class ImportManifestMapper {
    private ImportManifestMapper() {
    }

    private static final String CARGO_TYPE_DESCRIPTION = "DESC";
    private static final String CARGO_TYPE_MARKS = "MARKS";
    private static final String CONTAINER_OWN_CUSTOMER = "C";
    private static final String CONTAINER_OWN_SHIPPER = "S";

    public static ImportManifestBlDto mapToDto(ImportManifestBlRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return ImportManifestBlDto.builder()
                .transactionPoid(dto.getTransactionPoid())
                .docId(dto.getDocRef())

                .vesselVoyagePoid(dto.getVoyageTransactionPoid())
                .blNumber(dto.getBlNumber())
                .blType(dto.getBlType())
                .cargo(dto.getCargoType())
                .originalBlsCount(dto.getNoOfOrgnlBls())
                .freight(dto.getFreightStatus())
                .quotationPoid(dto.getQuotationTransactionPoid())
                .salesmanPoid(dto.getSalesmanPoid())
                .issueType(dto.getBlIssueType())
                .bookedByPrincipal(dto.getBookedByPp())
                .freightAllInOne(dto.getAllInOneFreight())
                .remarks(dto.getRemarks())

                .shipperName(dto.getShipperEdiName())
                .shipperAddress(dto.getShipperEdiAddress())
                .consigneeName(dto.getConsigneeEdiName())
                .consigneePoid(dto.getConsigneePoid())
                .manifestEmailVerified(dto.getManifestEmailVerified())
                .noRecentShipmentVerified(dto.getEmailVerifiedWithSpecialC())
                .bookingCustomerPoid(dto.getBookingPartyPoid())
                .ediAddress(dto.getConsigneeEdiAddress())

                .preCarried(dto.getPreCarriedBy())
                .loadPortPoid(dto.getPortOfLoadingPoid())
                .receiptPortPoid(dto.getPlaceOfRecieptPoid())
                .deliveryPortPoid(dto.getPlaceOfDelieveryPoid())
                .dischargePortPoid(dto.getPortOfDischargePoid())
                .commodityPoid(dto.getComodityPoid())
                .grossWeight(dto.getTotalWeight())
                .cbm(dto.getTotalNetVolume())
                .weight(dto.getWeightUnit())
                .packTypes(dto.getUnitPack())
                .numberOfPacks(dto.getTotalNoOfPacks())
                .notifyName(dto.getNotify1EdiName())
                .notify1Poid(dto.getNotifyPoid1())
                .notifyEdiAddress(dto.getNotify1EdiAddress())
                .holdCanAuto(dto.getHoldCanDo())
                .manualCanSend(dto.getManuallyCanSend())
                .holdReason(dto.getHoldReason())
                .holdRemarks(dto.getHoldRemarks())
                .descriptionsAndMarks(mapToDescriptionAndMarks(dto.getCargoDescriptions()))
                .generalCargoDetails(mapToGeneralCargoDetails(dto.getGeneralCargoDetails()))
                .containers(mapToContainers(dto.getContainers()))
                .charges(mapToCharges(dto.getChargeDetails()))
                .otherCharges(mapToChargesOther(dto.getChargeDetails()))
                .partBls(mapToPartBls(dto.getPartBls()))
                .build();
    }

    public static ImportManifestBlDto mapToDto(ShipBlManifestHdr entity) {
        if (entity == null) {
            return null;
        }
        return ImportManifestBlDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .docId(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .vesselVoyagePoid(entity.getVoyageTransactionPoid())
                .blNumber(entity.getBlNumber())
                .blType(entity.getBlType())
                .cargo(entity.getCargoType())
                .originalBlsCount(entity.getNoOfOrgnlBls())
                .freight(entity.getFreightStatus())
                .quotationPoid(entity.getQuotationTransactionPoid())
                .salesmanPoid(entity.getSalesmanPoid())
                .issueType(entity.getBlIssueType())
                .bookedByPrincipal(entity.getBookedByPp())
                .freightAllInOne(entity.getAllInOneFreight())
                .remarks(entity.getRemarks())
                .shipperName(entity.getShipperEdiName())
                .shipperAddress(entity.getShipperEdiAddress())
                .consigneeName(entity.getConsigneeEdiName())
                .consigneePoid(entity.getConsigneePoid())
                .manifestEmailVerified(entity.getManifestEmailVerified())
                .noRecentShipmentVerified(entity.getEmailVerifiedWithSpecialC())
                .bookingCustomerPoid(entity.getBookingPartyPoid())
                .ediAddress(entity.getConsigneeEdiAddress())
                .preCarried(entity.getPreCarriedBy())
                .loadPortPoid(entity.getPortOfLoadingPoid())
                .receiptPortPoid(entity.getPlaceOfReceiptPoid())
                .deliveryPortPoid(entity.getPlaceOfDeliveryPoid())
                .dischargePortPoid(entity.getPortOfDischargePoid())
                .commodityPoid(entity.getComodityPoid())
                .grossWeight(entity.getTotalWeight())
                .cbm(entity.getTotalNetVolume())
                .weight(entity.getWeightUnit())
                .packTypes(entity.getUnitPack())
                .numberOfPacks(entity.getTotalNoOfPacks())
                .notifyName(entity.getNotify1EdiName())
                .notify1Poid(entity.getNotifyPoid1())
                .notifyEdiAddress(entity.getNotify1EdiAddress())
                .holdCanAuto(entity.getHoldCanDo())
                .manualCanSend(entity.getManuallyCanSend())
                .holdReason(entity.getHoldReason())
                .holdRemarks(entity.getHoldRemarks())
                .otherNotifies(OtherNotifyDto.builder()
                        .notify2EdiName(entity.getNotify2EdiName())
                        .notify2EdiAddress(entity.getNotify2EdiAddress())
                        .notify2Poid(entity.getNotifyPoid2())
                        .notify3EdiName(entity.getNotify3EdiName())
                        .notify3EdiAddress(entity.getNotify3EdiAddress())
                        .notify3Poid(entity.getNotifyPoid3())
                        .build())
                .build();
    }

    public static List<ChargeDto> mapToCharges(List<ChargeRequestDto> chargeDetails) {
        if (chargeDetails == null || chargeDetails.isEmpty()) {
            return List.of();
        }

        return chargeDetails.stream()
                .filter(charge -> "C".equalsIgnoreCase(charge.getFreightType()))
                .map(charge -> ChargeDto.builder()
                        .detRowId(charge.getDetRowId())
                        .chargePoid(charge.getChargePoid())
                        .printGroup(charge.getPrintGroup())
                        .currencyCode(charge.getCurrencyCode())
                        .quantity(charge.getQuantity())
                        .buy(charge.getBuyPercharge())
                        .buyAmount(charge.getPerQuantityAmount())
                        .taxPercentage(charge.getTaxPercentage())
                        .taxAmount(charge.getTaxAmount())
                        .paidAtPortPoid(charge.getPaidAtPortPoid())
                        .chargeDescription(charge.getChargeDescription())
                        .taxPoid(charge.getTaxPoid())
                        .chargeType(charge.getChargeType())
                        .actionType(charge.getActionType())
                        .build())
                .toList();
    }

    public static List<ChargeOtherDto> mapToChargesOther(List<ChargeRequestDto> chargeDetails) {
        if (chargeDetails == null || chargeDetails.isEmpty()) {
            return List.of();
        }

        return chargeDetails.stream()
                .filter(charge -> !"C".equalsIgnoreCase(charge.getFreightType()))
                .map(charge -> ChargeOtherDto.builder()
                        .detRowId(charge.getDetRowId())
                        .chargePoid(charge.getChargePoid())
                        .chargeType(charge.getChargeType())
                        .basis(charge.getChargeBasisOn())
                        .quantity(charge.getQuantity())
                        .currencyCode(charge.getCurrencyCode())
                        .exchangeRate(charge.getCurrencyExchange())
                        .buy(charge.getBuyPercharge())
                        .paidAtPortPoid(charge.getPaidAtPortPoid())
                        .build())
                .toList();
    }



    public static List<AddressDetailsDto> mapToAddressDetails(List<NotifyPartyRequestDto> notifyParties) {
        if (notifyParties == null || notifyParties.isEmpty()) {
            return List.of();
        }
        return notifyParties.stream()
                .map(party -> AddressDetailsDto.builder()
                        .detRowId(party.getDetRowId())
                        .addressType(party.getAddressType())  // just map it directly
                        .addressPoid(party.getAddressPoid())
                        .email1(party.getEmail1())
                        .email2(party.getEmail2())
                        .sendYesNo(party.getSendYesNo())
                        .sendEmailFax(party.getSendEmailFax())
                        .actionType(party.getActionType())
                        .build())
                .toList();
    }

    public static List<ContainerDto> mapToContainers(List<ContainerRequestDto> containers) {
        if (containers == null || containers.isEmpty()) {
            return List.of();
        }

        return containers.stream()
                .map(dto -> ContainerDto.builder()
                        .detRowId(dto.getDetRowId())
                        .socType(decodeContainerOwnership(dto.getEquipmentShipperOwn()))
                        .containerNumber(dto.getContainerNo())
                        .sealNumber(dto.getEquipmentSealNo())
                        .equipmentIsoType(dto.getEquipmentIsoType())
                        .shortDescription(dto.getCargoDescription())
                        .commodityPoid(dto.getComodityPoid())
                        .cbm(dto.getNetVolume())
                        .grossWeight(dto.getGrsWeight())
                        .netWeight(dto.getNetWeight())
                        .tareWeight(dto.getTareWeight())
                        .packs(dto.getNoOfPacks())
                        .packsType(dto.getPackUnit())
                        .hsCode(dto.getHsCode())
                        .hsDescription(dto.getHsDescription())
                        .customerDays(dto.getExtraFreeDays())
                        .principalDays(dto.getExtraFreeDaysPrnpls())
                        .imco("Y".equalsIgnoreCase(dto.getIsImco()))
                        .reefer("Y".equalsIgnoreCase(dto.getIsRefer()))
                        .oog("Y".equalsIgnoreCase(dto.getIsOog()))
                        .grantFlag("Y".equalsIgnoreCase(dto.getGuaranteeFlag()))
                        .grantBy(dto.getGuaranteedBy())
                        .amountPerDayAfterFree(dto.getAmountPerDayAfterFree())
                        .actualDischargeDate(dto.getActualDischargeDate())
                        .emptyDate(dto.getReturnFromConsignee())
                        .collectionDate(dto.getDisplayCollectedDate())
                        .collectionAmount(dto.getTotalAmountCollected())
                        .collectionDays(dto.getTotalDaysCollected())
                        .imcoType(dto.getImcoClassType())
                        .imcoNumber(dto.getImo())
                        .imcoClassDescription(dto.getImcoClassActual())
                        .rfType(dto.getReferType())
                        .rfHumidity(dto.getRefferHum())
                        .rfVent(dto.getRefferVent())
                        .rfTemperature(dto.getRefferTemp())
                        .oogType(dto.getOogType())
                        .oogBack(dto.getOogB())
                        .oogLeftWidth(dto.getOogLW())
                        .oogRightWidth(dto.getOogRW())
                        .oogHeight(dto.getOogH())
                        .oogLength(dto.getOogL())
                        .oogAdditional(dto.getOogA())
                        .oogFront(dto.getOogF())
                        .actionType(dto.getActionType())
                        .build())
                .toList();
    }

    public static List<GeneralCargoDto> mapToGeneralCargoDetails(
            List<GeneralCargoRequestDto> cargoDescriptions) {

        if (cargoDescriptions == null || cargoDescriptions.isEmpty()) {
            return List.of();
        }

        return cargoDescriptions.stream()
                .map(dto -> GeneralCargoDto.builder()
                        .detRowId(dto.getDetRowId())
                        .description(dto.getCargoDescription())
                        .commodityPoid(dto.getComodityPoid())
                        .volume(dto.getGrsVolume())
                        .grossWeight(dto.getGrsWeight())
                        .netWeight(dto.getNetWeight())
                        .tareWeight(dto.getTareWeight())
                        .packs(dto.getNoOfPacks())
                        .unit(dto.getPackUnit())
                        .quantity(dto.getQuantity())
                        .destinationPortPoid(dto.getDestinationPortPoid())
                        .actionType(dto.getActionType())
                        .build())
                .toList();
    }

    public static List<MafiDetailsDto> mapToMafiDetails(List<MafiRequestDto> mafiDetails) {
        if (mafiDetails == null || mafiDetails.isEmpty()) {
            return List.of();
        }

        return mafiDetails.stream()
                .map(dto -> MafiDetailsDto.builder()
                        .detRowId(dto.getDetRowId())
                        .mafiReferenceNumber(dto.getMafiRef())
                        .mafiSize(dto.getMafiSize())
                        .mafiFreeDays(dto.getMafiFreeDays())
                        .remarks(dto.getRemarks())
                        .build())
                .toList();
    }

    public static OtherNotifyDto mapToOtherNotifies(ImportManifestBlRequestDto dto) {
        return OtherNotifyDto.builder()
                .notify2EdiName(dto.getNotify2EdiName())
                .notify2EdiAddress(dto.getNotify2EdiAddress())
                .notify2Poid(dto.getNotifyPoid2())
                .notify3EdiName(dto.getNotify3EdiName())
                .notify3EdiAddress(dto.getNotify3EdiAddress())
                .notify3Poid(dto.getNotifyPoid3())
                .build();
    }

    public static List<PartBlDto> mapToPartBls(List<PartBlRequestDto> partBls) {
        if (partBls == null || partBls.isEmpty()) {
            return List.of();
        }
        return partBls.stream()
                .map(dto -> PartBlDto.builder()
                        .detRowId(dto.getDetRowId())
                        .partBlNumber(dto.getPartBlNumber())
                        .shipperName(dto.getShipperName())
                        .consigneeName(dto.getConsigneeName())
                        .cargoDescription(dto.getCargoDescription())
                        .commodityPoid(dto.getComodityPoid())
                        .packageDetails(dto.getNoOfPacks())
                        .packUnit(dto.getPackUnit())
                        .netWeight(dto.getNetWeight())
                        .netVolume(dto.getNetVolume())
                        .actionType(dto.getActionType())
                        .build())
                .toList();
    }

    public static List<DescriptionAndMarksDto> mapToDescriptionAndMarks(String simpleCargoDescription,
            String simpleCargoMarks) {
        if (simpleCargoDescription == null && simpleCargoMarks == null) {
            return List.of();
        }
        return List.of(
                DescriptionAndMarksDto.builder()
                        .descriptionType(CARGO_TYPE_DESCRIPTION)
                        .cargoDescription(simpleCargoDescription)
                        .build(),
                DescriptionAndMarksDto.builder()
                        .descriptionType(CARGO_TYPE_MARKS)
                        .cargoDescription(simpleCargoMarks)
                        .build())
                .stream()
                .filter(dto -> dto.getCargoDescription() != null)
                .toList();
    }

    public static List<DescriptionAndMarksDto> mapToDescriptionAndMarks(
            List<CargoDescriptionRequestDto> cargoDescriptions) {
        if (cargoDescriptions == null || cargoDescriptions.isEmpty()) {
            return List.of();
        }
        return cargoDescriptions.stream()
                .map(dto -> DescriptionAndMarksDto.builder()
                        .descriptionType(dto.getDescriptionType())
                        .cargoDescription(dto.getCargoDescription())
                        .build())
                .toList();
    }

    public static ShipBlManifestHdr mapToEntity(ImportManifestBlDto dto,ShipBlManifestHdr entity) {
        if (dto == null)
            return null;

        entity.setTransactionPoid(dto.getTransactionPoid());
        entity.setDocRef(dto.getDocId());
        entity.setVoyageTransactionPoid(dto.getVesselVoyagePoid());
        entity.setBlNumber(dto.getBlNumber());
        entity.setBlType(dto.getBlType());
        entity.setCargoType(dto.getCargo());
        entity.setNoOfOrgnlBls(dto.getOriginalBlsCount());
        entity.setFreightStatus(dto.getFreight());
        entity.setQuotationTransactionPoid(dto.getQuotationPoid());
        entity.setSalesmanPoid(dto.getSalesmanPoid());
        entity.setBlIssueType(dto.getIssueType());
        entity.setBookedByPp(dto.getBookedByPrincipal());
        entity.setAllInOneFreight(dto.getFreightAllInOne());
        entity.setRemarks(dto.getRemarks());

        entity.setShipperEdiName(dto.getShipperName());
        entity.setShipperEdiAddress(dto.getShipperAddress());
        entity.setConsigneeEdiName(dto.getConsigneeName());
        entity.setConsigneePoid(dto.getConsigneePoid());
        entity.setManifestEmailVerified(dto.getManifestEmailVerified());
        entity.setEmailVerifiedWithSpecialC(dto.getNoRecentShipmentVerified());
        entity.setBookingPartyPoid(dto.getBookingCustomerPoid());
        entity.setConsigneeEdiAddress(dto.getEdiAddress());

        entity.setPreCarriedBy(dto.getPreCarried());
        entity.setPortOfLoadingPoid(dto.getLoadPortPoid());
        entity.setPlaceOfReceiptPoid(dto.getReceiptPortPoid());
        entity.setPlaceOfDeliveryPoid(dto.getDeliveryPortPoid());
        entity.setPortOfDischargePoid(dto.getDischargePortPoid());
        entity.setComodityPoid(dto.getCommodityPoid());
        entity.setTotalWeight(dto.getGrossWeight());
        entity.setTotalNetVolume(dto.getCbm());
        entity.setWeightUnit(dto.getWeight());
        entity.setUnitPack(dto.getPackTypes());
        entity.setTotalNoOfPacks(dto.getNumberOfPacks());

        entity.setNotify1EdiName(dto.getNotifyName());
        entity.setNotifyPoid1(dto.getNotify1Poid());
        entity.setNotify1EdiAddress(dto.getNotifyEdiAddress());

        if (dto.getOtherNotifies() != null) {
            OtherNotifyDto notify = dto.getOtherNotifies();
            entity.setNotify2EdiName(notify.getNotify2EdiName());
            entity.setNotify2EdiAddress(notify.getNotify2EdiAddress());
            entity.setNotifyPoid2(notify.getNotify2Poid());
            entity.setNotify3EdiName(notify.getNotify3EdiName());
            entity.setNotify3EdiAddress(notify.getNotify3EdiAddress());
            entity.setNotifyPoid3(notify.getNotify3Poid());
        }

        entity.setHoldCanDo(dto.getHoldCanAuto());
        entity.setManuallyCanSend(dto.getManualCanSend());
        entity.setHoldReason(dto.getHoldReason());
        entity.setHoldRemarks(dto.getHoldRemarks());

        return entity;
    }


    public static ShipBlManifestGeneralDtl mapGeneralEntityFromDto(GeneralCargoDto dto, Long transactionPoid,ShipBlManifestGeneralDtl entity) {
        if (dto == null)
            return null;

        ShipBlManifestDtlId id = new ShipBlManifestDtlId();
        id.setTransactionPoid(transactionPoid);
        if (dto.getDetRowId() != null)
            id.setDetRowId(dto.getDetRowId());
        entity.setId(id);

        entity.setCargoDescription(dto.getDescription());
        entity.setComodityPoid(dto.getCommodityPoid());
        entity.setGrsVolume(dto.getVolume());
        entity.setGrsWeight(dto.getGrossWeight());
        entity.setNetWeight(dto.getNetWeight());
        entity.setTareWeight(dto.getTareWeight());
        entity.setNoOfPacks(dto.getPacks());
        entity.setPackUnit(dto.getUnit());
        entity.setQuantity(dto.getQuantity());
        entity.setDestinationPortPoid(dto.getDestinationPortPoid());

        return entity;
    }

    public static ShipBlManifestCargoDtl mapCargoDescriptionAndMarksFromDto(DescriptionAndMarksDto dto,
            Long transactionPoid,ShipBlManifestCargoDtl entity) {
        if (dto == null)
            return null;


        ShipBlManifestCargoDtlId id = new ShipBlManifestCargoDtlId();
        id.setTransactionPoid(transactionPoid);
        if (dto.getDetRowId() != null)
            id.setDetRowId(dto.getDetRowId());
        entity.setId(id);

        entity.getId().setDescriptionType(dto.getDescriptionType());
        entity.setCargoDescription(dto.getCargoDescription());

        return entity;
    }

    public static ShipBlManifestContainerDtl mapContainerEntityFromDto(ContainerDto dto, Long transactionPoid,ShipBlManifestContainerDtl entity) {
        if (dto == null)
            return null;

        ShipBlManifestDtlId id = new ShipBlManifestDtlId();
        id.setTransactionPoid(transactionPoid);
        if (dto.getDetRowId() != null)
            id.setDetRowId(dto.getDetRowId());
        entity.setId(id);

        entity.setEquipmentShipperOwn(encodeContainerOwnership(dto.getSocType()));
        entity.setContainerNo(dto.getContainerNumber());
        entity.setEquipmentSealNo(dto.getSealNumber());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());
        entity.setCargoDescription(dto.getShortDescription());
        entity.setComodityPoid(dto.getCommodityPoid());
        entity.setNetVolume(dto.getCbm());
        entity.setGrsWeight(dto.getGrossWeight());
        entity.setNetWeight(dto.getNetWeight());
        entity.setTareWeight(dto.getTareWeight());
        entity.setNoOfPacks(dto.getPacks());
        entity.setPackUnit(dto.getPacksType());
        entity.setHsCode(dto.getHsCode());
        entity.setHsDescription(dto.getHsDescription());
        entity.setExtraFreeDays(dto.getCustomerDays());
        entity.setExtraFreeDaysPrnpls(dto.getPrincipalDays());
        entity.setIsImco(dto.getImco() != null && dto.getImco() ? "Y" : "N");
        entity.setIsRefer(dto.getReefer() != null && dto.getReefer() ? "Y" : "N");
        entity.setIsOog(dto.getOog() != null && dto.getOog() ? "Y" : "N");
        entity.setGuaranteeFlag(dto.getGrantFlag() != null && dto.getGrantFlag() ? "Y" : "N");
        entity.setGuaranteedBy(dto.getGrantBy());
        entity.setAmountPerDayAfterFree(dto.getAmountPerDayAfterFree());
        entity.setActualDischargeDate(dto.getActualDischargeDate());
        entity.setReturnFromConsignee(dto.getEmptyDate());
        entity.setDisplayCollectedDate(dto.getCollectionDate());
        entity.setTotalAmountCollected(dto.getCollectionAmount());
        entity.setTotalDaysCollected(dto.getCollectionDays());
        entity.setImcoClassType(dto.getImcoType());
        entity.setImo(dto.getImcoNumber());
        entity.setImcoClassActual(dto.getImcoClassDescription());
        entity.setReferType(dto.getRfType());
        entity.setRefferHum(dto.getRfHumidity());
        entity.setRefferVent(dto.getRfVent());
        entity.setRefferTemp(dto.getRfTemperature());
        entity.setOogType(dto.getOogType());
        entity.setOogB(dto.getOogBack());
        entity.setOogLW(dto.getOogLeftWidth());
        entity.setOogRW(dto.getOogRightWidth());
        entity.setOogH(dto.getOogHeight());
        entity.setOogL(dto.getOogLength());
        entity.setOogA(dto.getOogAdditional());
        entity.setOogF(dto.getOogFront());

        return entity;
    }

    public static ShipBlManifestChargesDtl mapChargesDtlFromDto(ChargeDto dto, Long transactionPoid, ShipBlManifestChargesDtl entity) {
        if (dto == null)
            return null;


        if (dto.getDetRowId() != null) {
            ShipBlManifestDtlId id = new ShipBlManifestDtlId();
            id.setTransactionPoid(transactionPoid);
            id.setDetRowId(dto.getDetRowId());
            entity.setId(id);
        }

        entity.setChargePoid(dto.getChargePoid());
        entity.setPrintGroup(dto.getPrintGroup());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setQuantity(dto.getQuantity());
        entity.setBuyPercharge(dto.getBuy());
        entity.setPerQuantityAmount(dto.getBuyAmount());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setPaidAtPortPoid(dto.getPaidAtPortPoid());
        entity.setChargeDescription(dto.getChargeDescription());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setFreightType(dto.getFreightType());
        entity.setChargeType(dto.getChargeType());
        entity.setChargeBasisOn(dto.getBasisPoid());
        return entity;
    }

    public static ShipBlManifestPartBL mapPartBlEntityFromDto(PartBlDto dto, Long transactionPoid,ShipBlManifestPartBL entity) {
        if (dto == null)
            return null;


        if (dto.getDetRowId() != null) {
            ShipBlManifestDtlId id = new ShipBlManifestDtlId();
            id.setTransactionPoid(transactionPoid);
            id.setDetRowId(dto.getDetRowId());
            entity.setId(id);
        }

        entity.setPartBlNumber(dto.getPartBlNumber());
        entity.setShipperName(dto.getShipperName());
        entity.setConsigneeName(dto.getConsigneeName());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setComodityPoid(dto.getCommodityPoid());
        entity.setNoOfPacks(dto.getPackageDetails());
        entity.setPackUnit(dto.getPackUnit());
        entity.setNetWeight(dto.getNetWeight());
        entity.setNetVolume(dto.getNetVolume());

        return entity;
    }

    public static ShipBlManifestEmailFaxDtl mapEmailFaxEntityFromDto(AddressDetailsDto dto, Long transactionPoid,ShipBlManifestEmailFaxDtl entity) {
        if (dto == null)
            return null;

        if (dto.getDetRowId() != null) {
            ShipBlManifestEmailFaxId id = new ShipBlManifestEmailFaxId();
            id.setTransactionPoid(transactionPoid);
            id.setDetRowId(dto.getDetRowId());
            id.setAddressType(dto.getAddressType());
            entity.setId(id);
        }

        entity.setAddressPoid(dto.getAddressPoid());
        entity.setEmail1(dto.getEmail1());
        entity.setEmail2(dto.getEmail2());
        entity.setSendYesNo(dto.getSendYesNo());
        entity.setSendEmailFax(dto.getSendEmailFax());

        return entity;
    }


    public static ShipBlManifestMafiDtl mapMafiEntityFromDto(MafiDetailsDto dto, Long transactionPoid,ShipBlManifestMafiDtl entity) {
        if (dto == null)
            return null;

        if (dto.getDetRowId() != null) {
            ShipBlManifestDtlId id = new ShipBlManifestDtlId();
            id.setTransactionPoid(transactionPoid);
            id.setDetRowId(dto.getDetRowId());
            entity.setId(id);
        }

        entity.setMafiRef(dto.getMafiReferenceNumber());
        entity.setMafiSize(dto.getMafiSize());
        entity.setMafiFreeDays(dto.getMafiFreeDays());
        entity.setRemarks(dto.getRemarks());

        return entity;
    }

    private static String encodeContainerOwnership(String socType) {
        if (socType == null) {
            return null;
        }
        return switch (socType.trim().toUpperCase()) {
            case "COC", "C" -> CONTAINER_OWN_CUSTOMER;
            case "SOC", "S" -> CONTAINER_OWN_SHIPPER;
            default -> socType.isEmpty() ? null : socType.substring(0, 1).toUpperCase();
        };
    }

    private static String decodeContainerOwnership(String equipmentShipperOwn) {
        if (equipmentShipperOwn == null) {
            return null;
        }
        return switch (equipmentShipperOwn.trim().toUpperCase()) {
            case CONTAINER_OWN_CUSTOMER -> "COC";
            case CONTAINER_OWN_SHIPPER -> "SOC";
            default -> equipmentShipperOwn;
        };
    }

}
