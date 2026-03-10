package com.asg.shipping.containertypes.util;

import com.asg.shipping.containertypes.dto.ContainerTypeCreateDTO;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.dto.ContainerTypeUpdateDTO;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class ContainerTypeMapper {

    /**
     * Convert Entity to DTO
     */
    public ContainerTypeDto mapToDto(ShipContainerTypeMaster entity) {
        if (entity == null) {
            return null;
        }

        return ContainerTypeDto.builder()
                .containerTypePoid(entity.getContainerTypePoid())
                .groupPoid(entity.getGroupPoid())
                .containerTypeCode(entity.getContainerTypeCode())
                .containerTypeName(entity.getContainerTypeName())
                .containerTypeSize(entity.getContainerTypeSize())
                .containerTypeIsoName(entity.getContainerTypeIsoName())
                .containerCargoWeight(entity.getContainerCargoWeight())
                .containerTareWeight(entity.getContainerTareWeight())
                .containerTeuFactor(entity.getContainerTeuFactor())
                .containerTypeCategory(entity.getContainerTypeCategory())
                .containerGrpPoid(entity.getContainerGrpPoid())
                .containerApmtTypeCode(entity.getContainerApmtTypeCode())
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
    public void mapCreateDTOToEntity(ContainerTypeCreateDTO dto, ShipContainerTypeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setContainerTypeCode(dto.getContainerTypeCode());
        entity.setContainerTypeName(dto.getContainerTypeName());
        entity.setContainerTypeSize(dto.getContainerTypeSize());
        entity.setContainerTypeIsoName(dto.getContainerTypeIsoName());
        entity.setContainerCargoWeight(dto.getContainerCargoWeight());
        entity.setContainerTareWeight(dto.getContainerTareWeight());
        entity.setContainerTeuFactor(dto.getContainerTeuFactor());
        entity.setContainerTypeCategory(dto.getContainerTypeCategory());
        entity.setContainerGrpPoid(dto.getContainerGrpPoid());
        entity.setContainerApmtTypeCode(dto.getContainerApmtTypeCode());
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
    public void mapUpdateDTOToEntity(ContainerTypeUpdateDTO dto, ShipContainerTypeMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        // Note: containerTypeCode is not updateable
        entity.setContainerTypeName(dto.getContainerTypeName());
        entity.setContainerTypeSize(dto.getContainerTypeSize());
        entity.setContainerTypeIsoName(dto.getContainerTypeIsoName());
        entity.setContainerCargoWeight(dto.getContainerCargoWeight());
        entity.setContainerTareWeight(dto.getContainerTareWeight());
        entity.setContainerTeuFactor(dto.getContainerTeuFactor());
        entity.setContainerTypeCategory(dto.getContainerTypeCategory());
        entity.setContainerGrpPoid(dto.getContainerGrpPoid());
        entity.setContainerApmtTypeCode(dto.getContainerApmtTypeCode());
        entity.setSeqno(dto.getSeqno());
        
        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }
    }
}


