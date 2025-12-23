package com.asg.shipping.remuneration.mapper;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

public class ShipRemunerationMasterMapper {

    public static ShipRemunerationMasterResponseDto toResponseDto(ShipRemunerationMaster entity) {

        if (entity == null) {
            return null;
        }

        ShipRemunerationMasterResponseDto dto =
                new ShipRemunerationMasterResponseDto();

        dto.setRemunerationPoid(entity.getRemunerationPoid());
        dto.setRemunCode(entity.getRemunCode());
        dto.setRemunDescription(entity.getRemunDescription());
        dto.setImpExpType(entity.getImpExpType());
        dto.setActive(entity.getActive());
        dto.setSeqNo(entity.getSeqNo());
        dto.setRemunBasedOn(entity.getRemunBasedOn());
        dto.setRemunChargeCodePoid(entity.getRemunChargeCodePoid());
        dto.setGlPoid(entity.getGlPoid());
        dto.setRemunBookedByUsed(entity.getRemunBookedByUsed());
        dto.setDeleted(entity.getDeleted());

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());

        return dto;
    }

    public static ShipRemunerationMaster toEntity(ShipRemunerationMasterRequestDto dto) {

        if (dto == null) {
            return null;
        }

        ShipRemunerationMaster entity = new ShipRemunerationMaster();

        entity.setRemunCode(dto.getRemunCode());
        entity.setRemunDescription(dto.getRemunDescription());
        entity.setImpExpType(dto.getImpExpType());
        entity.setActive(StringUtils.isNotBlank(dto.getActive()) ? dto.getActive() : "Y");
        entity.setSeqNo(dto.getSeqNo());
        entity.setRemunBasedOn(dto.getRemunBasedOn());
        entity.setRemunChargeCodePoid(String.valueOf(dto.getRemunChargeCodePoid()));
        entity.setGlPoid(dto.getGlPoid());
        entity.setRemunBookedByUsed(dto.getRemunBookedByUsed());

        entity.setDeleted("N");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setCreatedBy(UserContext.getUserId());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setLastModifiedBy(UserContext.getUserId());

        return entity;
    }

    public static void updateEntity(ShipRemunerationMasterRequestDto dto, ShipRemunerationMaster entity) {

        if (dto == null || entity == null) {
            return;
        }

        entity.setRemunDescription(dto.getRemunDescription());
        entity.setImpExpType(dto.getImpExpType());
        entity.setActive(StringUtils.isNotBlank(dto.getActive()) ? dto.getActive() : entity.getActive());
        entity.setSeqNo(dto.getSeqNo());
        entity.setRemunBasedOn(dto.getRemunBasedOn());
        entity.setRemunChargeCodePoid(String.valueOf(dto.getRemunChargeCodePoid()));
        entity.setGlPoid(dto.getGlPoid());
        entity.setRemunBookedByUsed(dto.getRemunBookedByUsed());

        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setLastModifiedBy(UserContext.getUserId());
    }
}