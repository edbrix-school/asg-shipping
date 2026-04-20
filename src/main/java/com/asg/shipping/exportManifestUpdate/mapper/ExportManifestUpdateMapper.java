package com.asg.shipping.exportManifestUpdate.mapper;

import com.asg.shipping.exportManifestUpdate.dto.*;
import com.asg.shipping.exportManifestUpdate.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between Entity and DTO for Export Manifest BL
 */
@Component
public class ExportManifestUpdateMapper {

    /**
     * Convert Header Entity to Response DTO
     */
    public ExportManifestBlResponse mapToResponse(ExportShipBlManifestHdr entity) {
        if (entity == null) {
            return null;
        }

        ExportManifestBlResponse response = new ExportManifestBlResponse();
        response.setTransactionPoid(entity.getTransactionPoid());
        response.setGroupPoid(entity.getGroupPoid());
        response.setCompanyPoid(entity.getCompanyPoid());
        response.setTransactionDate(entity.getTransactionDate());
        response.setVoyageTransactionPoid(entity.getVoyageTransactionPoid());
        response.setBlNumber(entity.getBlNumber());
        response.setAgentReference(entity.getAgentReference());
        response.setShipperPoid(entity.getShipperPoid());
        response.setShipperAddressPoid(entity.getShipperAddressPoid());
        response.setConsigneePoid(entity.getConsigneePoid());
        response.setConsigneeAddressPoid(entity.getConsigneeAddressPoid());
        response.setNotifyPoid1(entity.getNotifyPoid1());
        response.setNotifyAddressPoid1(entity.getNotifyAddressPoid1());
        response.setNotifyPoid2(entity.getNotifyPoid2());
        response.setNotifyAddressPoid2(entity.getNotifyAddressPoid2());
        response.setQuotationTransactionPoid(entity.getQuotationTransactionPoid());
        response.setSalesmanPoid(entity.getSalesmanPoid());
        response.setComodityPoid(entity.getComodityPoid());
        response.setNoOfOrgnlBls(entity.getNoOfOrgnlBls());
        response.setExportReference(entity.getExportReference());
        response.setLpoSrnNo(entity.getLpoSrnNo());
        response.setLpoSrnDate(entity.getLpoSrnDate());
        response.setTypeOfMove(entity.getTypeOfMove());
        response.setPreCarriedBy(entity.getPreCarriedBy());
        response.setPlaceOfIssuePoid(entity.getPlaceOfIssuePoid());
        response.setDateOfIssue(entity.getDateOfIssue());
        response.setPrintFreightDetails(entity.getPrintFreightDetails());
        response.setTotalVolume(entity.getTotalVolume());
        response.setTotalNetVolume(entity.getTotalNetVolume());
        response.setTotalWeight(entity.getTotalWeight());
        response.setTotalNetWeight(entity.getTotalNetWeight());
        response.setWeightUnit(entity.getWeightUnit());
        response.setUnitPack(entity.getUnitPack());
        response.setTotalNoOfPacks(entity.getTotalNoOfPacks());
        response.setPlaceOfRecieptPoid(entity.getPlaceOfRecieptPoid());
        response.setPlaceOfDelieveryPoid(entity.getPlaceOfDelieveryPoid());
        response.setPortOfLoadingPoid(entity.getPortOfLoadingPoid());
        response.setPortOfDischargePoid(entity.getPortOfDischargePoid());
        response.setRemarks(entity.getRemarks());
        response.setBlStatus(entity.getBlStatus());
        response.setBlOrginalPrint(entity.getBlOrginalPrint());
        response.setBlOrginalDate(entity.getBlOrginalDate());
        response.setBlPrintedBy(entity.getBlPrintedBy());
        response.setUniqueBlno(entity.getUniqueBlno());
        response.setDemRate(entity.getDemRate());
        response.setDemFreeDays(entity.getDemFreeDays());
        response.setReleasedStatus(entity.getReleasedStatus());
        response.setReleasedDate(entity.getReleasedDate());
        response.setRelasedToPerson(entity.getRelasedToPerson());
        response.setRelasedIdPerson(entity.getRelasedIdPerson());
        response.setRelasedAddrsPerson(entity.getRelasedAddrsPerson());
        response.setRelasedBy(entity.getRelasedBy());
        response.setOpenDaysAfter(entity.getOpenDaysAfter());
        response.setReleasedType(entity.getReleasedType());
        response.setRelasedSeqno(entity.getRelasedSeqno());
        response.setReleasedGrantBy(entity.getReleasedGrantBy());
        response.setReleasedGrantDate(entity.getReleasedGrantDate());
        response.setReleasedGrantReason(entity.getReleasedGrantReason());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedBy(entity.getLastModifiedBy());
        response.setLastModifiedDate(entity.getLastModifiedDate());
        response.setCargoType(entity.getCargoType());
        response.setBlType(entity.getBlType());
        response.setDoNo(entity.getDoNo());
        response.setDocRef(entity.getDocRef());
        response.setDeleted(entity.getDeleted());
        response.setBlIssueType(entity.getBlIssueType());
        response.setNotifyPoid3(entity.getNotifyPoid3());
        response.setNotifyAddressPoid3(entity.getNotifyAddressPoid3());
        response.setShipperEdiName(entity.getShipperEdiName());
        response.setShipperEdiAddress(entity.getShipperEdiAddress());
        response.setConsigneeEdiName(entity.getConsigneeEdiName());
        response.setConsigneeEdiAddress(entity.getConsigneeEdiAddress());
        response.setNotify1EdiName(entity.getNotify1EdiName());
        response.setNotify1EdiAddress(entity.getNotify1EdiAddress());
        response.setNotify2EdiName(entity.getNotify2EdiName());
        response.setNotify2EdiAddress(entity.getNotify2EdiAddress());
        response.setNotify3EdiName(entity.getNotify3EdiName());
        response.setNotify3EdiAddress(entity.getNotify3EdiAddress());
        response.setCanNotifyCustomerPoid(entity.getCanNotifyCustomerPoid());
        response.setBookedBy(entity.getBookedBy());
        response.setFreightStatus(entity.getFreightStatus());
        response.setHoldCanDo(entity.getHoldCanDo());
        response.setHoldReason(entity.getHoldReason());
        response.setCanSentQueue(entity.getCanSentQueue());
        response.setCanSentDate(entity.getCanSentDate());
        response.setCanSentBy(entity.getCanSentBy());
        response.setDocumentCompanyPoid(entity.getDocumentCompanyPoid());
        response.setDocumentCompanyDivisionPoid(entity.getDocumentCompanyDivisionPoid());
        response.setBlPlaceReceipt(entity.getBlPlaceReceipt());
        response.setBlPlaceLoad(entity.getBlPlaceLoad());
        response.setBlFinalDestination(entity.getBlFinalDestination());
        response.setBookingPartyPoid(entity.getBookingPartyPoid());
        response.setBlPlaceDischareDesc(entity.getBlPlaceDischareDesc());
        response.setCargoArrivalNumber(entity.getCargoArrivalNumber());
        response.setBookedByPp(entity.getBookedByPp());
        response.setManuallyCanSend(entity.getManuallyCanSend());
        response.setAllInOneFreight(entity.getAllInOneFreight());
        response.setHoldRemarks(entity.getHoldRemarks());
        response.setBlConsigneeAddressAdd(entity.getBlConsigneeAddressAdd());
        response.setAvoidCargoAlert(entity.getAvoidCargoAlert());
        response.setAgentPoid(entity.getAgentPoid());

        return response;
    }

