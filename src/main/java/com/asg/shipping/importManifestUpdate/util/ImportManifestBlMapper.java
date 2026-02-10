package com.asg.shipping.importManifestUpdate.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Component
public class ImportManifestBlMapper {

    /**
     * Convert Header Entity to DTO
     */
    public ImportManifestBlRequestDto mapToDto(ShipBlManifestHdr entity) {
        if (entity == null) {
            return null;
        }

        return ImportManifestBlRequestDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .voyageTransactionPoid(entity.getVoyageTransactionPoid())
                .blNumber(entity.getBlNumber())
                .agentReference(entity.getAgentReference())
                .shipperPoid(entity.getShipperPoid())
                .shipperAddressPoid(entity.getShipperAddressPoid())
                .consigneePoid(entity.getConsigneePoid())
                .consigneeAddressPoid(entity.getConsigneeAddressPoid())
                .notifyPoid1(entity.getNotifyPoid1())
                .notifyAddressPoid1(entity.getNotifyAddressPoid1())
                .notifyPoid2(entity.getNotifyPoid2())
                .notifyAddressPoid2(entity.getNotifyAddressPoid2())
                .notifyPoid3(entity.getNotifyPoid3())
                .notifyAddressPoid3(entity.getNotifyAddressPoid3())
                .quotationTransactionPoid(entity.getQuotationTransactionPoid())
                .salesmanPoid(entity.getSalesmanPoid())
                .comodityPoid(entity.getComodityPoid())
                .noOfOrgnlBls(entity.getNoOfOrgnlBls())
                .exportReference(entity.getExportReference())
                .lpoSrnNo(entity.getLpoSrnNo())
                .lpoSrnDate(entity.getLpoSrnDate())
                .typeOfMove(entity.getTypeOfMove())
                .preCarriedBy(entity.getPreCarriedBy())
                .placeOfIssuePoid(entity.getPlaceOfIssuePoid())
                .dateOfIssue(entity.getDateOfIssue())
                .printFreightDetails(entity.getPrintFreightDetails())
                .totalVolume(entity.getTotalVolume())
                .totalNetVolume(entity.getTotalNetVolume())
                .totalWeight(entity.getTotalWeight())
                .totalNetWeight(entity.getTotalNetWeight())
                .weightUnit(entity.getWeightUnit())
                .unitPack(entity.getUnitPack())
                .totalNoOfPacks(entity.getTotalNoOfPacks())
                .placeOfRecieptPoid(entity.getPlaceOfReceiptPoid())
                .placeOfDelieveryPoid(entity.getPlaceOfDeliveryPoid())
                .portOfLoadingPoid(entity.getPortOfLoadingPoid())
                .portOfDischargePoid(entity.getPortOfDischargePoid())
                .remarks(entity.getRemarks())
                .blStatus(entity.getBlStatus())
                .blOrginalPrint(entity.getBlOrginalPrint())
                .blOrginalDate(entity.getBlOrginalDate())
                .blPrintedBy(entity.getBlPrintedBy())
                .uniqueBlno(entity.getUniqueBlno())
                .demRate(entity.getDemRate())
                .demFreeDays(entity.getDemFreeDays())
                .releasedStatus(entity.getReleasedStatus())
                .releasedDate(entity.getReleasedDate())
                .relasedToPerson(entity.getRelasedToPerson())
                .relasedIdPerson(entity.getRelasedIdPerson())
                .relasedAddrsPerson(entity.getRelasedAddrsPerson())
                .relasedBy(entity.getRelasedBy())
                .openDaysAfter(entity.getOpenDaysAfter())
                .releasedType(entity.getReleasedType())
                .relasedSeqno(entity.getRelasedSeqno())
                .releasedGrantBy(entity.getReleasedGrantBy())
                .releasedGrantDate(entity.getReleasedGrantDate())
                .releasedGrantReason(entity.getReleasedGrantReason())
                .cargoType(entity.getCargoType())
                .blType(entity.getBlType())
                .doNo(entity.getDoNo())
                .deleted(entity.getDeleted())
                .blIssueType(entity.getBlIssueType())
                .shipperEdiName(entity.getShipperEdiName())
                .shipperEdiAddress(entity.getShipperEdiAddress())
                .consigneeEdiName(entity.getConsigneeEdiName())
                .consigneeEdiAddress(entity.getConsigneeEdiAddress())
                .notify1EdiName(entity.getNotify1EdiName())
                .notify1EdiAddress(entity.getNotify1EdiAddress())
                .notify2EdiName(entity.getNotify2EdiName())
                .notify2EdiAddress(entity.getNotify2EdiAddress())
                .notify3EdiName(entity.getNotify3EdiName())
                .notify3EdiAddress(entity.getNotify3EdiAddress())
                .canNotifyCustomerPoid(entity.getCanNotifyCustomerPoid())
                .bookedBy(entity.getBookedBy())
                .freightStatus(entity.getFreightStatus())
                .holdCanDo(entity.getHoldCanDo())
                .holdReason(entity.getHoldReason())
                .canSentQueue(entity.getCanSentQueue())
                .canSentDate(entity.getCanSentDate())
                .canSentBy(entity.getCanSentBy())
                .documentCompanyPoid(entity.getDocumentCompanyPoid())
                .documentCompanyDivisionPoid(entity.getDocumentCompanyDivisionPoid())
                .blPlaceReceipt(entity.getBlPlaceReceipt())
                .blPlaceLoad(entity.getBlPlaceLoad())
                .blFinalDestination(entity.getBlFinalDestination())
                .bookingPartyPoid(entity.getBookingPartyPoid())
                .blPlaceDischareDesc(entity.getBlPlaceDischargeDesc())
                .cargoArrivalNumber(entity.getCargoArrivalNumber())
                .bookedByPp(entity.getBookedByPp())
                .manuallyCanSend(entity.getManuallyCanSend())
                .allInOneFreight(entity.getAllInOneFreight())
                .holdRemarks(entity.getHoldRemarks())
                .blConsigneeAddressAdd(entity.getBlConsigneeAddressAdd())
                .avoidCargoAlert(entity.getAvoidCargoAlert())
                .agentPoid(entity.getAgentPoid())
                .ffJobNoHold(entity.getFfJobNoHold())
                .issueManualInvoice(entity.getIssueManualInvoice())
                .doPriority(entity.getDoPriority())
                .doIssueAuth(entity.getDoIssueAuth())
                .doIssueAuthPoid(entity.getDoIssueAuthPoid())
                .doCntToConsignee(entity.getDoCntToConsignee())
                .doCntToNotify(entity.getDoCntToNotify())
                .doCntToOthers(entity.getDoCntToOthers())
                .doCntToOthersMails(entity.getDoCntToOthersMails())
                .doCntReasonFailure(entity.getDoCntReasonFailure())
                .doCntToRegsMails(entity.getDoCntToRegsMails())
                .deliverySentTo(entity.getDeliverySentTo())
                .ffBillToPoid(entity.getFfBillToPoid())
                .demActualNextDay(entity.getDemActualNextDay())
                .principalDoNumber(entity.getPrincipalDoNumber())
                .stopUcanAlert(entity.getStopUcanAlert())
                .isMbl(entity.getIsMbl())
                .forwarderPin(entity.getForwarderPin())
                .manifestEmailVerified(entity.getManifestEmailVerified())
                .emailVerifiedWithSpecialC(entity.getEmailVerifiedWithSpecialC())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Convert DTO to Header Entity for create operation
     */
    public ShipBlManifestHdr mapToEntity(ImportManifestBlCreateDto dto) {
        if (dto == null) return null;
        return ShipBlManifestHdr.builder()
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .docRef(dto.getDocRef())
                .transactionDate(dto.getTransactionDate())
                .voyageTransactionPoid(dto.getVoyageTransactionPoid())
                .blNumber(dto.getBlNumber() != null ? dto.getBlNumber().trim() : null)
                .agentReference(dto.getAgentReference())
                .shipperPoid(dto.getShipperPoid())
                .shipperAddressPoid(dto.getShipperAddressPoid())
                .consigneePoid(dto.getConsigneePoid())
                .consigneeAddressPoid(dto.getConsigneeAddressPoid())
                .notifyPoid1(dto.getNotifyPoid1())
                .notifyAddressPoid1(dto.getNotifyAddressPoid1())
                .notifyPoid2(dto.getNotifyPoid2())
                .notifyAddressPoid2(dto.getNotifyAddressPoid2())
                .notifyPoid3(dto.getNotifyPoid3())
                .notifyAddressPoid3(dto.getNotifyAddressPoid3())
                .quotationTransactionPoid(dto.getQuotationTransactionPoid())
                .salesmanPoid(dto.getSalesmanPoid())
                .comodityPoid(dto.getComodityPoid())
                .noOfOrgnlBls(dto.getNoOfOrgnlBls())
                .exportReference(dto.getExportReference())
                .lpoSrnNo(dto.getLpoSrnNo())
                .lpoSrnDate(dto.getLpoSrnDate())
                .typeOfMove(dto.getTypeOfMove())
                .preCarriedBy(dto.getPreCarriedBy())
                .placeOfIssuePoid(dto.getPlaceOfIssuePoid())
                .dateOfIssue(dto.getDateOfIssue())
                .printFreightDetails(dto.getPrintFreightDetails())
                .totalVolume(dto.getTotalVolume())
                .totalNetVolume(dto.getTotalNetVolume())
                .totalWeight(dto.getTotalWeight())
                .totalNetWeight(dto.getTotalNetWeight())
                .weightUnit(dto.getWeightUnit())
                .unitPack(dto.getUnitPack())
                .totalNoOfPacks(dto.getTotalNoOfPacks())
                .placeOfReceiptPoid(dto.getPlaceOfRecieptPoid())
                .placeOfDeliveryPoid(dto.getPlaceOfDelieveryPoid())
                .portOfLoadingPoid(dto.getPortOfLoadingPoid())
                .portOfDischargePoid(dto.getPortOfDischargePoid())
                .remarks(dto.getRemarks())
                .blStatus(dto.getBlStatus())
                .blType(dto.getBlType())
                .cargoType(dto.getCargoType())
                .doNo(dto.getDoNo())
                .blIssueType(dto.getBlIssueType())
                .shipperEdiName(dto.getShipperEdiName())
                .shipperEdiAddress(dto.getShipperEdiAddress())
                .consigneeEdiName(dto.getConsigneeEdiName())
                .consigneeEdiAddress(dto.getConsigneeEdiAddress())
                .notify1EdiName(dto.getNotify1EdiName())
                .notify1EdiAddress(dto.getNotify1EdiAddress())
                .notify2EdiName(dto.getNotify2EdiName())
                .notify2EdiAddress(dto.getNotify2EdiAddress())
                .notify3EdiName(dto.getNotify3EdiName())
                .notify3EdiAddress(dto.getNotify3EdiAddress())
                .canNotifyCustomerPoid(dto.getCanNotifyCustomerPoid())
                .bookedBy(dto.getBookedBy())
                .freightStatus(dto.getFreightStatus())
                .holdCanDo(dto.getHoldCanDo())
                .holdReason(dto.getHoldReason())
                .documentCompanyPoid(dto.getDocumentCompanyPoid())
                .documentCompanyDivisionPoid(dto.getDocumentCompanyDivisionPoid())
                .blPlaceReceipt(dto.getBlPlaceReceipt())
                .blPlaceLoad(dto.getBlPlaceLoad())
                .blFinalDestination(dto.getBlFinalDestination())
                .bookingPartyPoid(dto.getBookingPartyPoid())
                .blPlaceDischargeDesc(dto.getBlPlaceDischareDesc())
                .cargoArrivalNumber(dto.getCargoArrivalNumber())
                .bookedByPp(dto.getBookedByPp())
                .manuallyCanSend(dto.getManuallyCanSend())
                .allInOneFreight(dto.getAllInOneFreight())
                .holdRemarks(dto.getHoldRemarks())
                .blConsigneeAddressAdd(dto.getBlConsigneeAddressAdd())
                .avoidCargoAlert(dto.getAvoidCargoAlert())
                .agentPoid(dto.getAgentPoid())
                .ffJobNoHold(dto.getFfJobNoHold())
                .issueManualInvoice(dto.getIssueManualInvoice())
                .doPriority(dto.getDoPriority())
                .doIssueAuth(dto.getDoIssueAuth())
                .doIssueAuthPoid(dto.getDoIssueAuthPoid())
                .doCntToConsignee(dto.getDoCntToConsignee())
                .doCntToNotify(dto.getDoCntToNotify())
                .doCntToOthers(dto.getDoCntToOthers())
                .doCntToOthersMails(dto.getDoCntToOthersMails())
                .doCntReasonFailure(dto.getDoCntReasonFailure())
                .doCntToRegsMails(dto.getDoCntToRegsMails())
                .deliverySentTo(dto.getDeliverySentTo())
                .ffBillToPoid(dto.getFfBillToPoid())
                .demActualNextDay(dto.getDemActualNextDay())
                .principalDoNumber(dto.getPrincipalDoNumber())
                .stopUcanAlert(dto.getStopUcanAlert())
                .isMbl(dto.getIsMbl())
                .forwarderPin(dto.getForwarderPin())
                .deleted("N")
                .build();
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public ShipBlManifestHdr mapUpdateDTOToEntity(ImportManifestBlUpdateDTO dto, ShipBlManifestHdr entity) {

        entity.setTransactionDate(dto.getTransactionDate());
        entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        entity.setBlNumber(dto.getBlNumber() != null ? dto.getBlNumber().trim() : null);
        entity.setAgentReference(dto.getAgentReference());
        entity.setShipperPoid(dto.getShipperPoid());
        entity.setShipperAddressPoid(dto.getShipperAddressPoid());
        entity.setConsigneePoid(dto.getConsigneePoid());
        entity.setConsigneeAddressPoid(dto.getConsigneeAddressPoid());
        entity.setNotifyPoid1(dto.getNotifyPoid1());
        entity.setNotifyAddressPoid1(dto.getNotifyAddressPoid1());
        entity.setNotifyPoid2(dto.getNotifyPoid2());
        entity.setNotifyAddressPoid2(dto.getNotifyAddressPoid2());
        entity.setNotifyPoid3(dto.getNotifyPoid3());
        entity.setNotifyAddressPoid3(dto.getNotifyAddressPoid3());
        entity.setQuotationTransactionPoid(dto.getQuotationTransactionPoid());
        entity.setSalesmanPoid(dto.getSalesmanPoid());
        entity.setComodityPoid(dto.getComodityPoid());
        entity.setNoOfOrgnlBls(dto.getNoOfOrgnlBls());
        entity.setExportReference(dto.getExportReference());
        entity.setLpoSrnNo(dto.getLpoSrnNo());
        entity.setLpoSrnDate(dto.getLpoSrnDate());
        entity.setTypeOfMove(dto.getTypeOfMove());
        entity.setPreCarriedBy(dto.getPreCarriedBy());
        entity.setPlaceOfIssuePoid(dto.getPlaceOfIssuePoid());
        entity.setDateOfIssue(dto.getDateOfIssue());
        entity.setPrintFreightDetails(dto.getPrintFreightDetails());
        entity.setTotalVolume(dto.getTotalVolume());
        entity.setTotalNetVolume(dto.getTotalNetVolume());
        entity.setTotalWeight(dto.getTotalWeight());
        entity.setTotalNetWeight(dto.getTotalNetWeight());
        entity.setWeightUnit(dto.getWeightUnit());
        entity.setUnitPack(dto.getUnitPack());
        entity.setTotalNoOfPacks(dto.getTotalNoOfPacks());
        entity.setPlaceOfReceiptPoid(dto.getPlaceOfRecieptPoid());
        entity.setPlaceOfDeliveryPoid(dto.getPlaceOfDelieveryPoid());
        entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
        entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
        entity.setRemarks(dto.getRemarks());
        entity.setBlStatus(dto.getBlStatus());
        entity.setBlType(dto.getBlType());
        entity.setCargoType(dto.getCargoType());
        entity.setDoNo(dto.getDoNo());
        entity.setBlIssueType(dto.getBlIssueType());
        entity.setShipperEdiName(dto.getShipperEdiName());
        entity.setShipperEdiAddress(dto.getShipperEdiAddress());
        entity.setConsigneeEdiName(dto.getConsigneeEdiName());
        entity.setConsigneeEdiAddress(dto.getConsigneeEdiAddress());
        entity.setNotify1EdiName(dto.getNotify1EdiName());
        entity.setNotify1EdiAddress(dto.getNotify1EdiAddress());
        entity.setNotify2EdiName(dto.getNotify2EdiName());
        entity.setNotify2EdiAddress(dto.getNotify2EdiAddress());
        entity.setNotify3EdiName(dto.getNotify3EdiName());
        entity.setNotify3EdiAddress(dto.getNotify3EdiAddress());
        entity.setCanNotifyCustomerPoid(dto.getCanNotifyCustomerPoid());
        entity.setBookedBy(dto.getBookedBy());
        entity.setFreightStatus(dto.getFreightStatus());
        entity.setHoldCanDo(dto.getHoldCanDo());
        entity.setHoldReason(dto.getHoldReason());
        entity.setDocumentCompanyPoid(dto.getDocumentCompanyPoid());
        entity.setDocumentCompanyDivisionPoid(dto.getDocumentCompanyDivisionPoid());
        entity.setBlPlaceReceipt(dto.getBlPlaceReceipt());
        entity.setBlPlaceLoad(dto.getBlPlaceLoad());
        entity.setBlFinalDestination(dto.getBlFinalDestination());
        entity.setBookingPartyPoid(dto.getBookingPartyPoid());
        entity.setBlPlaceDischargeDesc(dto.getBlPlaceDischareDesc());
        entity.setCargoArrivalNumber(dto.getCargoArrivalNumber());
        entity.setBookedByPp(dto.getBookedByPp());
        entity.setManuallyCanSend(dto.getManuallyCanSend());
        entity.setAllInOneFreight(dto.getAllInOneFreight());
        entity.setHoldRemarks(dto.getHoldRemarks());
        entity.setBlConsigneeAddressAdd(dto.getBlConsigneeAddressAdd());
        entity.setAvoidCargoAlert(dto.getAvoidCargoAlert());
        entity.setAgentPoid(dto.getAgentPoid());
        entity.setFfJobNoHold(dto.getFfJobNoHold());
        entity.setIssueManualInvoice(dto.getIssueManualInvoice());
        entity.setDoPriority(dto.getDoPriority());
        entity.setDoIssueAuth(dto.getDoIssueAuth());
        entity.setDoIssueAuthPoid(dto.getDoIssueAuthPoid());
        entity.setDoCntToConsignee(dto.getDoCntToConsignee());
        entity.setDoCntToNotify(dto.getDoCntToNotify());
        entity.setDoCntToOthers(dto.getDoCntToOthers());
        entity.setDoCntToOthersMails(dto.getDoCntToOthersMails());
        entity.setDoCntReasonFailure(dto.getDoCntReasonFailure());
        entity.setDoCntToRegsMails(dto.getDoCntToRegsMails());
        entity.setDeliverySentTo(dto.getDeliverySentTo());
        entity.setFfBillToPoid(dto.getFfBillToPoid());
        entity.setDemActualNextDay(dto.getDemActualNextDay());
        entity.setPrincipalDoNumber(dto.getPrincipalDoNumber());
        entity.setStopUcanAlert(dto.getStopUcanAlert());
        entity.setIsMbl(dto.getIsMbl());
        entity.setForwarderPin(dto.getForwarderPin());
        entity.setManifestEmailVerified(dto.getManifestEmailVerified());
        entity.setEmailVerifiedWithSpecialC(dto.getEmailVerifiedWithSpecialC());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }


    // Detail mapping methods - General DTL
    public GeneralCargoRequestDto mapGeneralDtlToDto(ShipBlManifestGeneralDtl entity) {
        if (entity == null) return null;
        return GeneralCargoRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .comodityPoid(entity.getComodityPoid())
                .cargoDescription(entity.getCargoDescription())
                .quantity(entity.getQuantity())
                .grsVolume(entity.getGrsVolume())
                .grsWeight(entity.getGrsWeight())
                .netVolume(entity.getNetVolume())
                .netWeight(entity.getNetWeight())
                .tareWeight(entity.getTareWeight())
                .noOfPacks(entity.getNoOfPacks())
                .packUnit(entity.getPackUnit())
                .destinationPortPoid(entity.getDestinationPortPoid())
                .build();
    }

    public ShipBlManifestGeneralDtl mapGeneralDtlFromDto(GeneralCargoRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;

        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );
        return ShipBlManifestGeneralDtl.builder()
                .id(id)
                .comodityPoid(dto.getComodityPoid())
                .cargoDescription(dto.getCargoDescription())
                .quantity(dto.getQuantity())
                .grsVolume(dto.getGrsVolume())
                .grsWeight(dto.getGrsWeight())
                .netVolume(dto.getNetVolume())
                .netWeight(dto.getNetWeight())
                .tareWeight(dto.getTareWeight())
                .noOfPacks(dto.getNoOfPacks())
                .packUnit(dto.getPackUnit())
                .destinationPortPoid(dto.getDestinationPortPoid())
                .build();
    }

    // Detail mapping methods - Cargo DTL
    public CargoDescriptionRequestDto mapCargoDtlToDto(ShipBlManifestCargoDtl entity) {
        if (entity == null) return null;
        return CargoDescriptionRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .cargoDescription(entity.getCargoDescription())
                .descriptionType(entity.getId()!=null ? entity.getId().getDescriptionType() : null)
                .recordOrder(entity.getRecordOrder())
                .build();
    }

    public ShipBlManifestCargoDtl mapCargoDtlFromDto(CargoDescriptionRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;

        ShipBlManifestCargoDtlId id = new ShipBlManifestCargoDtlId(
                transactionPoid,
                dto.getDetRowId(),
               dto.getDescriptionType()
        );
        return ShipBlManifestCargoDtl.builder()
                .id(id)
                .cargoDescription(dto.getCargoDescription())
                .recordOrder(dto.getRecordOrder())
                .build();
    }

    // Detail mapping methods - Container DTL
    public ContainerRequestDto mapContainerDtlToDto(ShipBlManifestContainerDtl entity) {
        if (entity == null) return null;

        return ContainerRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .mateTransactionPoid(entity.getMateTransactionPoid())
                .containerNo(entity.getContainerNo())
                .equipmentShipperOwn(entity.getEquipmentShipperOwn())
                .cargoDescription(entity.getCargoDescription())
                .equipmentSealNo(entity.getEquipmentSealNo())
                .equipmentIsoType(entity.getEquipmentIsoType())
                .equipmentType(entity.getEquipmentType())
                .equipmentSize(entity.getEquipmentSize())
                .quantity(entity.getQuantity())
                .grsVolume(entity.getGrsVolume())
                .grsWeight(entity.getGrsWeight())
                .netVolume(entity.getNetVolume())
                .netWeight(entity.getNetWeight())
                .tareWeight(entity.getTareWeight())
                .noOfPacks(entity.getNoOfPacks())
                .packUnit(entity.getPackUnit())
                .comodityPoid(entity.getComodityPoid())
                .destinationPortPoid(entity.getDestinationPortPoid())
                .imo(entity.getImo())
                .oogL(entity.getOogL())
                .oogB(entity.getOogB())
                .oogH(entity.getOogH())
                .refferTemp(entity.getRefferTemp())
                .refferHum(entity.getRefferHum())
                .refferVent(entity.getRefferVent())
                .issueToConsignee(toLocalDate(entity.getIssueToConsignee()))
                .returnFromConsignee(toLocalDate(entity.getReturnFromConsignee()))
                .displayCollectedDate(toLocalDate(entity.getDisplayCollectedDate()))
                .actualDischargeDate(toLocalDate(entity.getActualDischargeDate()))
                .isImco(entity.getIsImco())
                .isOog(entity.getIsOog())
                .isRefer(entity.getIsRefer())
                .referType(entity.getReferType())
                .imcoClassType(entity.getImcoClassType())
                .guaranteeFlag(entity.getGuaranteeFlag())
                .guaranteedBy(entity.getGuaranteedBy())
                .extraFreeDays(entity.getExtraFreeDays())
                .extraFreeDaysPrnpls(entity.getExtraFreeDaysPrnpls())
                .oogLW(entity.getOogLW())
                .oogRW(entity.getOogRW())
                .oogF(entity.getOogF())
                .oogA(entity.getOogA())
                .oogType(entity.getOogType())
                .imcoClassActual(entity.getImcoClassActual())
                .totalDaysCollected(entity.getTotalDaysCollected())
                .totalAmountCollected(entity.getTotalAmountCollected())
                .printReturnFormDefault(entity.getPrintReturnFormDefault())
                .printDeliveryFormDefault(entity.getPrintDeliveryFormDefault())
                .demDttPbleTrnsfd(entity.getDemDttPbleTrnsfd())
                .hsCode(entity.getHsCode())
                .hsDescription(entity.getHsDescription())
                .amountPerDayAfterFree(entity.getAmountPerDayAfterFree())
                .build();
    }


    public ShipBlManifestContainerDtl mapContainerDtlFromDto(
            ContainerRequestDto dto,
            Long transactionPoid
    ) {
        if (dto == null) return null;

        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );

        return ShipBlManifestContainerDtl.builder()
                .id(id)
                .mateTransactionPoid(dto.getMateTransactionPoid())
                .containerNo(dto.getContainerNo())
                .equipmentShipperOwn(dto.getEquipmentShipperOwn())
                .cargoDescription(dto.getCargoDescription())
                .equipmentSealNo(dto.getEquipmentSealNo())
                .equipmentIsoType(dto.getEquipmentIsoType())
                .equipmentType(dto.getEquipmentType())
                .equipmentSize(dto.getEquipmentSize())
                .quantity(dto.getQuantity())
                .grsVolume(dto.getGrsVolume())
                .grsWeight(dto.getGrsWeight())
                .netVolume(dto.getNetVolume())
                .netWeight(dto.getNetWeight())
                .tareWeight(dto.getTareWeight())
                .noOfPacks(dto.getNoOfPacks())
                .packUnit(dto.getPackUnit())
                .comodityPoid(dto.getComodityPoid())
                .destinationPortPoid(dto.getDestinationPortPoid())
                .imo(dto.getImo())
                .oogL(dto.getOogL())
                .oogB(dto.getOogB())
                .oogH(dto.getOogH())
                .refferTemp(dto.getRefferTemp())
                .refferHum(dto.getRefferHum())
                .refferVent(dto.getRefferVent())

                .issueToConsignee(toLocalDateTime(dto.getIssueToConsignee()))
                .returnFromConsignee(toLocalDateTime(dto.getReturnFromConsignee()))
                .displayCollectedDate(toLocalDateTime(dto.getDisplayCollectedDate()))
                .actualDischargeDate(toLocalDateTime(dto.getActualDischargeDate()))

                .isImco(dto.getIsImco())
                .isOog(dto.getIsOog())
                .isRefer(dto.getIsRefer())
                .referType(dto.getReferType())
                .imcoClassType(dto.getImcoClassType())
                .guaranteeFlag(dto.getGuaranteeFlag())
                .guaranteedBy(dto.getGuaranteedBy())
                .extraFreeDays(dto.getExtraFreeDays())
                .extraFreeDaysPrnpls(dto.getExtraFreeDaysPrnpls())
                .oogLW(dto.getOogLW())
                .oogRW(dto.getOogRW())
                .oogF(dto.getOogF())
                .oogA(dto.getOogA())
                .oogType(dto.getOogType())
                .imcoClassActual(dto.getImcoClassActual())
                .totalDaysCollected(dto.getTotalDaysCollected())
                .totalAmountCollected(dto.getTotalAmountCollected())
                .printReturnFormDefault(dto.getPrintReturnFormDefault())
                .printDeliveryFormDefault(dto.getPrintDeliveryFormDefault())
                .demDttPbleTrnsfd(dto.getDemDttPbleTrnsfd())
                .hsCode(dto.getHsCode())
                .hsDescription(dto.getHsDescription())
                .amountPerDayAfterFree(dto.getAmountPerDayAfterFree())
                .build();
    }


