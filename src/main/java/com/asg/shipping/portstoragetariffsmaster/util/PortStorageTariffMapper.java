package com.asg.shipping.portstoragetariffsmaster.util;


import com.asg.shipping.portstoragetariffsmaster.dto.*;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtl;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffHdr;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class PortStorageTariffMapper {

    /**
     * Convert Header Entity to DTO
     */
    public PortStorageTariffDto mapToDto(ShipPortTariffHdr entity) {
        if (entity == null) {
            return null;
        }

        return PortStorageTariffDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .portPoid(entity.getPortPoid())
                .description(entity.getDescription())
                .tariffType(entity.getTariffType())
                .periodFrom(entity.getPeriodFrom())
                .periodTo(entity.getPeriodTo())
                .transactionDate(entity.getTransactionDate())
                .docRef(entity.getDocRef())
                .companyPoid(entity.getCompanyPoid())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .deleted(entity.getDeleted())
                .build();
    }

    /**
     * Convert Detail Entity to DTO
     */
    public TariffDetailDto mapDetailToDto(ShipPortTariffDtl entity) {
        if (entity == null) {
            return null;
        }

        return TariffDetailDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
                .containerSize(entity.getContainerSize())
                .freeDays(entity.getFreeDays())
                .slab1Tilldays(entity.getSlab1Tilldays())
                .slab1Rate(entity.getSlab1Rate())
                .slab2Tilldays(entity.getSlab2Tilldays())
                .slab2Rate(entity.getSlab2Rate())
                .slab3Tilldays(entity.getSlab3Tilldays())
                .slab3Rate(entity.getSlab3Rate())
                .slab4Tilldays(entity.getSlab4Tilldays())
                .slab4Rate(entity.getSlab4Rate())
                .slab5Tilldays(entity.getSlab5Tilldays())
                .slab5Rate(entity.getSlab5Rate())
                .slab6Tilldays(entity.getSlab6Tilldays())
                .slab6Rate(entity.getSlab6Rate())
                .slab7Tilldays(entity.getSlab7Tilldays())
                .slab7Rate(entity.getSlab7Rate())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Convert list of Detail Entities to DTOs
     */
    public List<TariffDetailDto> mapDetailsToDto(List<ShipPortTariffDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapDetailToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(PortStorageTariffCreateDTO dto, ShipPortTariffHdr entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setPortPoid(dto.getPortPoid());
        entity.setDescription(dto.getDescription());
        entity.setTariffType(dto.getTariffType());
        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now());
        entity.setDocRef(dto.getDocRef());
        entity.setCompanyPoid(companyPoid);

        // Set audit fields
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(PortStorageTariffUpdateDTO dto, ShipPortTariffHdr entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setPortPoid(dto.getPortPoid());
        entity.setDescription(dto.getDescription());
        entity.setTariffType(dto.getTariffType());
        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setDocRef(dto.getDocRef());
        entity.setCompanyPoid(companyPoid);

        // Update audit fields (do not update createdBy/createdDate)
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    /**
     * Map Detail CreateDTO to Entity
     */
    public ShipPortTariffDtl mapDetailCreateDTOToEntity(TariffDetailCreateDTO dto, Long transactionPoid, String currentUser) {
        ShipPortTariffDtl entity = new ShipPortTariffDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
        entity.setContainerSize(dto.getContainerSize());
        entity.setFreeDays(dto.getFreeDays());
        entity.setSlab1Tilldays(dto.getSlab1Tilldays());
        entity.setSlab1Rate(dto.getSlab1Rate());
        entity.setSlab2Tilldays(dto.getSlab2Tilldays());
        entity.setSlab2Rate(dto.getSlab2Rate());
        entity.setSlab3Tilldays(dto.getSlab3Tilldays());
        entity.setSlab3Rate(dto.getSlab3Rate());
        entity.setSlab4Tilldays(dto.getSlab4Tilldays());
        entity.setSlab4Rate(dto.getSlab4Rate());
        entity.setSlab5Tilldays(dto.getSlab5Tilldays());
        entity.setSlab5Rate(dto.getSlab5Rate());
        entity.setSlab6Tilldays(dto.getSlab6Tilldays());
        entity.setSlab6Rate(dto.getSlab6Rate());
        entity.setSlab7Tilldays(dto.getSlab7Tilldays());
        entity.setSlab7Rate(dto.getSlab7Rate());

        // Set audit fields
        entity.setCreatedBy(currentUser);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(LocalDateTime.now());

        return entity;
    }

    /**
     * Map Detail UpdateDTO to Entity (for new records)
     */
    public ShipPortTariffDtl mapDetailUpdateDTOToEntity(TariffDetailUpdateDTO dto, Long transactionPoid, String currentUser) {
        ShipPortTariffDtl entity = new ShipPortTariffDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
        entity.setContainerSize(dto.getContainerSize());
        entity.setFreeDays(dto.getFreeDays());
        entity.setSlab1Tilldays(dto.getSlab1Tilldays());
        entity.setSlab1Rate(dto.getSlab1Rate());
        entity.setSlab2Tilldays(dto.getSlab2Tilldays());
        entity.setSlab2Rate(dto.getSlab2Rate());
        entity.setSlab3Tilldays(dto.getSlab3Tilldays());
        entity.setSlab3Rate(dto.getSlab3Rate());
        entity.setSlab4Tilldays(dto.getSlab4Tilldays());
        entity.setSlab4Rate(dto.getSlab4Rate());
        entity.setSlab5Tilldays(dto.getSlab5Tilldays());
        entity.setSlab5Rate(dto.getSlab5Rate());
        entity.setSlab6Tilldays(dto.getSlab6Tilldays());
        entity.setSlab6Rate(dto.getSlab6Rate());
        entity.setSlab7Tilldays(dto.getSlab7Tilldays());
        entity.setSlab7Rate(dto.getSlab7Rate());

        // Set audit fields
        entity.setCreatedBy(currentUser);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(LocalDateTime.now());

        return entity;
    }

    /**
     * Update existing Detail Entity from UpdateDTO
     */
    public void updateDetailFromDTO(TariffDetailUpdateDTO dto, ShipPortTariffDtl entity, String currentUser) {
        entity.setContainerTypePoid(dto.getContainerTypePoid());
        entity.setContainerSize(dto.getContainerSize());
        entity.setFreeDays(dto.getFreeDays());
        entity.setSlab1Tilldays(dto.getSlab1Tilldays());
        entity.setSlab1Rate(dto.getSlab1Rate());
        entity.setSlab2Tilldays(dto.getSlab2Tilldays());
        entity.setSlab2Rate(dto.getSlab2Rate());
        entity.setSlab3Tilldays(dto.getSlab3Tilldays());
        entity.setSlab3Rate(dto.getSlab3Rate());
        entity.setSlab4Tilldays(dto.getSlab4Tilldays());
        entity.setSlab4Rate(dto.getSlab4Rate());
        entity.setSlab5Tilldays(dto.getSlab5Tilldays());
        entity.setSlab5Rate(dto.getSlab5Rate());
        entity.setSlab6Tilldays(dto.getSlab6Tilldays());
        entity.setSlab6Rate(dto.getSlab6Rate());
        entity.setSlab7Tilldays(dto.getSlab7Tilldays());
        entity.setSlab7Rate(dto.getSlab7Rate());

        // Update audit fields (do not update createdBy/createdDate)
        entity.setLastModifiedBy(currentUser);
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}