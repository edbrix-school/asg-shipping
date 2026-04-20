package com.asg.shipping.regionmaster.util;

import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import com.asg.shipping.regionmaster.entity.ShipRegionMasterEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RegionMasterMapper {

    /**
     * Map ShipRegionMasterEntity → Response DTO
     */
    public RegionMasterResponse toResponse(ShipRegionMasterEntity entity) {
        if (entity == null) {
            return null;
        }

        RegionMasterResponse response = new RegionMasterResponse();
        response.setRegionPoid(entity.getRegionPoid());
        response.setRegionCode(entity.getRegionCode());
        response.setRegionName(entity.getRegionName());
        response.setRegionName2(entity.getRegionName2());
        response.setActive(entity.getActive());
        response.setSeqno(entity.getSeqno());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedBy(entity.getLastModifiedBy());
        response.setLastModifiedDate(entity.getLastModifiedDate());
        response.setDeleted(entity.getDeleted());

        return response;
    }

    /**
     * Map Request DTO → Entity (Create)
     */
    public ShipRegionMasterEntity toEntity(
            RegionMasterRequest request,
            Long groupPoid,
            String userId) {

        if (request == null) {
            return null;
        }

        ShipRegionMasterEntity entity = new ShipRegionMasterEntity();

        String code = request.getRegionCode() != null
                ? request.getRegionCode().trim()
                : null;
        entity.setRegionCode(code);

        entity.setRegionName(
                request.getRegionName() != null
                        ? request.getRegionName().trim()
                        : null
        );

        entity.setRegionName2(
                request.getRegionName2() != null
                        ? request.getRegionName2().trim()
                        : null
        );

        entity.setSeqno(request.getSeqno());

        String activeValue = request.getActive() != null ? request.getActive() : "Y";
        entity.setActive(
                activeValue.equalsIgnoreCase("true")
                        || activeValue.equalsIgnoreCase("Y")
                        ? "Y"
                        : "N"
        );

        entity.setDeleted("N");
        entity.setGroupPoid(groupPoid);

        return entity;
    }

    /**
     * Update Entity from Request DTO (Update)
     */
    public void updateEntity(
            ShipRegionMasterEntity entity,
            RegionMasterRequest request,
            String userId) {

        if (entity == null || request == null) {
            return;
        }

        if (request.getRegionCode() != null) {
            entity.setRegionCode(request.getRegionCode().trim());
        }

        if (request.getRegionName() != null) {
            entity.setRegionName(request.getRegionName().trim());
        }

        if (request.getRegionName2() != null) {
            entity.setRegionName2(request.getRegionName2().trim());
        }

        if (request.getSeqno() != null) {
            entity.setSeqno(request.getSeqno());
        }

        if (request.getActive() != null) {
            String activeValue = request.getActive();
            entity.setActive(
                    activeValue.equalsIgnoreCase("true")
                            || activeValue.equalsIgnoreCase("Y")
                            ? "Y"
                            : "N"
            );
        }
    }

    /**
     * Map list of entities → response list
     */
    public List<RegionMasterResponse> toResponseList(
            List<ShipRegionMasterEntity> entities) {

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

