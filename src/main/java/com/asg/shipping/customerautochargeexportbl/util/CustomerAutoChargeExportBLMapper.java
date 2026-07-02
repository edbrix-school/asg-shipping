package com.asg.shipping.customerautochargeexportbl.util;

import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeDetailDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesDtlEntity;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesHdrEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class CustomerAutoChargeExportBLMapper {

    /**
     * Convert Header Entity to DTO
     */
    public CustomerAutoChargeExportBLDto mapToDto(ShipCustomerChargesHdrEntity entity) {
        if (entity == null) {
            return null;
        }

        return CustomerAutoChargeExportBLDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .customerPoid(entity.getCustomerPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .description(entity.getDescription())
                .periodFrom(entity.getPeriodFrom())
                .periodTo(entity.getPeriodTo())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(CustomerAutoChargeExportBLCreateDTO dto, ShipCustomerChargesHdrEntity entity, Long groupPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCustomerPoid(dto.getCustomerPoid());
        entity.setDescription(dto.getDescription());
        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setTransactionDate(dto.getTransactionDate() == null ? DateUtil.getCurrentDateInUserTimeZone() : dto.getTransactionDate());
        entity.setDocRef(dto.getDocRef());
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(CustomerAutoChargeExportBLUpdateDTO dto, ShipCustomerChargesHdrEntity entity) {
        if (dto.getCustomerPoid() != null) {
            entity.setCustomerPoid(dto.getCustomerPoid());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getPeriodFrom() != null) {
            entity.setPeriodFrom(dto.getPeriodFrom());
        }
        if (dto.getPeriodTo() != null) {
            entity.setPeriodTo(dto.getPeriodTo());
        }
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getDocRef() != null) {
            entity.setDocRef(dto.getDocRef());
        }
    }

    public CustomerAutoChargeDetailDto mapDtlToDto(ShipCustomerChargesDtlEntity entity) {
        if (entity == null) return null;
        return CustomerAutoChargeDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .chargeCodePoid(entity.getChargeCodePoid())
                .type(entity.getType())
                .chargeApplicable(entity.getChargeApplicable())
                .imcoClassType(entity.getImcoClassType())
                .oogType(entity.getOogType())
                .othersType(entity.getOthersType())
                .currencyCode(entity.getCurrencyCode())
                .currencyExchange(entity.getCurrencyExchange())
                .amount20(entity.getAmount20())
                .amount40(entity.getAmount40())
                .amountOther(entity.getAmountOther())
                .amount20Cost(entity.getAmount20Cost())
                .amount40Cost(entity.getAmount40Cost())
                .amountOtherCost(entity.getAmountOtherCost())
                .amount53(entity.getAmount53())
                .amount53Cost(entity.getAmount53Cost())
                .build();
    }

    public ShipCustomerChargesDtlEntity mapDtlFromDto(CustomerAutoChargeDetailDto dto, Long transactionPoid, Long detRowId) {
        if (dto == null) return null;
        return ShipCustomerChargesDtlEntity.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .chargeCodePoid(dto.getChargeCodePoid())
                .type(dto.getType())
                .chargeApplicable(dto.getChargeApplicable())
                .imcoClassType(dto.getImcoClassType())
                .oogType(dto.getOogType())
                .othersType(dto.getOthersType())
                .currencyCode(dto.getCurrencyCode())
                .currencyExchange(dto.getCurrencyExchange())
                .amount20(dto.getAmount20())
                .amount40(dto.getAmount40())
                .amountOther(dto.getAmountOther())
                .amount20Cost(dto.getAmount20Cost())
                .amount40Cost(dto.getAmount40Cost())
                .amountOtherCost(dto.getAmountOtherCost())
                .amount53(dto.getAmount53())
                .amount53Cost(dto.getAmount53Cost())
                .build();
    }


    public List<CustomerAutoChargeDetailDto> mapDtlListToDto(List<ShipCustomerChargesDtlEntity> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapDtlToDto).collect(Collectors.toList());
    }

    public void updateDtlEntity(CustomerAutoChargeDetailDto dto, ShipCustomerChargesDtlEntity entity) {
        if (dto.getChargeCodePoid() != null) entity.setChargeCodePoid(dto.getChargeCodePoid());
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getChargeApplicable() != null) entity.setChargeApplicable(dto.getChargeApplicable());
        if (dto.getImcoClassType() != null) entity.setImcoClassType(dto.getImcoClassType());
        if (dto.getOogType() != null) entity.setOogType(dto.getOogType());
        if (dto.getOthersType() != null) entity.setOthersType(dto.getOthersType());
        if (dto.getCurrencyCode() != null) entity.setCurrencyCode(dto.getCurrencyCode());
        if (dto.getCurrencyExchange() != null) entity.setCurrencyExchange(dto.getCurrencyExchange());
        if (dto.getAmount20() != null) entity.setAmount20(dto.getAmount20());
        if (dto.getAmount40() != null) entity.setAmount40(dto.getAmount40());
        if (dto.getAmountOther() != null) entity.setAmountOther(dto.getAmountOther());
        if (dto.getAmount20Cost() != null) entity.setAmount20Cost(dto.getAmount20Cost());
        if (dto.getAmount40Cost() != null) entity.setAmount40Cost(dto.getAmount40Cost());
        if (dto.getAmountOtherCost() != null) entity.setAmountOtherCost(dto.getAmountOtherCost());
        if (dto.getAmount53() != null) entity.setAmount53(dto.getAmount53());
        if (dto.getAmount53Cost() != null) entity.setAmount53Cost(dto.getAmount53Cost());
    }

}

