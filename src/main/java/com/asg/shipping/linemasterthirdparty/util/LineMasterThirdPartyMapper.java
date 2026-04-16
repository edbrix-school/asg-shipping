package com.asg.shipping.linemasterthirdparty.util;

import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.entity.ShipLineMaster;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class LineMasterThirdPartyMapper {

    /**
     * Convert Entity to DTO
     */
    public LineMasterThirdPartyDto mapToDto(ShipLineMaster entity) {
        if (entity == null) {
            return null;
        }

        return LineMasterThirdPartyDto.builder()
                .linePoid(entity.getLinePoid())
                .lineCode(entity.getLineCode())
                .lineName(entity.getLineName())
                .lineName2(entity.getLineName2())
                .lineAddress(entity.getLineAddress())
                .countryPoid(entity.getCountryPoid())
                .currencyPoid(entity.getCurrencyPoid())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .billTo(entity.getBillTo())
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
    public void mapCreateDTOToEntity(LineMasterThirdPartyCreateDTO dto, ShipLineMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setLineCode(dto.getLineCode());
        entity.setLineName(dto.getLineName());
        entity.setLineName2(dto.getLineName2());
        entity.setLineAddress(dto.getLineAddress());
        entity.setCountryPoid(dto.getCountryPoid());
        entity.setCurrencyPoid(dto.getCurrencyPoid());
        entity.setBillTo(dto.getBillTo());
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
    public void mapUpdateDTOToEntity(LineMasterThirdPartyUpdateDTO dto, ShipLineMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setLineName(dto.getLineName());
        entity.setLineName2(dto.getLineName2());
        entity.setLineAddress(dto.getLineAddress());
        entity.setCountryPoid(dto.getCountryPoid());
        entity.setCurrencyPoid(dto.getCurrencyPoid());
        entity.setSeqno(dto.getSeqno());
        entity.setBillTo(dto.getBillTo());

        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

    }
}

