package com.asg.shipping.shippingmanifestcorrector.util;

import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.shippingmanifestcorrector.dto.*;
import com.asg.shipping.shippingmanifestcorrector.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class ManifestCorrectorMapper {

    /**
     * Convert Header Entity to DTO
     */
    public ManifestCorrectorDto mapToDto(ShipBlReprintHdr entity) {
        if (entity == null) {
            return null;
        }

        return ManifestCorrectorDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .transactionDate(entity.getTransactionDate())
                .docRef(entity.getDocRef())
                .blNumber(entity.getBlNumber())
                .doReprint(entity.getDoReprint())
                .containerReprint(entity.getContainerReprint())
                .returnReprint(entity.getReturnReprint())
                .blReprint(entity.getBlReprint())
                .issueType(entity.getIssueType())
                .consigneePoid(entity.getConsigneePoid())
                .notifyPoid(entity.getNotifyPoid())
                .shipperEdiName(entity.getShipperEdiName())
                .remarks(entity.getRemarks())
                .blType(entity.getBlType())
                .demRefund(entity.getDemRefund())
                .holdCanDo(entity.getHoldCanDo())
                .holdReason(entity.getHoldReason())
                .payableGlPoid(entity.getPayableGlPoid())
                .incomeGlPoid(entity.getIncomeGlPoid())
                .payingTo(entity.getPayingTo())
                .demPayType(entity.getDemPayType())
                .demCustomerPoid(entity.getDemCustomerPoid())
                .placeOfDeliveryPoid(entity.getPlaceOfDeliveryPoid())
                .placeOfReceiptPoid(entity.getPlaceOfReceiptPoid())
                .blPlaceDelivery(entity.getBlPlaceReceipt())
                .blPlaceLoad(entity.getBlPlaceLoad())
                .blFinalDestination(entity.getBlFinalDestination())
                .blPlaceDischargeDesc(entity.getBlPlaceDischargeDesc())
                .portOfLoadingPoid(entity.getPortOfLoadingPoid())
                .portOfDischargePoid(entity.getPortOfDischargePoid())
                .voyageTransactionPoid(entity.getVoyageTransactionPoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .companyPoid(entity.getCompanyPoid())
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(ManifestCorrectorCreateDTO dto, ShipBlReprintHdr entity, Long companyPoid) {
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone());
        entity.setBlNumber(dto.getBlNumber());
        entity.setDoReprint(dto.getDoReprint() != null ? dto.getDoReprint() : "N");
        entity.setContainerReprint(dto.getContainerReprint() != null ? dto.getContainerReprint() : "N");
        entity.setReturnReprint(dto.getReturnReprint() != null ? dto.getReturnReprint() : "N");
        entity.setBlReprint(dto.getBlReprint() != null ? dto.getBlReprint() : "N");
        entity.setIssueType(dto.getIssueType());
        entity.setConsigneePoid(dto.getConsigneePoid());
        entity.setNotifyPoid(dto.getNotifyPoid());
        entity.setShipperEdiName(dto.getShipperEdiName());
        entity.setRemarks(dto.getRemarks());
        entity.setBlType(dto.getBlType());
        entity.setDemRefund(dto.getDemRefund() != null ? dto.getDemRefund() : "N");
        entity.setHoldCanDo(dto.getHoldCanDo() != null ? dto.getHoldCanDo() : "N");
        entity.setHoldReason(dto.getHoldReason());
        entity.setPayableGlPoid(dto.getPayableGlPoid());
        entity.setIncomeGlPoid(dto.getIncomeGlPoid());
        entity.setPayingTo(dto.getPayingTo());
        entity.setDemPayType(dto.getDemPayType());
        entity.setDemCustomerPoid(dto.getDemCustomerPoid());
        entity.setPlaceOfDeliveryPoid(dto.getPlaceOfDeliveryPoid());
        entity.setPlaceOfReceiptPoid(dto.getPlaceOfReceiptPoid());
        entity.setBlPlaceReceipt(dto.getBlPlaceReceipt());
        entity.setBlPlaceLoad(dto.getBlPlaceLoad());
        entity.setBlFinalDestination(dto.getBlFinalDestination());
        entity.setBlPlaceDischargeDesc(dto.getBlPlaceDischargeDesc());
        entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
        entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
        entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        entity.setCompanyPoid(companyPoid);
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(ManifestCorrectorUpdateDTO dto, ShipBlReprintHdr entity) {
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone());
        }
        if (dto.getBlNumber() != null) {
            entity.setBlNumber(dto.getBlNumber());
        }
        if (dto.getDoReprint() != null) {
            entity.setDoReprint(dto.getDoReprint());
        }
        if (dto.getContainerReprint() != null) {
            entity.setContainerReprint(dto.getContainerReprint());
        }
        if (dto.getReturnReprint() != null) {
            entity.setReturnReprint(dto.getReturnReprint());
        }
        if (dto.getBlReprint() != null) {
            entity.setBlReprint(dto.getBlReprint());
        }
        if (dto.getIssueType() != null) {
            entity.setIssueType(dto.getIssueType());
        }
        if (dto.getConsigneePoid() != null) {
            entity.setConsigneePoid(dto.getConsigneePoid());
        }
        if (dto.getNotifyPoid() != null) {
            entity.setNotifyPoid(dto.getNotifyPoid());
        }
        if (dto.getShipperEdiName() != null) {
            entity.setShipperEdiName(dto.getShipperEdiName());
        }
        if (dto.getRemarks() != null) {
            entity.setRemarks(dto.getRemarks());
        }
        if (dto.getBlType() != null) {
            entity.setBlType(dto.getBlType());
        }
        if (dto.getDemRefund() != null) {
            entity.setDemRefund(dto.getDemRefund());
        }
        if (dto.getHoldCanDo() != null) {
            entity.setHoldCanDo(dto.getHoldCanDo());
        }
        if (dto.getHoldReason() != null) {
            entity.setHoldReason(dto.getHoldReason());
        }
        if (dto.getPayableGlPoid() != null) {
            entity.setPayableGlPoid(dto.getPayableGlPoid());
        }
        if (dto.getIncomeGlPoid() != null) {
            entity.setIncomeGlPoid(dto.getIncomeGlPoid());
        }
        if (dto.getPayingTo() != null) {
            entity.setPayingTo(dto.getPayingTo());
        }
        if (dto.getDemPayType() != null) {
            entity.setDemPayType(dto.getDemPayType());
        }
        if (dto.getDemCustomerPoid() != null) {
            entity.setDemCustomerPoid(dto.getDemCustomerPoid());
        }
        if (dto.getPlaceOfDeliveryPoid() != null) {
            entity.setPlaceOfDeliveryPoid(dto.getPlaceOfDeliveryPoid());
        }
        if (dto.getPlaceOfReceiptPoid() != null) {
            entity.setPlaceOfReceiptPoid(dto.getPlaceOfReceiptPoid());
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
        if (dto.getBlPlaceDischargeDesc() != null) {
            entity.setBlPlaceDischargeDesc(dto.getBlPlaceDischargeDesc());
        }
        if (dto.getPortOfLoadingPoid() != null) {
            entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
        }
        if (dto.getPortOfDischargePoid() != null) {
            entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
        }
        if (dto.getVoyageTransactionPoid() != null) {
            entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        }
    }

    /**
     * Convert Charge Detail Entity to DTO
     */
    public ManifestCorrectorChargeDtlDto mapChargeDtlToDto(ShipBlReprintChargeDtl entity) {
        if (entity == null) {
            return null;
        }

        return ManifestCorrectorChargeDtlDto.builder()
                .detRowId(entity.getDetRowId())
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
                .containerNumber(entity.getContainerNumber())
                .revPayable(entity.getRevPayable())
                .revIncome(entity.getRevIncome())
                .build();
    }

    /**
     * Map Charge Detail DTO to Entity
     */
    public ShipBlReprintChargeDtl mapChargeDtlFromDto(ManifestCorrectorChargeDtlDto dto, Long transactionPoid, Long detRowId) {
        if (dto == null) {
            return null;
        }

        return ShipBlReprintChargeDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .chargePoid(dto.getChargePoid())
                .currencyExchange(dto.getCurrencyExchange() != null ? dto.getCurrencyExchange() : BigDecimal.ONE)
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
                .containerNumber(dto.getContainerNumber())
                .revPayable(dto.getRevPayable())
                .revIncome(dto.getRevIncome())
                .build();
    }

    /**
     * Convert Container Detail Entity to DTO
     */
    public ManifestCorrectorContainerDtlDto mapContainerDtlToDto(ShipBlReprintContainerDtl entity) {
        if (entity == null) {
            return null;
        }

        return ManifestCorrectorContainerDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .containerNumber(entity.getContainerNumber())
                .containerType(entity.getEquipmentIsoType())
                .isSelectedDlv(entity.getIsSelectedDlv())
                .isSelectedRtn(entity.getIsSelectedRtn())
                .build();
    }

    /**
     * Map Container Detail DTO to Entity
     */
    public ShipBlReprintContainerDtl mapContainerDtlFromDto(ManifestCorrectorContainerDtlDto dto, Long transactionPoid, Long detRowId) {
        if (dto == null) {
            return null;
        }

        return ShipBlReprintContainerDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .containerNumber(dto.getContainerNumber())
                .equipmentIsoType(dto.getContainerType())
                .isSelectedDlv(dto.getIsSelectedDlv())
                .isSelectedRtn(dto.getIsSelectedRtn())
                .build();
    }

    /**
     * Convert list of Charge Details
     */
    public List<ManifestCorrectorChargeDtlDto> mapChargeDtlListToDto(List<ShipBlReprintChargeDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapChargeDtlToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Container Details
     */
    public List<ManifestCorrectorContainerDtlDto> mapContainerDtlListToDto(List<ShipBlReprintContainerDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapContainerDtlToDto)
                .collect(Collectors.toList());
    }
}

