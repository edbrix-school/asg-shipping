package com.asg.shipping.customerautochargeexportbl.util;

import com.asg.shipping.customerautochargeexportbl.dto.*;
import com.asg.shipping.customerautochargeexportbl.entity.*;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

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
                .transactionDate(toLocalDate(entity.getTransactionDate()))
                .description(entity.getDescription())
                .periodFrom(toLocalDate(entity.getPeriodFrom()))
                .periodTo(toLocalDate(entity.getPeriodTo()))
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
        entity.setPeriodFrom(java.sql.Date.valueOf(dto.getPeriodFrom()));
        entity.setPeriodTo(java.sql.Date.valueOf(dto.getPeriodTo()));
        entity.setTransactionDate(dto.getTransactionDate() != null ? java.sql.Date.valueOf(dto.getTransactionDate()) : java.sql.Date.valueOf(java.time.LocalDate.now()));
        entity.setDocRef(dto.getDocRef());
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(new Timestamp(System.currentTimeMillis()));
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
            entity.setPeriodFrom(java.sql.Date.valueOf(dto.getPeriodFrom()));
        }
        if (dto.getPeriodTo() != null) {
            entity.setPeriodTo(java.sql.Date.valueOf(dto.getPeriodTo()));
        }
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(java.sql.Date.valueOf(dto.getTransactionDate()));
        }
        if (dto.getDocRef() != null) {
            entity.setDocRef(dto.getDocRef());
        }
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(new Timestamp(System.currentTimeMillis()));
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
                .createdBy(getCurrentUser())
                .createdDate(new Timestamp(System.currentTimeMillis()))
                .build();
    }


    public List<CustomerAutoChargeDetailDto> mapDtlListToDto(List<ShipCustomerChargesDtlEntity> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapDtlToDto).collect(Collectors.toList());
    }

    public LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

}

