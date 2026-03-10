package com.asg.shipping.groupcontainertypes.util;

import com.asg.shipping.groupcontainertypes.dto.ContainerGroupCreateDTO;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupDto;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupUpdateDTO;
import com.asg.shipping.groupcontainertypes.entity.ShipContainerTypeGrpMaster;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class ContainerGroupMapper {

    /**
     * Convert Entity to DTO
     */
    public ContainerGroupDto mapToDto(ShipContainerTypeGrpMaster entity) {
        if (entity == null) {
            return null;
        }

        return ContainerGroupDto.builder()
                .containerGrpPoid(entity.getContainerGrpPoid())
                .groupPoid(entity.getGroupPoid())
                .containerGrpCode(entity.getContainerGrpCode())
                .containerGrpName(entity.getContainerGrpName())
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
    public void mapCreateDTOToEntity(ContainerGroupCreateDTO dto, ShipContainerTypeGrpMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setContainerGrpCode(dto.getContainerGrpCode());
        entity.setContainerGrpName(dto.getContainerGrpName());
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
    public void mapUpdateDTOToEntity(ContainerGroupUpdateDTO dto, ShipContainerTypeGrpMaster entity, Long groupPoid, Long userPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setContainerGrpCode(dto.getContainerGrpCode());
        entity.setContainerGrpName(dto.getContainerGrpName());
        entity.setSeqno(dto.getSeqno());
        
        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

    }
}

