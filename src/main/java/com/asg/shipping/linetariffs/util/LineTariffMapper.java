package com.asg.shipping.linetariffs.util;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class LineTariffMapper {

    private static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /**
     * Convert Header Entity to DTO
     */
    public LovGetListDto toContainerTypeLov(ShipContainerTypeMaster ct) {
        if (ct == null) return null;
        LovGetListDto lov = new LovGetListDto();
        lov.setPoid(ct.getContainerTypePoid());
        lov.setCode(ct.getContainerTypeCode());
        lov.setLabel(ct.getContainerTypeName());
        lov.setValue(ct.getContainerTypePoid());
        return lov;
    }

    public LineTariffDto mapToDto(ShipLineTariffHdr entity,
                                   List<ShipLineTariffImpDtl> impDtlList,
                                   List<ShipLineTariffImpPayDtl> impPayDtlList,
                                   List<ShipLineTariffExpDtl> expDtlList,
                                   List<ShipLineTariffExpPayDtl> expPayDtlList,
                                   Map<Long, ShipContainerTypeMaster> containerTypeMap) {
        if (entity == null) {
            return null;
        }

        LineTariffDto dto = LineTariffDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .transactionDate(entity.getTransactionDate())
                .groupPoid(entity.getGroupPoid())
                .linePoid(entity.getLinePoid())
                .description(entity.getDescription())
                .periodFrom(entity.getPeriodFrom())
                .periodTo(entity.getPeriodTo())
                .dmgFromSameday(entity.getDmgFromSameday())
                .dmgFromNextday(entity.getDmgFromNextday())
                .dmgSkipHolidays(entity.getDmgSkipHolidays())
                .dmgSkipWeekends(entity.getDmgSkipWeekends())
                .dmgBaseslabAfterFree(entity.getDmgBaseslabAfterFree())
                .dtnFromSameday(entity.getDtnFromSameday())
                .dtnFromNextday(entity.getDtnFromNextday())
                .dtnSkipHolidays(entity.getDtnSkipHolidays())
                .dtnSkipWeekends(entity.getDtnSkipWeekends())
                .dtnBaseslabAfterFree(entity.getDtnBaseslabAfterFree())
                .payableCurrency(entity.getPayableCurrency())
                .receivableCurrency(entity.getReceivableCurrency())
                .docRef(entity.getDocRef())
                .companyPoid(entity.getCompanyPoid())
                .seqno(entity.getSeqno())
                .extraTariff(entity.getExtraTariff())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .deleted(entity.getDeleted())
                .build();

        // Map detail lists
        if (impDtlList != null) {
            dto.setImportDemurrageCollectable(impDtlList.stream()
                    .map(d -> mapImpDtlToDto(d, containerTypeMap))
                    .toList());
        }
        if (impPayDtlList != null) {
            dto.setImportDemurragePayable(impPayDtlList.stream()
                    .map(d -> mapImpPayDtlToDto(d, containerTypeMap))
                    .toList());
        }
        if (expDtlList != null) {
            dto.setExportDetentionCollectable(expDtlList.stream()
                    .map(d -> mapExpDtlToDto(d, containerTypeMap))
                    .toList());
        }
        if (expPayDtlList != null) {
            dto.setExportDetentionPayable(expPayDtlList.stream()
                    .map(d -> mapExpPayDtlToDto(d, containerTypeMap))
                    .toList());
        }

        return dto;
    }

    /**
     * Map Import Demurrage Collectable Detail to DTO
     */
    public TariffDetailDto mapImpDtlToDto(ShipLineTariffImpDtl entity, Map<Long, ShipContainerTypeMaster> containerTypeMap) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
                .containerTypeDet(toContainerTypeLov(containerTypeMap.get(entity.getContainerTypePoid())))
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
                .build();
    }

    /**
     * Map Import Demurrage Payable Detail to DTO
     */
    public TariffDetailDto mapImpPayDtlToDto(ShipLineTariffImpPayDtl entity, Map<Long, ShipContainerTypeMaster> containerTypeMap) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
                .containerTypeDet(toContainerTypeLov(containerTypeMap.get(entity.getContainerTypePoid())))
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
                .build();
    }

    /**
     * Map Export Detention Collectable Detail to DTO
     */
    public TariffDetailDto mapExpDtlToDto(ShipLineTariffExpDtl entity, Map<Long, ShipContainerTypeMaster> containerTypeMap) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
                .containerTypeDet(toContainerTypeLov(containerTypeMap.get(entity.getContainerTypePoid())))
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
                .build();
    }

    /**
     * Map Export Detention Payable Detail to DTO
     */
    public TariffDetailDto mapExpPayDtlToDto(ShipLineTariffExpPayDtl entity, Map<Long, ShipContainerTypeMaster> containerTypeMap) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
                .containerTypeDet(toContainerTypeLov(containerTypeMap.get(entity.getContainerTypePoid())))
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
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(LineTariffCreateDTO dto, ShipLineTariffHdr entity, Long groupPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setLinePoid(dto.getLinePoid());
        entity.setDescription(dto.getDescription());
        setIfNotNull(dto.getTransactionDate(), entity::setTransactionDate);
        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setDmgFromSameday(dto.getDmgFromSameday());
        entity.setDmgFromNextday(dto.getDmgFromNextday());
        entity.setDmgSkipHolidays(dto.getDmgSkipHolidays());
        entity.setDmgSkipWeekends(dto.getDmgSkipWeekends());
        entity.setDmgBaseslabAfterFree(dto.getDmgBaseslabAfterFree());
        entity.setDtnFromSameday(dto.getDtnFromSameday());
        entity.setDtnFromNextday(dto.getDtnFromNextday());
        entity.setDtnSkipHolidays(dto.getDtnSkipHolidays());
        entity.setDtnSkipWeekends(dto.getDtnSkipWeekends());
        entity.setDtnBaseslabAfterFree(dto.getDtnBaseslabAfterFree());
        entity.setPayableCurrency(dto.getPayableCurrency());
        entity.setReceivableCurrency(dto.getReceivableCurrency());
        entity.setDocRef(dto.getDocRef());
        entity.setCompanyPoid(dto.getCompanyPoid());
        entity.setSeqno(dto.getSeqno());
        entity.setExtraTariff(dto.getExtraTariff() != null ? dto.getExtraTariff() : "N");

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        setIfNotNull(dto.getLinePoid(), entity::setLinePoid);
        setIfNotNull(dto.getDescription(), entity::setDescription);
        setIfNotNull(dto.getTransactionDate(), entity::setTransactionDate);
        setIfNotNull(dto.getPeriodFrom(), entity::setPeriodFrom);
        setIfNotNull(dto.getPeriodTo(), entity::setPeriodTo);
        setIfNotNull(dto.getDmgFromSameday(), entity::setDmgFromSameday);
        setIfNotNull(dto.getDmgFromNextday(), entity::setDmgFromNextday);
        setIfNotNull(dto.getDmgSkipHolidays(), entity::setDmgSkipHolidays);
        setIfNotNull(dto.getDmgSkipWeekends(), entity::setDmgSkipWeekends);
        setIfNotNull(dto.getDmgBaseslabAfterFree(), entity::setDmgBaseslabAfterFree);
        setIfNotNull(dto.getDtnFromSameday(), entity::setDtnFromSameday);
        setIfNotNull(dto.getDtnFromNextday(), entity::setDtnFromNextday);
        setIfNotNull(dto.getDtnSkipHolidays(), entity::setDtnSkipHolidays);
        setIfNotNull(dto.getDtnSkipWeekends(), entity::setDtnSkipWeekends);
        setIfNotNull(dto.getDtnBaseslabAfterFree(), entity::setDtnBaseslabAfterFree);
        setIfNotNull(dto.getPayableCurrency(), entity::setPayableCurrency);
        setIfNotNull(dto.getReceivableCurrency(), entity::setReceivableCurrency);
        if (dto.getDocRef() != null && !dto.getDocRef().trim().isEmpty()) {
            entity.setDocRef(dto.getDocRef().trim());
        }
        setIfNotNull(dto.getCompanyPoid(), entity::setCompanyPoid);
        setIfNotNull(dto.getSeqno(), entity::setSeqno);
        setIfNotNull(dto.getExtraTariff(), entity::setExtraTariff);

        // Update audit fields
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    /**
     * Map CreateDTO to Import Demurrage Collectable Detail Entity
     */
    public ShipLineTariffImpDtl mapImpDtlCreateDTOToEntity(
            TariffDetailCreateDTO dto,
            Long transactionPoid,
            Long detRowId) {

        ShipLineTariffImpDtl entity = new ShipLineTariffImpDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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

        return entity;
    }


    /**
     * Map UpdateDTO to Import Demurrage Collectable Detail Entity
     */
    public ShipLineTariffImpDtl mapImpDtlUpdateDTOToEntity(TariffDetailUpdateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffImpDtl entity = new ShipLineTariffImpDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Update existing Import Demurrage Collectable Detail Entity from UpdateDTO
     */
    public void updateImpDtlFromDTO(
            TariffDetailUpdateDTO dto,
            ShipLineTariffImpDtl entity) {

        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
    }


    /**
     * Map CreateDTO to Import Demurrage Payable Detail Entity
     */
    public ShipLineTariffImpPayDtl mapImpPayDtlCreateDTOToEntity(TariffDetailCreateDTO dto, Long transactionPoid, Long detRowId) {
        return ShipLineTariffImpPayDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .containerTypePoid(dto.getContainerTypePoid())
                .freeDays(dto.getFreeDays())
                .slab1Tilldays(dto.getSlab1Tilldays())
                .slab1Rate(dto.getSlab1Rate())
                .slab2Tilldays(dto.getSlab2Tilldays())
                .slab2Rate(dto.getSlab2Rate())
                .slab3Tilldays(dto.getSlab3Tilldays())
                .slab3Rate(dto.getSlab3Rate())
                .slab4Tilldays(dto.getSlab4Tilldays())
                .slab4Rate(dto.getSlab4Rate())
                .slab5Tilldays(dto.getSlab5Tilldays())
                .slab5Rate(dto.getSlab5Rate())
                .slab6Tilldays(dto.getSlab6Tilldays())
                .slab6Rate(dto.getSlab6Rate())
                .slab7Tilldays(dto.getSlab7Tilldays())
                .slab7Rate(dto.getSlab7Rate())
                .build();
    }

    /**
     * Map UpdateDTO to Import Demurrage Payable Detail Entity
     */
    public ShipLineTariffImpPayDtl mapImpPayDtlUpdateDTOToEntity(TariffDetailUpdateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffImpPayDtl entity = new ShipLineTariffImpPayDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Update existing Import Demurrage Payable Detail Entity from UpdateDTO
     */
    public void updateImpPayDtlFromDTO(TariffDetailUpdateDTO dto, ShipLineTariffImpPayDtl entity) {
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
    }

    /**
     * Map CreateDTO to Export Detention Collectable Detail Entity
     */
    public ShipLineTariffExpDtl mapExpDtlCreateDTOToEntity(TariffDetailCreateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffExpDtl entity = new ShipLineTariffExpDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Map UpdateDTO to Export Detention Collectable Detail Entity
     */
    public ShipLineTariffExpDtl mapExpDtlUpdateDTOToEntity(TariffDetailUpdateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffExpDtl entity = new ShipLineTariffExpDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Update existing Export Detention Collectable Detail Entity from UpdateDTO
     */
    public void updateExpDtlFromDTO(TariffDetailUpdateDTO dto, ShipLineTariffExpDtl entity) {
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
    }

    /**
     * Map CreateDTO to Export Detention Payable Detail Entity
     */
    public ShipLineTariffExpPayDtl mapExpPayDtlCreateDTOToEntity(TariffDetailCreateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffExpPayDtl entity = new ShipLineTariffExpPayDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Map UpdateDTO to Export Detention Payable Detail Entity
     */
    public ShipLineTariffExpPayDtl mapExpPayDtlUpdateDTOToEntity(TariffDetailUpdateDTO dto, Long transactionPoid, Long detRowId) {
        ShipLineTariffExpPayDtl entity = new ShipLineTariffExpPayDtl();
        entity.setTransactionPoid(transactionPoid);
        entity.setDetRowId(detRowId);
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
        return entity;
    }

    /**
     * Update existing Export Detention Payable Detail Entity from UpdateDTO
     */
    public void updateExpPayDtlFromDTO(TariffDetailUpdateDTO dto, ShipLineTariffExpPayDtl entity) {
        entity.setContainerTypePoid(dto.getContainerTypePoid());
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
    }
}

