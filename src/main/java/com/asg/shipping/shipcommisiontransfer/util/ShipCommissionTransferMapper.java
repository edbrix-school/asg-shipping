package com.asg.shipping.shipcommisiontransfer.util;

import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionDetailDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferCreateDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferUpdateDTO;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class ShipCommissionTransferMapper {

    /**
     * Convert Header Entity to DTO
     */
    public ShipCommissionTransferDto mapToDto(ShipBlCommissionHdr entity) {
        if (entity == null) {
            return null;
        }

        return ShipCommissionTransferDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .voyageTransactionPoid(entity.getVoyageTransactionPoid())
                .remarks(entity.getRemarks())
                .deleted(entity.getDeleted())
                .currencyExchange(entity.getCurrencyExchange())
                .currencyCode(entity.getCurrencyCode())
                .fdaTransactionPoid(entity.getFdaTransactionPoid())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(ShipCommissionTransferCreateDTO dto, ShipBlCommissionHdr entity, Long groupPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        entity.setRemarks(dto.getRemarks());
        entity.setCurrencyExchange(dto.getCurrencyExchange());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setFdaTransactionPoid(dto.getFdaTransactionPoid());

        // Set audit fields
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(ShipCommissionTransferUpdateDTO dto, ShipBlCommissionHdr entity) {
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getVoyageTransactionPoid() != null) {
            entity.setVoyageTransactionPoid(dto.getVoyageTransactionPoid());
        }
        if (dto.getRemarks() != null) {
            entity.setRemarks(dto.getRemarks());
        }
        if (dto.getCurrencyExchange() != null) {
            entity.setCurrencyExchange(dto.getCurrencyExchange());
        }
        if (dto.getCurrencyCode() != null) {
            entity.setCurrencyCode(dto.getCurrencyCode());
        }
        if (dto.getFdaTransactionPoid() != null) {
            entity.setFdaTransactionPoid(dto.getFdaTransactionPoid());
        }

        // Update audit fields
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    // Detail mapping methods
    public ShipCommissionDetailDto mapDtlToDto(ShipBlCommissionDtl entity) {
        if (entity == null) return null;
        return ShipCommissionDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .description(entity.getDescription())
                .blTransactionPoid(entity.getBlTransactionPoid())
                .currencyCode(entity.getCurrencyCode())
                .currencyExchange(entity.getCurrencyExchange())
                .quantity20(entity.getQuantity20())
                .quantity40(entity.getQuantity40())
                .buyPercharge(entity.getBuyPercharge())
                .sellAmount(entity.getSellAmount())
                .commissionAmt(entity.getCommissionAmt())
                .freightType(entity.getFreightType())
                .selected(entity.getSelected())
                .commitionOnAmount(entity.getCommitionOnAmount())
                .buyPerchargeFrt(entity.getBuyPerchargeFrt())
                .sellAmountFrt(entity.getSellAmountFrt())
                .drilldownLinkInfo(entity.getDrilldownLinkInfo())
                .blType(entity.getBlType())
                .blStatus(entity.getBlStatus())
                .thcAmount(entity.getThcAmount())
                .commissionHandAmt(entity.getCommissionHandAmt())
                .commissionAdjAmt(entity.getCommissionAdjAmt())
                .shortLegSelected(entity.getShortLegSelected())
                .build();
    }

    public ShipBlCommissionDtl mapDtlFromDto(ShipCommissionDetailDto dto, Long transactionPoid) {
        if (dto == null) return null;
        return ShipBlCommissionDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(dto.getDetRowId())
                .description(dto.getDescription())
                .blTransactionPoid(dto.getBlTransactionPoid())
                .currencyCode(dto.getCurrencyCode())
                .currencyExchange(dto.getCurrencyExchange())
                .quantity20(dto.getQuantity20())
                .quantity40(dto.getQuantity40())
                .buyPercharge(dto.getBuyPercharge())
                .sellAmount(dto.getSellAmount())
                .commissionAmt(dto.getCommissionAmt())
                .freightType(dto.getFreightType())
                .selected(dto.getSelected())
                .commitionOnAmount(dto.getCommitionOnAmount())
                .buyPerchargeFrt(dto.getBuyPerchargeFrt())
                .sellAmountFrt(dto.getSellAmountFrt())
                .drilldownLinkInfo(dto.getDrilldownLinkInfo())
                .blType(dto.getBlType())
                .blStatus(dto.getBlStatus())
                .thcAmount(dto.getThcAmount())
                .commissionHandAmt(dto.getCommissionHandAmt())
                .commissionAdjAmt(dto.getCommissionAdjAmt())
                .shortLegSelected(dto.getShortLegSelected())
                .build();
    }

    // Helper methods to map lists
    public List<ShipCommissionDetailDto> mapDtlListToDto(List<ShipBlCommissionDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapDtlToDto).collect(Collectors.toList());
    }
}
