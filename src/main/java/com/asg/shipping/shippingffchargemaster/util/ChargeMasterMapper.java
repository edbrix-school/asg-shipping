package com.asg.shipping.shippingffchargemaster.util;

import com.asg.shipping.shippingffchargemaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingffchargemaster.dto.ChargeDto;
import com.asg.shipping.shippingffchargemaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingffchargemaster.entity.ShipChargeMaster;
import org.springframework.stereotype.Component;

@Component
public class ChargeMasterMapper {

    public ChargeDto mapToDto(ShipChargeMaster entity) {
        if (entity == null) {
            return null;
        }

        return ChargeDto.builder()
                .chargePoid(entity.getChargePoid())
                .groupPoid(entity.getGroupPoid())
                .chargeCode(entity.getChargeCode())
                .chargeName(entity.getChargeName())
                .chargeName2(entity.getChargeName2())
                .chargeRevenueType(entity.getChargeRevenueType())
                .chargeType(entity.getChargeType())
                .chargeApplicableType(entity.getChargeApplicableType())
                .divisionCode(entity.getDivisionCode())
                .chargeGlRevenue(entity.getChargeGlRevenue())
                .chargeGlCost(entity.getChargeGlCost())
                .chargeGlWip(entity.getChargeGlWip())
                .chargePayableGl(entity.getChargePayableGl())
                .fdaGlRevenue(entity.getFdaGlRevenue())
                .fdaGlCost(entity.getFdaGlCost())
                .directRevenueGl(entity.getDirectRevenueGl())
                .directCostOfSaleGl(entity.getDirectCostOfSaleGl())
                .directPayableGl(entity.getDirectPayableGl())
                .taxPoid(entity.getTaxPoid())
                .inputTaxPoid(entity.getInputTaxPoid())
                .chargeGroupPoid(entity.getChargeGroupPoid())
                .shFfChargeMap(entity.getShFfChargeMap())
                .shFfChargeGlPoid(entity.getShFfChargeGlPoid())
                .shFfChargeGlPoidRev(entity.getShFfChargeGlPoidRev())
                .visibleInFf(entity.getVisibleInFf())
                .oldChargeGlRevenue(entity.getOldChargeGlRevenue())
                .oldChargeGlCost(entity.getOldChargeGlCost())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .deleted(entity.getDeleted())
                .build();
    }

    /**
     * Map CreateDTO to Entity
     */
    public void mapCreateDTOToEntity(ChargeCreateDTO dto, ShipChargeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setChargeCode(dto.getChargeCode());
        entity.setChargeName(dto.getChargeName());
        entity.setChargeRevenueType(dto.getChargeRevenueType());
        entity.setChargeType(dto.getChargeType());
        entity.setChargeApplicableType(dto.getChargeApplicableType());
        entity.setDivisionCode(dto.getDivisionCode());
        entity.setChargeGlRevenue(dto.getChargeGlRevenue());
        entity.setChargeGlCost(dto.getChargeGlCost());
        entity.setChargeGlWip(dto.getChargeGlWip());
        entity.setChargePayableGl(dto.getChargePayableGl());
        entity.setFdaGlRevenue(dto.getFdaGlRevenue());
        entity.setFdaGlCost(dto.getFdaGlCost());
        entity.setDirectRevenueGl(dto.getDirectRevenueGl());
        entity.setDirectCostOfSaleGl(dto.getDirectCostOfSaleGl());
        entity.setDirectPayableGl(dto.getDirectPayableGl());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setInputTaxPoid(dto.getInputTaxPoid());
        entity.setChargeGroupPoid(dto.getChargeGroupPoid());
        entity.setShFfChargeMap(dto.getShFfChargeMap());
        entity.setShFfChargeGlPoid(dto.getShFfChargeGlPoid());
        entity.setSeqno(dto.getSeqno());

        // Set active status (default to Y if not provided)
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        } else {
            entity.setActive("Y");
        }


        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Entity
     */
    public void mapUpdateDTOToEntity(ChargeUpdateDTO dto, ShipChargeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        // Note: chargeCode is not updateable
        entity.setChargeName(dto.getChargeName());

        entity.setChargeRevenueType(dto.getChargeRevenueType());
        entity.setChargeType(dto.getChargeType());
        entity.setChargeApplicableType(dto.getChargeApplicableType());
        entity.setDivisionCode(dto.getDivisionCode());
        entity.setChargeGlRevenue(dto.getChargeGlRevenue());
        entity.setChargeGlCost(dto.getChargeGlCost());
        entity.setChargeGlWip(dto.getChargeGlWip());
        entity.setChargePayableGl(dto.getChargePayableGl());
        entity.setFdaGlRevenue(dto.getFdaGlRevenue());
        entity.setFdaGlCost(dto.getFdaGlCost());
        entity.setDirectRevenueGl(dto.getDirectRevenueGl());
        entity.setDirectCostOfSaleGl(dto.getDirectCostOfSaleGl());
        entity.setDirectPayableGl(dto.getDirectPayableGl());
        entity.setTaxPoid(dto.getTaxPoid());
        entity.setInputTaxPoid(dto.getInputTaxPoid());
        entity.setChargeGroupPoid(dto.getChargeGroupPoid());
        entity.setShFfChargeMap(dto.getShFfChargeMap());
        entity.setShFfChargeGlPoid(dto.getShFfChargeGlPoid());

        entity.setSeqno(dto.getSeqno());

        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

    }
}