    // Detail mapping methods - Charges DTL
    public ChargeRequestDto mapChargesDtlToDto(ShipBlManifestChargesDtl entity) {
        if (entity == null) return null;
        return ChargeRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .chargePoid(entity.getChargePoid())
                .currencyExchange(entity.getCurrencyExchange())
                .quantity(entity.getQuantity())
                .buyPercharge(entity.getBuyPercharge())
                .perQuantityAmount(entity.getPerQuantityAmount())
                .paidAtPortPoid(entity.getPaidAtPortPoid())
                .chargeType(entity.getChargeType())
                .currencyCode(entity.getCurrencyCode())
                .freightType(entity.getFreightType())
                .ediChargeCode(entity.getEdiChargeCode())
                .arShReceiptTransactionPoid(entity.getArShReceiptTransactionPoid())
                .chargeBasisOn(entity.getChargeBasisOn())
                .printGroup(entity.getPrintGroup())
                .receiptInvoicePoid(entity.getReceiptInvoicePoid())
                .docRefLinkNo(entity.getDocRefLinkNo())
                .reprintDetRowId(entity.getReprintDetRowId())
                .reprintTransactionPoid(entity.getReprintTransactionPoid())
                .invoiceType(entity.getInvoiceType())
                .autoCanInvoiceNo(entity.getAutoCanInvoiceNo())
                .chargeDescription(entity.getChargeDescription())
                .taxPoid(entity.getTaxPoid())
                .taxPercentage(entity.getTaxPercentage())
                .taxAmount(entity.getTaxAmount())
                .cnRefDocId(entity.getCnRefDocId())
                .cnRefDocPoid(entity.getCnRefDocPoid())
                .cnRefDetRowId(entity.getCnRefDetRowId())
                .cnIssueInvoice(entity.getCnIssueInvoice())
                .selectRow(entity.getSelectRow())
                .build();
    }

