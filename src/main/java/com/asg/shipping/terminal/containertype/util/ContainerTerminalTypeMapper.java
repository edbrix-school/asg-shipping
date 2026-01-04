package com.asg.shipping.terminal.containertype.util;

import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import com.asg.shipping.terminal.containertype.entity.ContainerTerminalTypeEntity;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContainerTerminalTypeMapper {

    /**
     * Map ContainerTerminalTypeEntity → Response DTO
     */
    public ContainerTerminalTypeResponse toResponse(ContainerTerminalTypeEntity entity) {
        if (entity == null) {
            return null;
        }

        ContainerTerminalTypeResponse response = new ContainerTerminalTypeResponse();
        response.setContainerTerminalTypePoid(entity.getContainerTerminalTypePoid());
        response.setContainerTerminalTypeCode(entity.getContainerTerminalTypeCode());
        response.setContainerTerminalTypeName(entity.getContainerTerminalTypeName());
        response.setContainerTerminalTypeSize(
                entity.getContainerTerminalTypeSize() != null
                        ? Long.valueOf(entity.getContainerTerminalTypeSize())
                        : null
        );
        response.setSeqNo(
                entity.getSeqno() != null
                        ? entity.getSeqno()
                        : null
        );
        response.setActive(entity.getActive());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setModifiedBy(entity.getLastModifiedBy());
        response.setModifiedDate(entity.getLastModifiedDate());

        return response;
    }

    /**
     * Map Request DTO → Entity (Create)
     */
    public ContainerTerminalTypeEntity toEntity(
            ContainerTerminalTypeRequest request,
            Long groupPoid,
            String userId) {

        if (request == null) {
            return null;
        }

        ContainerTerminalTypeEntity entity = new ContainerTerminalTypeEntity();

        String code = request.getContainerTerminalTypeCode() != null
                ? request.getContainerTerminalTypeCode().trim().toUpperCase()
                : null;
        entity.setContainerTerminalTypeCode(code);

        entity.setContainerTerminalTypeName(
                request.getContainerTerminalTypeName() != null
                        ? request.getContainerTerminalTypeName().trim()
                        : null
        );

        entity.setContainerTerminalTypeSize(
                request.getContainerTerminalTypeSize() != null
                        ? request.getContainerTerminalTypeSize().toString()
                        : null
        );

        entity.setSeqno(
                request.getSeqNo() != null
                        ? request.getSeqNo()
                        : null
        );

        String activeValue = request.getActive() != null ? request.getActive() : "Y";
        entity.setActive(
                activeValue.equalsIgnoreCase("true")
                        || activeValue.equalsIgnoreCase("Y")
                        ? "Y"
                        : "N"
        );

        entity.setDeleted("N");

        entity.setCreatedBy(userId);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());

        entity.setGroupPoid(groupPoid);

        return entity;
    }

    /**
     * Update Entity from Request DTO (Update)
     */
    public void updateEntity(
            ContainerTerminalTypeEntity entity,
            ContainerTerminalTypeRequest request,
            String userId) {



        if (entity == null || request == null) {
            return;
        }

        if (request.getContainerTerminalTypeName() != null) {
            entity.setContainerTerminalTypeName(
                    request.getContainerTerminalTypeName().trim()
            );
        }

        if (request.getContainerTerminalTypeSize() != null) {
            entity.setContainerTerminalTypeSize(
                    request.getContainerTerminalTypeSize().toString()
            );
        }

        if (request.getSeqNo() != null) {
            entity.setSeqno(request.getSeqNo());
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

        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    /**
     * Map list of entities → response list
     */
    public List<ContainerTerminalTypeResponse> toResponseList(
            List<ContainerTerminalTypeEntity> entities) {

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
