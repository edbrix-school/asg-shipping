package com.asg.shipping.vesselmaster.util;

import com.asg.shipping.vesselmaster.dto.*;
import com.asg.shipping.vesselmaster.entity.ShipVesselMaster;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class VesselMasterMapper {

    /**
     * Convert Entity to DTO
     */
    public VesselMasterDto mapToDto(ShipVesselMaster entity) {
        if (entity == null) {
            return null;
        }

        return VesselMasterDto.builder()
                .vesselPoid(entity.getVesselPoid())
                .groupPoid(entity.getGroupPoid())
                .vesselCode(entity.getVesselCode())
                .vesselName(entity.getVesselName())
                .vesselName2(entity.getVesselName2())
                .linePoid(entity.getLinePoid())
                .owner(entity.getOwner())
                .agentPoid(entity.getAgentPoid())
                .registrationNo(entity.getRegistrationNo())
                .registrationDate(entity.getRegistrationDate())
                .countryOfRegistration(entity.getCountryOfRegistration())
                .flagOfCountry(entity.getFlagOfCountry())
                .vesselTypePoid(entity.getVesselTypePoid())
                .vesselTypeClass(entity.getVesselTypeClass())
                .grt(entity.getGrt())
                .nrt(entity.getNrt())
                .dwt(entity.getDwt())
                .vesselLength(entity.getVesselLength())
                .beam(entity.getBeam())
                .draft(entity.getDraft())
                .hatches(entity.getHatches())
                .bayhatch(entity.getBayhatch())
                .imoNumber(entity.getImoNumber())
                .remarks(entity.getRemarks())
                .lineName(entity.getLineName())
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
    public void mapCreateDTOToEntity(VesselMasterCreateDTO dto, ShipVesselMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setVesselCode(dto.getVesselCode());
        entity.setVesselName(dto.getVesselName());
        entity.setVesselName2(dto.getVesselName2());
        entity.setLinePoid(dto.getLinePoid());
        entity.setOwner(dto.getOwner());
        entity.setAgentPoid(dto.getAgentPoid());
        entity.setRegistrationNo(dto.getRegistrationNo());
        entity.setRegistrationDate(dto.getRegistrationDate());
        entity.setCountryOfRegistration(dto.getCountryOfRegistration());
        entity.setFlagOfCountry(dto.getFlagOfCountry());
        entity.setVesselTypePoid(dto.getVesselTypePoid());
        entity.setVesselTypeClass(dto.getVesselTypeClass());
        entity.setGrt(dto.getGrt());
        entity.setNrt(dto.getNrt());
        entity.setDwt(dto.getDwt());
        entity.setVesselLength(dto.getVesselLength());
        entity.setBeam(dto.getBeam());
        entity.setDraft(dto.getDraft());
        entity.setHatches(dto.getHatches());
        entity.setBayhatch(dto.getBayhatch());
        entity.setImoNumber(dto.getImoNumber());
        entity.setRemarks(dto.getRemarks());

        // Set active status (default to Y if not provided)
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        } else {
            entity.setActive("Y");
        }

        entity.setSeqno(dto.getSeqno());

        // Set audit fields
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Entity
     * Note: VESSEL_CODE is not updated as it's updateable only on insert
     */
    public void mapUpdateDTOToEntity(VesselMasterUpdateDTO dto, ShipVesselMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        // VESSEL_CODE is not updated (updateable only on insert)
        entity.setVesselName(dto.getVesselName());
        entity.setVesselName2(dto.getVesselName2());
        entity.setLinePoid(dto.getLinePoid());
        entity.setOwner(dto.getOwner());
        entity.setAgentPoid(dto.getAgentPoid());
        entity.setRegistrationNo(dto.getRegistrationNo());
        entity.setRegistrationDate(dto.getRegistrationDate());
        entity.setCountryOfRegistration(dto.getCountryOfRegistration());
        entity.setFlagOfCountry(dto.getFlagOfCountry());
        entity.setVesselTypePoid(dto.getVesselTypePoid());
        entity.setVesselTypeClass(dto.getVesselTypeClass());
        entity.setGrt(dto.getGrt());
        entity.setNrt(dto.getNrt());
        entity.setDwt(dto.getDwt());
        entity.setVesselLength(dto.getVesselLength());
        entity.setBeam(dto.getBeam());
        entity.setDraft(dto.getDraft());
        entity.setHatches(dto.getHatches());
        entity.setBayhatch(dto.getBayhatch());
        entity.setImoNumber(dto.getImoNumber());
        entity.setRemarks(dto.getRemarks());

        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

        entity.setSeqno(dto.getSeqno());

        // Update audit fields (do not update createdBy/createdDate)
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}