    public ShipBlManifestChargesDtl mapChargesDtlFromDto(ChargeRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );
        return ShipBlManifestChargesDtl.builder()
                .id(id)
                .chargePoid(dto.getChargePoid())
                .currencyExchange(dto.getCurrencyExchange())
                .quantity(dto.getQuantity())
                .buyPercharge(dto.getBuyPercharge())
                .perQuantityAmount(dto.getPerQuantityAmount())
                .paidAtPortPoid(dto.getPaidAtPortPoid())
                .chargeType(dto.getChargeType() != null ? dto.getChargeType() : "MANIFEST")
                .currencyCode(dto.getCurrencyCode())
                .freightType(dto.getFreightType())
                .ediChargeCode(dto.getEdiChargeCode())
                .arShReceiptTransactionPoid(dto.getArShReceiptTransactionPoid())
                .chargeBasisOn(dto.getChargeBasisOn())
                .printGroup(dto.getPrintGroup())
                .receiptInvoicePoid(dto.getReceiptInvoicePoid())
                .docRefLinkNo(dto.getDocRefLinkNo())
                .reprintDetRowId(dto.getReprintDetRowId())
                .reprintTransactionPoid(dto.getReprintTransactionPoid())
                .invoiceType(dto.getInvoiceType() != null ? dto.getInvoiceType() : "MANUAL")
                .autoCanInvoiceNo(dto.getAutoCanInvoiceNo())
                .chargeDescription(dto.getChargeDescription())
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .cnRefDocId(dto.getCnRefDocId())
                .cnRefDocPoid(dto.getCnRefDocPoid())
                .cnRefDetRowId(dto.getCnRefDetRowId())
                .cnIssueInvoice(dto.getCnIssueInvoice())
                .selectRow(dto.getSelectRow())
                .build();
    }

    // Detail mapping methods - Container PRT
    public PartBlRequestDto mapContainerPrtToDto(ShipBlManifestPartBL entity) {
        if (entity == null) return null;
        return PartBlRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .shipperName(entity.getShipperName())
                .consigneeName(entity.getConsigneeName())
                .containerNo(entity.getContainerNo())
                .cargoDescription(entity.getCargoDescription())
                .comodityPoid(entity.getComodityPoid())
                .netVolume(entity.getNetVolume())
                .netWeight(entity.getNetWeight())
                .noOfPacks(entity.getNoOfPacks())
                .packUnit(entity.getPackUnit())
                .partBlNumber(entity.getPartBlNumber())
                .build();
    }

    public ShipBlManifestPartBL mapContainerPrtFromDto(PartBlRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );
        return ShipBlManifestPartBL.builder()
                .id(id)
                .shipperName(dto.getShipperName())
                .consigneeName(dto.getConsigneeName())
                .containerNo(dto.getContainerNo())
                .cargoDescription(dto.getCargoDescription())
                .comodityPoid(dto.getComodityPoid())
                .netVolume(dto.getNetVolume())
                .netWeight(dto.getNetWeight())
                .noOfPacks(dto.getNoOfPacks())
                .packUnit(dto.getPackUnit())
                .partBlNumber(dto.getPartBlNumber())
                .build();
    }

    // Detail mapping methods - Email Fax DTL
    public NotifyPartyRequestDto mapEmailFaxDtlToDto(ShipBlManifestEmailFaxDtl entity) {
        if (entity == null) return null;
        return NotifyPartyRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .addressPoid(entity.getAddressPoid())
                .fax(entity.getFax())
                .email1(entity.getEmail1())
                .email2(entity.getEmail2())
                .sendEmailFax(entity.getSendEmailFax())
                .sendYesNo(entity.getSendYesNo())
                //.addressType(entity.getAddressType())
                .addressType(entity.getId().getAddressType())
                .faxLog(entity.getFaxLog())
                .emailLog(entity.getEmailLog())
                .build();
    }

    public ShipBlManifestEmailFaxDtl mapEmailFaxDtlFromDto(NotifyPartyRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;
        ShipBlManifestEmailFaxId id = new ShipBlManifestEmailFaxId(
                transactionPoid,
                dto.getDetRowId(),
                dto.getAddressType()
        );
        return ShipBlManifestEmailFaxDtl.builder()
                .id(id)
                .addressPoid(dto.getAddressPoid() != null ? dto.getAddressPoid() : 1L)
                .fax(dto.getFax())
                .email1(dto.getEmail1())
                .email2(dto.getEmail2())
                .sendEmailFax(dto.getSendEmailFax() != null ? dto.getSendEmailFax() : "BOTH")
                .sendYesNo(dto.getSendYesNo() != null ? dto.getSendYesNo() : "Y")
                .faxLog(dto.getFaxLog())
                .emailLog(dto.getEmailLog())
                .build();
    }

    // Detail mapping methods - MAFI DTL
    public MafiRequestDto mapMafiDtlToDto(ShipBlManifestMafiDtl entity) {
        if (entity == null) return null;
        return MafiRequestDto.builder()
                .detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
                .mafiRef(entity.getMafiRef())
                .remarks(entity.getRemarks())
                .mafiFreeDays(entity.getMafiFreeDays())
                .mafiSize(entity.getMafiSize())
                .mafiEmptyDate(entity.getMafiEmptyDate())
                .backLoadDate(entity.getBackLoadDate())
                .build();
    }

    public ShipBlManifestMafiDtl mapMafiDtlFromDto(MafiRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );
        return ShipBlManifestMafiDtl.builder()
                .id(id)
                .mafiRef(dto.getMafiRef())
                .remarks(dto.getRemarks())
                .mafiFreeDays(dto.getMafiFreeDays())
                .mafiSize(dto.getMafiSize())
                .mafiEmptyDate(dto.getMafiEmptyDate())
                .backLoadDate(dto.getBackLoadDate())
                .build();
    }

    // Helper methods to map lists
    public List<GeneralCargoRequestDto> mapGeneralDtlListToDto(List<ShipBlManifestGeneralDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapGeneralDtlToDto).collect(Collectors.toList());
    }

    public List<CargoDescriptionRequestDto> mapCargoDtlListToDto(List<ShipBlManifestCargoDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapCargoDtlToDto).collect(Collectors.toList());
    }

    public List<ContainerRequestDto> mapContainerDtlListToDto(List<ShipBlManifestContainerDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapContainerDtlToDto).collect(Collectors.toList());
    }

    public List<ChargeRequestDto> mapChargesDtlListToDto(List<ShipBlManifestChargesDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapChargesDtlToDto).collect(Collectors.toList());
    }

    public List<PartBlRequestDto> mapContainerPrtListToDto(List<ShipBlManifestPartBL> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapContainerPrtToDto).collect(Collectors.toList());
    }

    public List<NotifyPartyRequestDto> mapEmailFaxDtlListToDto(List<ShipBlManifestEmailFaxDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapEmailFaxDtlToDto).collect(Collectors.toList());
    }

    public List<MafiRequestDto> mapMafiDtlListToDto(List<ShipBlManifestMafiDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapMafiDtlToDto).collect(Collectors.toList());
    }

    public void updateGeneralFromDto(GeneralCargoRequestDto dto, ShipBlManifestGeneralDtl entity) {
        entity.setComodityPoid(dto.getComodityPoid());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setQuantity(dto.getQuantity());
        entity.setGrsVolume(dto.getGrsVolume());
        entity.setGrsWeight(dto.getGrsWeight());
        entity.setNetVolume(dto.getNetVolume());
        entity.setNetWeight(dto.getNetWeight());
        entity.setTareWeight(dto.getTareWeight());
        entity.setNoOfPacks(dto.getNoOfPacks());
        entity.setPackUnit(dto.getPackUnit());
        entity.setDestinationPortPoid(dto.getDestinationPortPoid());
    }

    public void updateChargesFromDto(ChargeRequestDto dto, ShipBlManifestChargesDtl entity) {
        entity.setChargePoid(dto.getChargePoid());
        entity.setCurrencyExchange(dto.getCurrencyExchange());
        entity.setQuantity(dto.getQuantity());
        entity.setBuyPercharge(dto.getBuyPercharge());
        entity.setPerQuantityAmount(dto.getPerQuantityAmount());
        entity.setPaidAtPortPoid(dto.getPaidAtPortPoid());
        entity.setChargeType(dto.getChargeType());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setFreightType(dto.getFreightType());
        entity.setEdiChargeCode(dto.getEdiChargeCode());
        entity.setArShReceiptTransactionPoid(dto.getArShReceiptTransactionPoid());
        entity.setChargeBasisOn(dto.getChargeBasisOn());
        entity.setPrintGroup(dto.getPrintGroup());
        entity.setReceiptInvoicePoid(dto.getReceiptInvoicePoid());
        entity.setDocRefLinkNo(dto.getDocRefLinkNo());
        entity.setReprintDetRowId(dto.getReprintDetRowId());
        entity.setReprintTransactionPoid(dto.getReprintTransactionPoid());
        entity.setInvoiceType(dto.getInvoiceType());
        entity.setAutoCanInvoiceNo(dto.getAutoCanInvoiceNo());
        entity.setChargeDescription(dto.getChargeDescription());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setTaxPercentage(dto.getTaxPercentage());
        entity.setTaxAmount(dto.getTaxAmount());
        entity.setCnRefDocId(dto.getCnRefDocId());
        entity.setCnRefDocPoid(dto.getCnRefDocPoid());
        entity.setCnRefDetRowId(dto.getCnRefDetRowId());
        entity.setCnIssueInvoice(dto.getCnIssueInvoice());
        entity.setSelectRow(dto.getSelectRow());
    }

    public void updatePartBlFromDto(PartBlRequestDto dto, ShipBlManifestPartBL entity) {
        entity.setShipperName(dto.getShipperName());
        entity.setConsigneeName(dto.getConsigneeName());
        entity.setContainerNo(dto.getContainerNo());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setComodityPoid(dto.getComodityPoid());
        entity.setNetVolume(dto.getNetVolume());
        entity.setNetWeight(dto.getNetWeight());
        entity.setNoOfPacks(dto.getNoOfPacks());
        entity.setPackUnit(dto.getPackUnit());
        entity.setPartBlNumber(dto.getPartBlNumber());
    }

    public void updateEmailFaxFromDto(NotifyPartyRequestDto dto, ShipBlManifestEmailFaxDtl entity) {
        entity.setAddressPoid(dto.getAddressPoid());
        entity.setFax(dto.getFax());
        entity.setEmail1(dto.getEmail1());
        entity.setEmail2(dto.getEmail2());
        entity.setSendEmailFax(dto.getSendEmailFax());
        entity.setSendYesNo(dto.getSendYesNo());
        entity.setFaxLog(dto.getFaxLog());
        entity.setEmailLog(dto.getEmailLog());
    }

    public void updateMafiFromDto(MafiRequestDto dto, ShipBlManifestMafiDtl entity) {
        entity.setMafiRef(dto.getMafiRef());
        entity.setRemarks(dto.getRemarks());
        entity.setMafiFreeDays(dto.getMafiFreeDays());
        entity.setMafiSize(dto.getMafiSize());
        entity.setMafiEmptyDate(dto.getMafiEmptyDate());
        entity.setBackLoadDate(dto.getBackLoadDate());
    }

    public void updateContainerFromDto(
            ContainerRequestDto dto,
            ShipBlManifestContainerDtl entity) {

        if (dto == null || entity == null) {
            return;
        }

        if (dto.getMateTransactionPoid() != null) {
            entity.setMateTransactionPoid(dto.getMateTransactionPoid());
        }

        if (dto.getContainerNo() != null) {
            entity.setContainerNo(dto.getContainerNo().trim());
        }

        entity.setEquipmentShipperOwn(dto.getEquipmentShipperOwn());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setEquipmentSealNo(dto.getEquipmentSealNo());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());
        entity.setEquipmentType(dto.getEquipmentType());
        entity.setEquipmentSize(dto.getEquipmentSize());
        entity.setQuantity(dto.getQuantity());
        entity.setGrsVolume(dto.getGrsVolume());
        entity.setGrsWeight(dto.getGrsWeight());
        entity.setNetVolume(dto.getNetVolume());
        entity.setNetWeight(dto.getNetWeight());
        entity.setTareWeight(dto.getTareWeight());
        entity.setNoOfPacks(dto.getNoOfPacks());
        entity.setPackUnit(dto.getPackUnit());
        entity.setComodityPoid(dto.getComodityPoid());
        entity.setDestinationPortPoid(dto.getDestinationPortPoid());
        entity.setImo(dto.getImo());
        entity.setOogL(dto.getOogL());
        entity.setOogB(dto.getOogB());
        entity.setOogH(dto.getOogH());
        entity.setRefferTemp(dto.getRefferTemp());
        entity.setRefferHum(dto.getRefferHum());
        entity.setRefferVent(dto.getRefferVent());

        entity.setIssueToConsignee(toLocalDateTime(dto.getIssueToConsignee()));
        entity.setReturnFromConsignee(toLocalDateTime(dto.getReturnFromConsignee()));
        entity.setActualDischargeDate(toLocalDateTime(dto.getActualDischargeDate()));

        entity.setIsImco(dto.getIsImco());
        entity.setIsOog(dto.getIsOog());
        entity.setIsRefer(dto.getIsRefer());
        entity.setReferType(dto.getReferType());
        entity.setImcoClassType(dto.getImcoClassType());
        entity.setGuaranteeFlag(dto.getGuaranteeFlag());
        entity.setGuaranteedBy(dto.getGuaranteedBy());
        entity.setExtraFreeDays(dto.getExtraFreeDays());
        entity.setExtraFreeDaysPrnpls(dto.getExtraFreeDaysPrnpls());
        entity.setOogLW(dto.getOogLW());
        entity.setOogRW(dto.getOogRW());
        entity.setOogF(dto.getOogF());
        entity.setOogA(dto.getOogA());
        entity.setOogType(dto.getOogType());
        entity.setImcoClassActual(dto.getImcoClassActual());
        entity.setPrintReturnFormDefault(dto.getPrintReturnFormDefault());
        entity.setPrintDeliveryFormDefault(dto.getPrintDeliveryFormDefault());
        entity.setHsCode(dto.getHsCode());
        entity.setHsDescription(dto.getHsDescription());
        entity.setAmountPerDayAfterFree(dto.getAmountPerDayAfterFree());
    }


    public static LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDateTime ldt -> ldt;
            case LocalDate ld -> ld.atStartOfDay();
            default -> throw new IllegalArgumentException("Unsupported date type: " + value.getClass());
        };

    }


    public static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }


}
