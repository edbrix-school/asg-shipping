package com.asg.shipping.exportManifestBl.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlCreateDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlRequestDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlUpdateDto;
import com.asg.shipping.exportManifestBl.entity.ExportManifestBlHdr;
import org.springframework.stereotype.Component;

@Component
public class ExportManifestBlMapper {

    /**
     * Convert Header Entity to DTO
     */
    public ExportManifestBlRequestDto mapToDto(ExportManifestBlHdr entity) {
        if (entity == null) {
            return null;
        }

        return ExportManifestBlRequestDto.builder()
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
    public ExportManifestBlHdr mapToEntity(ExportManifestBlCreateDto dto) {
        if (dto == null) return null;
        return ExportManifestBlHdr.builder()
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
                .blType("EXPORT") // Always set to EXPORT for Export Manifest
                .cargoType(dto.getCargoType())
                .doNo(dto.getDoNo())
                .demFreeDays(dto.getDemFreeDays())
                .blIssueType(dto.getBlIssueType())
                .blOrginalPrint(dto.getBlOrginalPrint())
                .blOrginalDate(dto.getBlOrginalDate())
                .blPrintedBy(dto.getBlPrintedBy())
                .releasedStatus(dto.getReleasedStatus())
                .releasedDate(dto.getReleasedDate())
                .relasedToPerson(dto.getRelasedToPerson())
                .relasedIdPerson(dto.getRelasedIdPerson())
                .relasedAddrsPerson(dto.getRelasedAddrsPerson())
                .relasedBy(dto.getRelasedBy())
                .openDaysAfter(dto.getOpenDaysAfter())
                .releasedType(dto.getReleasedType())
                .relasedSeqno(dto.getRelasedSeqno())
                .releasedGrantBy(dto.getReleasedGrantBy())
                .releasedGrantDate(dto.getReleasedGrantDate())
                .releasedGrantReason(dto.getReleasedGrantReason())
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
                .manifestEmailVerified(dto.getManifestEmailVerified())
                .emailVerifiedWithSpecialC(dto.getEmailVerifiedWithSpecialC())
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
     * Map UpdateDTO to Header Entity - partial update
     */
    public void mapUpdateDTOToEntity(ExportManifestBlUpdateDto dto, ExportManifestBlHdr entity) {
        if (dto == null || entity == null) return;

        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getVoyageTransactionPoid() != null) {
            entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        }
        if (dto.getBlNumber() != null) {
            entity.setBlNumber(dto.getBlNumber().trim());
        }
        if (dto.getAgentReference() != null) {
            entity.setAgentReference(dto.getAgentReference());
        }
        if (dto.getShipperPoid() != null) {
            entity.setShipperPoid(dto.getShipperPoid());
        }
        if (dto.getShipperAddressPoid() != null) {
            entity.setShipperAddressPoid(dto.getShipperAddressPoid());
        }
        if (dto.getConsigneePoid() != null) {
            entity.setConsigneePoid(dto.getConsigneePoid());
        }
        if (dto.getConsigneeAddressPoid() != null) {
            entity.setConsigneeAddressPoid(dto.getConsigneeAddressPoid());
        }
        if (dto.getNotifyPoid1() != null) {
            entity.setNotifyPoid1(dto.getNotifyPoid1());
        }
        if (dto.getNotifyAddressPoid1() != null) {
            entity.setNotifyAddressPoid1(dto.getNotifyAddressPoid1());
        }
        if (dto.getNotifyPoid2() != null) {
            entity.setNotifyPoid2(dto.getNotifyPoid2());
        }
        if (dto.getNotifyAddressPoid2() != null) {
            entity.setNotifyAddressPoid2(dto.getNotifyAddressPoid2());
        }
        if (dto.getNotifyPoid3() != null) {
            entity.setNotifyPoid3(dto.getNotifyPoid3());
        }
        if (dto.getNotifyAddressPoid3() != null) {
            entity.setNotifyAddressPoid3(dto.getNotifyAddressPoid3());
        }
        if (dto.getQuotationTransactionPoid() != null) {
            entity.setQuotationTransactionPoid(dto.getQuotationTransactionPoid());
        }
        if (dto.getSalesmanPoid() != null) {
            entity.setSalesmanPoid(dto.getSalesmanPoid());
        }
        if (dto.getComodityPoid() != null) {
            entity.setComodityPoid(dto.getComodityPoid());
        }
        if (dto.getNoOfOrgnlBls() != null) {
            entity.setNoOfOrgnlBls(dto.getNoOfOrgnlBls());
        }
        if (dto.getExportReference() != null) {
            entity.setExportReference(dto.getExportReference());
        }
        if (dto.getLpoSrnNo() != null) {
            entity.setLpoSrnNo(dto.getLpoSrnNo());
        }
        if (dto.getLpoSrnDate() != null) {
            entity.setLpoSrnDate(dto.getLpoSrnDate());
        }
        if (dto.getTypeOfMove() != null) {
            entity.setTypeOfMove(dto.getTypeOfMove());
        }
        if (dto.getPreCarriedBy() != null) {
            entity.setPreCarriedBy(dto.getPreCarriedBy());
        }
        if (dto.getPlaceOfIssuePoid() != null) {
            entity.setPlaceOfIssuePoid(dto.getPlaceOfIssuePoid());
        }
        if (dto.getDateOfIssue() != null) {
            entity.setDateOfIssue(dto.getDateOfIssue());
        }
        if (dto.getPrintFreightDetails() != null) {
            entity.setPrintFreightDetails(dto.getPrintFreightDetails());
        }
        if (dto.getTotalVolume() != null) {
            entity.setTotalVolume(dto.getTotalVolume());
        }
        if (dto.getTotalNetVolume() != null) {
            entity.setTotalNetVolume(dto.getTotalNetVolume());
        }
        if (dto.getTotalWeight() != null) {
            entity.setTotalWeight(dto.getTotalWeight());
        }
        if (dto.getTotalNetWeight() != null) {
            entity.setTotalNetWeight(dto.getTotalNetWeight());
        }
        if (dto.getWeightUnit() != null) {
            entity.setWeightUnit(dto.getWeightUnit());
        }
        if (dto.getUnitPack() != null) {
            entity.setUnitPack(dto.getUnitPack());
        }
        if (dto.getTotalNoOfPacks() != null) {
            entity.setTotalNoOfPacks(dto.getTotalNoOfPacks());
        }
        if (dto.getPlaceOfRecieptPoid() != null) {
            entity.setPlaceOfReceiptPoid(dto.getPlaceOfRecieptPoid());
        }
        if (dto.getPlaceOfDelieveryPoid() != null) {
            entity.setPlaceOfDeliveryPoid(dto.getPlaceOfDelieveryPoid());
        }
        if (dto.getPortOfLoadingPoid() != null) {
            entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
        }
        if (dto.getPortOfDischargePoid() != null) {
            entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
        }
        if (dto.getRemarks() != null) {
            entity.setRemarks(dto.getRemarks());
        }
        if (dto.getBlStatus() != null) {
            entity.setBlStatus(dto.getBlStatus());
        }
        if (dto.getCargoType() != null) {
            entity.setCargoType(dto.getCargoType());
        }
        if (dto.getDoNo() != null) {
            entity.setDoNo(dto.getDoNo());
        }
        if (dto.getBlIssueType() != null) {
            entity.setBlIssueType(dto.getBlIssueType());
        }

        // Header fields that were missing in UPDATE mapping
        if (dto.getDemFreeDays() != null) {
            entity.setDemFreeDays(dto.getDemFreeDays());
        }
        if (dto.getBlOrginalPrint() != null) {
            entity.setBlOrginalPrint(dto.getBlOrginalPrint());
        }
        if (dto.getBlOrginalDate() != null) {
            entity.setBlOrginalDate(dto.getBlOrginalDate());
        }
        if (dto.getBlPrintedBy() != null) {
            entity.setBlPrintedBy(dto.getBlPrintedBy());
        }
        if (dto.getReleasedStatus() != null) {
            entity.setReleasedStatus(dto.getReleasedStatus());
        }
        if (dto.getReleasedDate() != null) {
            entity.setReleasedDate(dto.getReleasedDate());
        }
        if (dto.getRelasedToPerson() != null) {
            entity.setRelasedToPerson(dto.getRelasedToPerson());
        }
        if (dto.getRelasedIdPerson() != null) {
            entity.setRelasedIdPerson(dto.getRelasedIdPerson());
        }
        if (dto.getRelasedAddrsPerson() != null) {
            entity.setRelasedAddrsPerson(dto.getRelasedAddrsPerson());
        }
        if (dto.getRelasedBy() != null) {
            entity.setRelasedBy(dto.getRelasedBy());
        }
        if (dto.getOpenDaysAfter() != null) {
            entity.setOpenDaysAfter(dto.getOpenDaysAfter());
        }
        if (dto.getReleasedType() != null) {
            entity.setReleasedType(dto.getReleasedType());
        }
        if (dto.getRelasedSeqno() != null) {
            entity.setRelasedSeqno(dto.getRelasedSeqno());
        }
        if (dto.getReleasedGrantBy() != null) {
            entity.setReleasedGrantBy(dto.getReleasedGrantBy());
        }
        if (dto.getReleasedGrantDate() != null) {
            entity.setReleasedGrantDate(dto.getReleasedGrantDate());
        }
        if (dto.getReleasedGrantReason() != null) {
            entity.setReleasedGrantReason(dto.getReleasedGrantReason());
        }
        if (dto.getManifestEmailVerified() != null) {
            entity.setManifestEmailVerified(dto.getManifestEmailVerified());
        }
        if (dto.getEmailVerifiedWithSpecialC() != null) {
            entity.setEmailVerifiedWithSpecialC(dto.getEmailVerifiedWithSpecialC());
        }
        // EDI fields
        if (dto.getShipperEdiName() != null) {
            entity.setShipperEdiName(dto.getShipperEdiName());
        }
        if (dto.getShipperEdiAddress() != null) {
            entity.setShipperEdiAddress(dto.getShipperEdiAddress());
        }
        if (dto.getConsigneeEdiName() != null) {
            entity.setConsigneeEdiName(dto.getConsigneeEdiName());
        }
        if (dto.getConsigneeEdiAddress() != null) {
            entity.setConsigneeEdiAddress(dto.getConsigneeEdiAddress());
        }
        if (dto.getNotify1EdiName() != null) {
            entity.setNotify1EdiName(dto.getNotify1EdiName());
        }
        if (dto.getNotify1EdiAddress() != null) {
            entity.setNotify1EdiAddress(dto.getNotify1EdiAddress());
        }
        if (dto.getNotify2EdiName() != null) {
            entity.setNotify2EdiName(dto.getNotify2EdiName());
        }
        if (dto.getNotify2EdiAddress() != null) {
            entity.setNotify2EdiAddress(dto.getNotify2EdiAddress());
        }
        if (dto.getNotify3EdiName() != null) {
            entity.setNotify3EdiName(dto.getNotify3EdiName());
        }
        if (dto.getNotify3EdiAddress() != null) {
            entity.setNotify3EdiAddress(dto.getNotify3EdiAddress());
        }
        if (dto.getCanNotifyCustomerPoid() != null) {
            entity.setCanNotifyCustomerPoid(dto.getCanNotifyCustomerPoid());
        }
        if (dto.getBookedBy() != null) {
            entity.setBookedBy(dto.getBookedBy());
        }
        if (dto.getFreightStatus() != null) {
            entity.setFreightStatus(dto.getFreightStatus());
        }
        if (dto.getHoldCanDo() != null) {
            entity.setHoldCanDo(dto.getHoldCanDo());
        }
        if (dto.getHoldReason() != null) {
            entity.setHoldReason(dto.getHoldReason());
        }
        if (dto.getDocumentCompanyPoid() != null) {
            entity.setDocumentCompanyPoid(dto.getDocumentCompanyPoid());
        }
        if (dto.getDocumentCompanyDivisionPoid() != null) {
            entity.setDocumentCompanyDivisionPoid(dto.getDocumentCompanyDivisionPoid());
        }
        if (dto.getBlPlaceReceipt() != null) {
            entity.setBlPlaceReceipt(dto.getBlPlaceReceipt());
        }
        if (dto.getBlPlaceLoad() != null) {
            entity.setBlPlaceLoad(dto.getBlPlaceLoad());
        }
        if (dto.getBlFinalDestination() != null) {
            entity.setBlFinalDestination(dto.getBlFinalDestination());
        }
        if (dto.getBookingPartyPoid() != null) {
            entity.setBookingPartyPoid(dto.getBookingPartyPoid());
        }
        if (dto.getBlPlaceDischareDesc() != null) {
            entity.setBlPlaceDischargeDesc(dto.getBlPlaceDischareDesc());
        }
        if (dto.getCargoArrivalNumber() != null) {
            entity.setCargoArrivalNumber(dto.getCargoArrivalNumber());
        }
        if (dto.getBookedByPp() != null) {
            entity.setBookedByPp(dto.getBookedByPp());
        }
        if (dto.getManuallyCanSend() != null) {
            entity.setManuallyCanSend(dto.getManuallyCanSend());
        }
        if (dto.getAllInOneFreight() != null) {
            entity.setAllInOneFreight(dto.getAllInOneFreight());
        }
        if (dto.getHoldRemarks() != null) {
            entity.setHoldRemarks(dto.getHoldRemarks());
        }
        if (dto.getBlConsigneeAddressAdd() != null) {
            entity.setBlConsigneeAddressAdd(dto.getBlConsigneeAddressAdd());
        }
        if (dto.getAvoidCargoAlert() != null) {
            entity.setAvoidCargoAlert(dto.getAvoidCargoAlert());
        }
        if (dto.getAgentPoid() != null) {
            entity.setAgentPoid(dto.getAgentPoid());
        }
        if (dto.getFfJobNoHold() != null) {
            entity.setFfJobNoHold(dto.getFfJobNoHold());
        }
        if (dto.getIssueManualInvoice() != null) {
            entity.setIssueManualInvoice(dto.getIssueManualInvoice());
        }
        if (dto.getDoPriority() != null) {
            entity.setDoPriority(dto.getDoPriority());
        }
        if (dto.getDoIssueAuth() != null) {
            entity.setDoIssueAuth(dto.getDoIssueAuth());
        }
        if (dto.getDoIssueAuthPoid() != null) {
            entity.setDoIssueAuthPoid(dto.getDoIssueAuthPoid());
        }
        if (dto.getDoCntToConsignee() != null) {
            entity.setDoCntToConsignee(dto.getDoCntToConsignee());
        }
        if (dto.getDoCntToNotify() != null) {
            entity.setDoCntToNotify(dto.getDoCntToNotify());
        }
        if (dto.getDoCntToOthers() != null) {
            entity.setDoCntToOthers(dto.getDoCntToOthers());
        }
        if (dto.getDoCntToOthersMails() != null) {
            entity.setDoCntToOthersMails(dto.getDoCntToOthersMails());
        }
        if (dto.getDoCntReasonFailure() != null) {
            entity.setDoCntReasonFailure(dto.getDoCntReasonFailure());
        }
        if (dto.getDoCntToRegsMails() != null) {
            entity.setDoCntToRegsMails(dto.getDoCntToRegsMails());
        }
        if (dto.getDeliverySentTo() != null) {
            entity.setDeliverySentTo(dto.getDeliverySentTo());
        }
        if (dto.getFfBillToPoid() != null) {
            entity.setFfBillToPoid(dto.getFfBillToPoid());
        }
        if (dto.getDemActualNextDay() != null) {
            entity.setDemActualNextDay(dto.getDemActualNextDay());
        }
        if (dto.getPrincipalDoNumber() != null) {
            entity.setPrincipalDoNumber(dto.getPrincipalDoNumber());
        }
        if (dto.getStopUcanAlert() != null) {
            entity.setStopUcanAlert(dto.getStopUcanAlert());
        }
        if (dto.getIsMbl() != null) {
            entity.setIsMbl(dto.getIsMbl());
        }
        if (dto.getForwarderPin() != null) {
            entity.setForwarderPin(dto.getForwarderPin());
        }
        
        // Always keep BL_TYPE as 'EXPORT'
        entity.setBlType("EXPORT");
    }
}