    /**
     * Map Request DTO to Entity (for create/update)
     */
    public void mapRequestToEntity(ExportManifestBlRequest request, ExportShipBlManifestHdr entity, Long groupPoid, Long companyPoid, String userId) {
        if (request == null || entity == null) {
            return;
        }

        // Only map non-null fields from request
        if (request.getVoyageTransactionPoid() != null) {
            entity.setVoyageTransactionPoid(request.getVoyageTransactionPoid());
        }
        if (request.getBlType() != null) {
            entity.setBlType(request.getBlType());
        }
        if (request.getCargoType() != null) {
            entity.setCargoType(request.getCargoType());
        }
        if (request.getQuotationTransactionPoid() != null) {
            entity.setQuotationTransactionPoid(request.getQuotationTransactionPoid());
        }
        if (request.getSalesmanPoid() != null) {
            entity.setSalesmanPoid(request.getSalesmanPoid());
        }
        if (request.getBlNumber() != null) {
            entity.setBlNumber(request.getBlNumber().trim());
        }
        if (request.getNoOfOrgnlBls() != null) {
            entity.setNoOfOrgnlBls(request.getNoOfOrgnlBls());
        }
        if (request.getFreightStatus() != null) {
            entity.setFreightStatus(request.getFreightStatus());
        }
        if (request.getTypeOfMove() != null) {
            entity.setTypeOfMove(request.getTypeOfMove());
        }
        if (request.getDemFreeDays() != null) {
            entity.setDemFreeDays(request.getDemFreeDays());
        }
        if (request.getDemRate() != null) {
            entity.setDemRate(request.getDemRate());
        }
        if (request.getBookedByPp() != null) {
            entity.setBookedByPp(request.getBookedByPp());
        }
        if (request.getAgentReference() != null) {
            entity.setAgentReference(request.getAgentReference());
        }
        if (request.getExportReference() != null) {
            entity.setExportReference(request.getExportReference());
        }
        if (request.getRemarks() != null) {
            entity.setRemarks(request.getRemarks());
        }
        if (request.getComodityPoid() != null) {
            entity.setComodityPoid(request.getComodityPoid());
        }
        if (request.getTotalNetVolume() != null) {
            entity.setTotalNetVolume(request.getTotalNetVolume());
        }
        if (request.getTotalWeight() != null) {
            entity.setTotalWeight(request.getTotalWeight());
        }
        if (request.getTotalNetWeight() != null) {
            entity.setTotalNetWeight(request.getTotalNetWeight());
        }
        if (request.getWeightUnit() != null) {
            entity.setWeightUnit(request.getWeightUnit());
        }
        if (request.getUnitPack() != null) {
            entity.setUnitPack(request.getUnitPack());
        }
        if (request.getTotalNoOfPacks() != null) {
            entity.setTotalNoOfPacks(request.getTotalNoOfPacks());
        }
        if (request.getBlIssueType() != null) {
            entity.setBlIssueType(request.getBlIssueType());
        }
        if (request.getDocumentCompanyPoid() != null) {
            entity.setDocumentCompanyPoid(request.getDocumentCompanyPoid());
        }
        if (request.getDocumentCompanyDivisionPoid() != null) {
            entity.setDocumentCompanyDivisionPoid(request.getDocumentCompanyDivisionPoid());
        }
        if (request.getPortOfLoadingPoid() != null) {
            entity.setPortOfLoadingPoid(request.getPortOfLoadingPoid());
        }
        if (request.getPlaceOfRecieptPoid() != null) {
            entity.setPlaceOfRecieptPoid(request.getPlaceOfRecieptPoid());
        }
        if (request.getPortOfDischargePoid() != null) {
            entity.setPortOfDischargePoid(request.getPortOfDischargePoid());
        }
        if (request.getPlaceOfDelieveryPoid() != null) {
            entity.setPlaceOfDelieveryPoid(request.getPlaceOfDelieveryPoid());
        }
        if (request.getBlPlaceReceipt() != null) {
            entity.setBlPlaceReceipt(request.getBlPlaceReceipt());
        }
        if (request.getBlPlaceLoad() != null) {
            entity.setBlPlaceLoad(request.getBlPlaceLoad());
        }
        if (request.getBlFinalDestination() != null) {
            entity.setBlFinalDestination(request.getBlFinalDestination());
        }
        if (request.getBlPlaceDischareDesc() != null) {
            entity.setBlPlaceDischareDesc(request.getBlPlaceDischareDesc());
        }
        if (request.getShipperPoid() != null) {
            entity.setShipperPoid(request.getShipperPoid());
        }
        if (request.getShipperAddressPoid() != null) {
            entity.setShipperAddressPoid(request.getShipperAddressPoid());
        }
        if (request.getConsigneePoid() != null) {
            entity.setConsigneePoid(request.getConsigneePoid());
        }
        if (request.getConsigneeAddressPoid() != null) {
            entity.setConsigneeAddressPoid(request.getConsigneeAddressPoid());
        }
        if (request.getNotifyPoid1() != null) {
            entity.setNotifyPoid1(request.getNotifyPoid1());
        }
        if (request.getNotifyAddressPoid1() != null) {
            entity.setNotifyAddressPoid1(request.getNotifyAddressPoid1());
        }
        if (request.getNotifyPoid2() != null) {
            entity.setNotifyPoid2(request.getNotifyPoid2());
        }
        if (request.getNotifyAddressPoid2() != null) {
            entity.setNotifyAddressPoid2(request.getNotifyAddressPoid2());
        }
        if (request.getBookingPartyPoid() != null) {
            entity.setBookingPartyPoid(request.getBookingPartyPoid());
        }
        if (request.getShipperEdiName() != null) {
            entity.setShipperEdiName(request.getShipperEdiName());
        }
        if (request.getShipperEdiAddress() != null) {
            entity.setShipperEdiAddress(request.getShipperEdiAddress());
        }
        if (request.getConsigneeEdiName() != null) {
            entity.setConsigneeEdiName(request.getConsigneeEdiName());
        }
        if (request.getConsigneeEdiAddress() != null) {
            entity.setConsigneeEdiAddress(request.getConsigneeEdiAddress());
        }
        if (request.getNotify1EdiName() != null) {
            entity.setNotify1EdiName(request.getNotify1EdiName());
        }
        if (request.getNotify1EdiAddress() != null) {
            entity.setNotify1EdiAddress(request.getNotify1EdiAddress());
        }
        if (request.getNotify2EdiName() != null) {
            entity.setNotify2EdiName(request.getNotify2EdiName());
        }
        if (request.getNotify2EdiAddress() != null) {
            entity.setNotify2EdiAddress(request.getNotify2EdiAddress());
        }
        if (request.getAllInOneFreight() != null) {
            entity.setAllInOneFreight(request.getAllInOneFreight());
        }
        if (request.getLpoSrnNo() != null) {
            entity.setLpoSrnNo(request.getLpoSrnNo());
        }
        if (request.getLpoSrnDate() != null) {
            entity.setLpoSrnDate(request.getLpoSrnDate());
        }
        if (request.getPreCarriedBy() != null) {
            entity.setPreCarriedBy(request.getPreCarriedBy());
        }
        if (request.getPlaceOfIssuePoid() != null) {
            entity.setPlaceOfIssuePoid(request.getPlaceOfIssuePoid());
        }
        if (request.getDateOfIssue() != null) {
            entity.setDateOfIssue(request.getDateOfIssue());
        }
        if (request.getPrintFreightDetails() != null) {
            entity.setPrintFreightDetails(request.getPrintFreightDetails());
        }
        if (request.getTotalVolume() != null) {
            entity.setTotalVolume(request.getTotalVolume());
        }

        // Set audit fields
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
    }

