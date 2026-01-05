package com.asg.shipping.importManifestUpdate.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.entity.*;
import org.springframework.stereotype.Component;

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
     * Map CreateDTO to Header Entity
     */
  /*  public void mapCreateDTOToEntity(ImportManifestBlCreateDTO dto, ShipBlManifestHdr entity, Long groupPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
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
        entity.setPlaceOfRecieptPoid(dto.getPlaceOfRecieptPoid());
        entity.setPlaceOfDelieveryPoid(dto.getPlaceOfDelieveryPoid());
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
        entity.setBlPlaceDischareDesc(dto.getBlPlaceDischareDesc());
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

        // Set audit fields
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        // Set deleted flag
        entity.setDeleted("N");
    }*/

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
    public void mapUpdateDTOToEntity(ImportManifestBlUpdateDTO dto, ShipBlManifestHdr entity) {
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
        if (dto.getBlType() != null) {
            entity.setBlType(dto.getBlType());
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

        if(dto.getManifestEmailVerified()!= null){
            entity.setManifestEmailVerified(dto.getManifestEmailVerified());
        }

        if(dto.getEmailVerifiedWithSpecialC()!= null){
             entity.setEmailVerifiedWithSpecialC(dto.getEmailVerifiedWithSpecialC());
        }


        // Update audit fields
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
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
                .issueToConsignee(entity.getIssueToConsignee())
                .returnFromConsignee(entity.getReturnFromConsignee())
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
                .displayCollectedDate(entity.getDisplayCollectedDate())
                .totalDaysCollected(entity.getTotalDaysCollected())
                .totalAmountCollected(entity.getTotalAmountCollected())
                .printReturnFormDefault(entity.getPrintReturnFormDefault())
                .printDeliveryFormDefault(entity.getPrintDeliveryFormDefault())
                .demDttPbleTrnsfd(entity.getDemDttPbleTrnsfd())
                .hsCode(entity.getHsCode())
                .hsDescription(entity.getHsDescription())
                .amountPerDayAfterFree(entity.getAmountPerDayAfterFree())
                .actualDischargeDate(entity.getActualDischargeDate())
                .build();
    }

    public ShipBlManifestContainerDtl mapContainerDtlFromDto(ContainerRequestDto dto, Long transactionPoid) {
        if (dto == null) return null;

        ShipBlManifestDtlId id = new ShipBlManifestDtlId(
                transactionPoid,
                dto.getDetRowId()
        );
        return ShipBlManifestContainerDtl.builder()
                .id(id)
                .mateTransactionPoid(dto.getMateTransactionPoid())
                .containerNo(dto.getContainerNo() != null ? dto.getContainerNo().trim() : null)
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
                .issueToConsignee(dto.getIssueToConsignee())
                .returnFromConsignee(dto.getReturnFromConsignee())
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
                .displayCollectedDate(dto.getDisplayCollectedDate())
                .totalDaysCollected(dto.getTotalDaysCollected())
                .totalAmountCollected(dto.getTotalAmountCollected())
                .printReturnFormDefault(dto.getPrintReturnFormDefault())
                .printDeliveryFormDefault(dto.getPrintDeliveryFormDefault())
                .demDttPbleTrnsfd(dto.getDemDttPbleTrnsfd())
                .hsCode(dto.getHsCode())
                .hsDescription(dto.getHsDescription())
                .amountPerDayAfterFree(dto.getAmountPerDayAfterFree())
                .actualDischargeDate(dto.getActualDischargeDate())
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
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1L)
                .buyPercharge(dto.getBuyPercharge())
                .perQuantityAmount(dto.getPerQuantityAmount())
                .paidAtPortPoid(dto.getPaidAtPortPoid())
                .chargeType(dto.getChargeType() != null ? dto.getChargeType() : "MANIFEST")
                .currencyCode(dto.getCurrencyCode())
                .freightType(dto.getFreightType() != null ? dto.getFreightType() : "FREIGHT")
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
}
