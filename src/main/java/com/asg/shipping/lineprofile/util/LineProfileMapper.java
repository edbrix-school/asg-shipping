package com.asg.shipping.lineprofile.util;

import com.asg.shipping.lineprofile.dto.LineProfileContactDto;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import com.asg.shipping.lineprofile.entity.ShipLineProfileContactDtlEntity;
import com.asg.shipping.lineprofile.entity.ShipLineProfileMasterEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LineProfileMapper {

    public ShipLineProfileMasterEntity toCreateEntity(LineProfileRequest request, Long groupPoid, String userId) {
        ShipLineProfileMasterEntity entity = new ShipLineProfileMasterEntity();
        entity.setGroupPoid(groupPoid);
        entity.setLinePoid(request.getLinePoid());
        entity.setRegion(joinRegionPoids(request.getRegionPoids()));
        entity.setRemarks(request.getRemarks());
        entity.setAgreementPoid(request.getAgreementPoid());
        entity.setActive(StringUtils.defaultIfBlank(request.getActive(), "Y"));
        entity.setSeqNo(request.getSeqNo());
        entity.setLogoImage(decodeBase64(request.getLogoImageBase64()));
        entity.setDeleted("N");
        entity.setCreatedBy(userId);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    public void applyUpdate(ShipLineProfileMasterEntity entity, LineProfileRequest request, String userId) {
        entity.setRegion(joinRegionPoids(request.getRegionPoids()));
        entity.setRemarks(request.getRemarks());
        entity.setAgreementPoid(request.getAgreementPoid());
        if (StringUtils.isNotBlank(request.getActive())) {
            entity.setActive(request.getActive());
        }
        entity.setSeqNo(request.getSeqNo());
        if (request.getLogoImageBase64() != null) {
            entity.setLogoImage(decodeBase64(request.getLogoImageBase64()));
        }
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    public LineProfileResponse toResponse(ShipLineProfileMasterEntity entity, List<ShipLineProfileContactDtlEntity> contacts) {
        LineProfileResponse response = new LineProfileResponse();
        response.setLineProfilePoid(entity.getLineProfilePoid());
        response.setLinePoid(entity.getLinePoid());
        response.setRegionPoids(parseRegionPoids(entity.getRegion()));
        response.setRemarks(entity.getRemarks());
        response.setAgreementPoid(entity.getAgreementPoid());
        response.setActive(entity.getActive());
        response.setSeqNo(entity.getSeqNo());
        response.setDeleted(entity.getDeleted());
        response.setLogoImageBase64(encodeBase64(entity.getLogoImage()));
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedBy(entity.getLastModifiedBy());
        response.setLastModifiedDate(entity.getLastModifiedDate());
        if (contacts != null) {
            response.setContactDetails(contacts.stream().map(this::toContactDto).collect(Collectors.toList()));
        }
        return response;
    }

    public ShipLineProfileContactDtlEntity toNewContactEntity(Long lineProfilePoid, Long detRowId, LineProfileContactDto dto, String userId) {
        ShipLineProfileContactDtlEntity entity = new ShipLineProfileContactDtlEntity();
        entity.setLineProfilePoid(lineProfilePoid);
        entity.setDetRowId(detRowId);
        entity.setContactName(dto.getContactName());
        entity.setDesignation(dto.getDesignation());
        entity.setMobile(dto.getMobile());
        entity.setLandline(dto.getLandline());
        entity.setEmailAddress(dto.getEmailAddress());
        entity.setCreatedBy(userId);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    public void applyUpdateContactEntity(ShipLineProfileContactDtlEntity entity, LineProfileContactDto dto, String userId) {
        entity.setContactName(dto.getContactName());
        entity.setDesignation(dto.getDesignation());
        entity.setMobile(dto.getMobile());
        entity.setLandline(dto.getLandline());
        entity.setEmailAddress(dto.getEmailAddress());
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private LineProfileContactDto toContactDto(ShipLineProfileContactDtlEntity entity) {
        LineProfileContactDto dto = new LineProfileContactDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setContactName(entity.getContactName());
        dto.setDesignation(entity.getDesignation());
        dto.setMobile(entity.getMobile());
        dto.setLandline(entity.getLandline());
        dto.setEmailAddress(entity.getEmailAddress());
        return dto;
    }

    private String joinRegionPoids(List<Long> regionPoids) {
        if (regionPoids == null || regionPoids.isEmpty()) {
            return null;
        }
        return regionPoids.stream()
                .filter(value -> value != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> parseRegionPoids(String region) {
        if (StringUtils.isBlank(region)) {
            return List.of();
        }
        List<Long> out = new java.util.ArrayList<>();
        for (String part : region.split(",")) {
            if (StringUtils.isBlank(part)) continue;
            try {
                out.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid tokens
            }
        }
        return out;
    }

    private byte[] decodeBase64(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Base64.getDecoder().decode(value);
    }

    private String encodeBase64(byte[] value) {
        if (value == null || value.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value);
    }
}