    /**
     * Convert General Cargo Detail Entity to DTO
     */
    public GeneralCargoDetailDto mapGeneralCargoToDto(ExportShipBlManifestGeneralDtl entity) {
        if (entity == null) {
            return null;
        }

        GeneralCargoDetailDto dto = new GeneralCargoDetailDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setComodityPoid(entity.getComodityPoid());
        dto.setCargoDescription(entity.getCargoDescription());
        dto.setQuantity(entity.getQuantity());
        dto.setGrsVolume(entity.getGrsVolume());
        dto.setGrsWeight(entity.getGrsWeight());
        dto.setNetVolume(entity.getNetVolume());
        dto.setNetWeight(entity.getNetWeight());
        dto.setTareWeight(entity.getTareWeight());
        dto.setNoOfPacks(entity.getNoOfPacks());
        dto.setPackUnit(entity.getPackUnit());
        dto.setDestinationPortPoid(entity.getDestinationPortPoid());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    /**
     * Convert General Cargo Detail DTO to Entity
     */
    public ExportShipBlManifestGeneralDtl mapGeneralCargoToEntity(GeneralCargoDetailDto dto, Long transactionPoid, Long detRowId, String userId) {
        if (dto == null) {
            return null;
        }

        ExportShipBlManifestGeneralDtl entity = new ExportShipBlManifestGeneralDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId != null ? detRowId : 0L);
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
        return entity;
    }

