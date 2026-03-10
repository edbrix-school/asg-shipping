package com.asg.shipping.vesseltypemaster.util;


import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;
import com.asg.shipping.vesseltypemaster.entity.ShipVesselTypeMaster;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class VesselTypeMapper {

    /**
     * Convert Entity to DTO
     */
    public VesselTypeDto mapToDto(ShipVesselTypeMaster entity) {
        if (entity == null) {
            return null;
        }

        return VesselTypeDto.builder()
                .vesselTypePoid(entity.getVesselTypePoid())
                .groupPoid(entity.getGroupPoid())
                .vesselTypeCode(entity.getVesselTypeCode())
                .vesselTypeName(entity.getVesselTypeName())
                .vesselTypeName2(entity.getVesselTypeName2())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .costCentrePoid(entity.getCostCentrePoid())
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
    public void mapCreateDTOToEntity(VesselTypeCreateDTO dto, ShipVesselTypeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setVesselTypeCode(dto.getVesselTypeCode());
        entity.setVesselTypeName(dto.getVesselTypeName());
        entity.setVesselTypeName2(dto.getVesselTypeName2());
        entity.setSeqno(dto.getSeqno());
        entity.setCostCentrePoid(dto.getCostCentrePoid());

        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        } else {
            entity.setActive("Y");
        }
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Entity
     */
    public void mapUpdateDTOToEntity(VesselTypeUpdateDTO dto, ShipVesselTypeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setVesselTypeName(dto.getVesselTypeName());
        entity.setVesselTypeName2(dto.getVesselTypeName2());
        entity.setSeqno(dto.getSeqno());
        entity.setCostCentrePoid(dto.getCostCentrePoid());

        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}