    /**
     * Convert Container Detail Entity to DTO
     */
    public ContainerDetailDto mapContainerToDto(ExportShipBlManifestContainerDtl entity) {
        if (entity == null) {
            return null;
        }

        ContainerDetailDto dto = new ContainerDetailDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setMateTransactionPoid(entity.getMateTransactionPoid());
        dto.setContainerNo(entity.getContainerNo());
        dto.setEquipmentShipperOwn(entity.getEquipmentShipperOwn());
        dto.setCargoDescription(entity.getCargoDescription());
        dto.setEquipmentSealNo(entity.getEquipmentSealNo());
        dto.setEquipmentIsoType(entity.getEquipmentIsoType());
        dto.setEquipmentType(entity.getEquipmentType());
        dto.setEquipmentSize(entity.getEquipmentSize());
        dto.setQuantity(entity.getQuantity());
        dto.setGrsVolume(entity.getGrsVolume());
        dto.setGrsWeight(entity.getGrsWeight());
        dto.setNetVolume(entity.getNetVolume());
        dto.setNetWeight(entity.getNetWeight());
        dto.setTareWeight(entity.getTareWeight());
        dto.setNoOfPacks(entity.getNoOfPacks());
        dto.setPackUnit(entity.getPackUnit());
        dto.setComodityPoid(entity.getComodityPoid());
        dto.setDestinationPortPoid(entity.getDestinationPortPoid());
        dto.setImo(entity.getImo());
        dto.setOogL(entity.getOogL());
        dto.setOogB(entity.getOogB());
        dto.setOogH(entity.getOogH());
        dto.setRefferTemp(entity.getRefferTemp());
        dto.setRefferHum(entity.getRefferHum());
        dto.setRefferVent(entity.getRefferVent());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setIssueToConsignee(entity.getIssueToConsignee());
        dto.setReturnFromConsignee(entity.getReturnFromConsignee());
        dto.setIsImco(entity.getIsImco());
        dto.setIsOog(entity.getIsOog());
        dto.setIsRefer(entity.getIsRefer());
        dto.setReferType(entity.getReferType());
        dto.setImcoClassType(entity.getImcoClassType());
        dto.setGuaranteeFlag(entity.getGuaranteeFlag());
        dto.setGuaranteedBy(entity.getGuaranteedBy());
        dto.setExtraFreeDays(entity.getExtraFreeDays());
        dto.setExtraFreeDaysPrnpls(entity.getExtraFreeDaysPrnpls());
        dto.setOogLW(entity.getOogLW());
        dto.setOogRW(entity.getOogRW());
        dto.setOogF(entity.getOogF());
        dto.setOogA(entity.getOogA());
        dto.setOogType(entity.getOogType());
        dto.setImcoClassActual(entity.getImcoClassActual());
        dto.setDisplayCollectedDate(entity.getDisplayCollectedDate());
        dto.setTotalDaysCollected(entity.getTotalDaysCollected());
        dto.setTotalAmountCollected(entity.getTotalAmountCollected());
        dto.setPrintReturnFormDefault(entity.getPrintReturnFormDefault());
        dto.setPrintDeliveryFormDefault(entity.getPrintDeliveryFormDefault());
        dto.setDemDttPbleTrnsfd(entity.getDemDttPbleTrnsfd());
        dto.setHsCode(entity.getHsCode());
        dto.setHsDescription(entity.getHsDescription());
        dto.setAmountPerDayAfterFree(entity.getAmountPerDayAfterFree());
        dto.setActualDischargeDate(entity.getActualDischargeDate());
        return dto;
    }

    /**
     * Convert Container Detail DTO to Entity
     */
    public ExportShipBlManifestContainerDtl mapContainerToEntity(ContainerDetailDto dto, Long transactionPoid, Long detRowId, String userId) {
        if (dto == null) {
            return null;
        }

        ExportShipBlManifestContainerDtl entity = new ExportShipBlManifestContainerDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId != null ? detRowId : 0L);
        entity.setMateTransactionPoid(dto.getMateTransactionPoid());
        entity.setContainerNo(dto.getContainerNo() != null ? dto.getContainerNo().trim() : null);
        entity.setEquipmentShipperOwn(dto.getEquipmentShipperOwn());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setEquipmentSealNo(dto.getEquipmentSealNo());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());
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
        return entity;
    }

    /**
     * Convert Cargo Description Entity to DTO
     */
    public CargoDescriptionDto mapCargoDescriptionToDto(ExportShipBlManifestCargoDtl entity) {
        if (entity == null) {
            return null;
        }

        CargoDescriptionDto dto = new CargoDescriptionDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setCargoDescription(entity.getCargoDescription());
        dto.setDescriptionType(entity.getDescriptionType());
        dto.setRecordOrder(entity.getRecordOrder());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    /**
     * Convert Cargo Description DTO to Entity
     */
    public ExportShipBlManifestCargoDtl mapCargoDescriptionToEntity(CargoDescriptionDto dto, Long transactionPoid, Long detRowId, String userId) {
        if (dto == null) {
            return null;
        }

        ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId != null ? detRowId : 0L);
        entity.setDescriptionType("CARGO");
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setRecordOrder(dto.getRecordOrder());
        return entity;
    }

    /**
     * Convert Cargo Marks Entity to DTO
     */
    public CargoMarksDto mapCargoMarksToDto(ExportShipBlManifestCargoDtl entity) {
        if (entity == null) {
            return null;
        }

        CargoMarksDto dto = new CargoMarksDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setCargoDescription(entity.getCargoDescription());
        dto.setDescriptionType(entity.getDescriptionType());
        dto.setRecordOrder(entity.getRecordOrder());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    /**
     * Convert Cargo Marks DTO to Entity
     */
    public ExportShipBlManifestCargoDtl mapCargoMarksToEntity(CargoMarksDto dto, Long transactionPoid, Long detRowId, String userId) {
        if (dto == null) {
            return null;
        }

        ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId != null ? detRowId : 0L);
        entity.setDescriptionType("MARKS");
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setRecordOrder(dto.getRecordOrder());
        return entity;
    }

    /**
     * Convert Charge Detail Entity to DTO
     */
    public ChargeDetailDto mapChargeToDto(ExportShipBlManifestChargesDtl entity) {
        if (entity == null) {
            return null;
        }

        ChargeDetailDto dto = new ChargeDetailDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setChargePoid(entity.getChargePoid());
        dto.setCurrencyExchange(entity.getCurrencyExchange());
        dto.setQuantity(entity.getQuantity());
        dto.setBuyPercharge(entity.getBuyPercharge());
        dto.setPerQuantityAmount(entity.getPerQuantityAmount());
        dto.setPaidAtPortPoid(entity.getPaidAtPortPoid());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setChargeType(entity.getChargeType());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setFreightType(entity.getFreightType());
        dto.setEdiChargeCode(entity.getEdiChargeCode());
        dto.setArShReceiptTransactionPoid(entity.getArShReceiptTransactionPoid());
        dto.setChargeBasisOn(entity.getChargeBasisOn());
        dto.setPrintGroup(entity.getPrintGroup());
        dto.setReceiptInvoicePoid(entity.getReceiptInvoicePoid());
        dto.setDocRefLinkNo(entity.getDocRefLinkNo());
        dto.setReprintDetRowId(entity.getReprintDetRowId());
        dto.setReprintTransactionPoid(entity.getReprintTransactionPoid());
        dto.setInvoiceType(entity.getInvoiceType());
        dto.setAutoCanInvoiceNo(entity.getAutoCanInvoiceNo());
        dto.setChargeDescription(entity.getChargeDescription());
        dto.setTaxPoid(entity.getTaxPoid());
        dto.setTaxPercentage(entity.getTaxPercentage());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setCnRefDocId(entity.getCnRefDocId());
        dto.setCnRefDocPoid(entity.getCnRefDocPoid());
        dto.setCnRefDetRowId(entity.getCnRefDetRowId());
        dto.setCnIssueInvoice(entity.getCnIssueInvoice());
        dto.setSelectRow(entity.getSelectRow());

        // Calculate derived fields
        if (entity.getQuantity() != null && entity.getBuyPercharge() != null) {
            dto.setBuyAmount(entity.getQuantity().multiply(entity.getBuyPercharge()));
        }
        if (entity.getQuantity() != null && entity.getPerQuantityAmount() != null) {
            dto.setSaleAmount(entity.getQuantity().multiply(entity.getPerQuantityAmount()));
        }
        if (dto.getBuyAmount() != null && dto.getSaleAmount() != null) {
            dto.setRevenue(dto.getSaleAmount().subtract(dto.getBuyAmount()));
        }
        if (entity.getDocRefLinkNo() != null) {
            dto.setDrilldownLinkInfo(entity.getDocRefLinkNo());
        }

        return dto;
    }

    /**
     * Convert Charge Detail DTO to Entity
     */
    public ExportShipBlManifestChargesDtl mapChargeToEntity(ChargeDetailDto dto, Long transactionPoid, Long detRowId, String userId) {
        if (dto == null) {
            return null;
        }

        ExportShipBlManifestChargesDtl entity = new ExportShipBlManifestChargesDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId != null ? detRowId : 0L);
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
        return entity;
    }

    /**
     * Convert list of General Cargo Details
     */
    public List<GeneralCargoDetailDto> mapGeneralCargoListToDto(List<ExportShipBlManifestGeneralDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapGeneralCargoToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Container Details
     */
    public List<ContainerDetailDto> mapContainerListToDto(List<ExportShipBlManifestContainerDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapContainerToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Cargo Descriptions
     */
    public List<CargoDescriptionDto> mapCargoDescriptionListToDto(List<ExportShipBlManifestCargoDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapCargoDescriptionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Cargo Marks
     */
    public List<CargoMarksDto> mapCargoMarksListToDto(List<ExportShipBlManifestCargoDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapCargoMarksToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Charge Details
     */
    public List<ChargeDetailDto> mapChargeListToDto(List<ExportShipBlManifestChargesDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapChargeToDto)
                .collect(Collectors.toList());
    }
}

