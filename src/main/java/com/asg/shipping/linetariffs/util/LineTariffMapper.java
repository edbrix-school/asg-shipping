package com.asg.shipping.linetariffs.util;

import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class LineTariffMapper {

    /**
     * Convert Header Entity to DTO
     */
    public LineTariffDto mapToDto(ShipLineTariffHdr entity,
                                   List<ShipLineTariffImpDtl> impDtlList,
                                   List<ShipLineTariffImpPayDtl> impPayDtlList,
                                   List<ShipLineTariffExpDtl> expDtlList,
                                   List<ShipLineTariffExpPayDtl> expPayDtlList) {
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
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .deleted(entity.getDeleted())
                .build();

        // Map detail lists
        if (impDtlList != null) {
            dto.setImportDemurrageCollectable(impDtlList.stream()
                    .map(this::mapImpDtlToDto)
                    .toList());
        }
        if (impPayDtlList != null) {
            dto.setImportDemurragePayable(impPayDtlList.stream()
                    .map(this::mapImpPayDtlToDto)
                    .toList());
        }
        if (expDtlList != null) {
            dto.setExportDetentionCollectable(expDtlList.stream()
                    .map(this::mapExpDtlToDto)
                    .toList());
        }
        if (expPayDtlList != null) {
            dto.setExportDetentionPayable(expPayDtlList.stream()
                    .map(this::mapExpPayDtlToDto)
                    .toList());
        }

        return dto;
    }

    /**
     * Map Import Demurrage Collectable Detail to DTO
     */
    public TariffDetailDto mapImpDtlToDto(ShipLineTariffImpDtl entity) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
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
    public TariffDetailDto mapImpPayDtlToDto(ShipLineTariffImpPayDtl entity) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
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
    public TariffDetailDto mapExpDtlToDto(ShipLineTariffExpDtl entity) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
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
    public TariffDetailDto mapExpPayDtlToDto(ShipLineTariffExpPayDtl entity) {
        if (entity == null) {
            return null;
        }
        return TariffDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .containerTypePoid(entity.getContainerTypePoid())
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

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        updateBasicFields(dto, entity);
        updateDmgFlags(dto, entity);
        updateDtnFlags(dto, entity);
        updateAccountingFields(dto, entity);

        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    private void updateBasicFields(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        applyIfNotNull(dto.getLinePoid(), entity::setLinePoid);
        applyIfNotNull(dto.getDescription(), entity::setDescription);
        applyIfNotNull(dto.getPeriodFrom(), entity::setPeriodFrom);
        applyIfNotNull(dto.getPeriodTo(), entity::setPeriodTo);
    }

    private void updateDmgFlags(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        applyIfNotNull(dto.getDmgFromSameday(), entity::setDmgFromSameday);
        applyIfNotNull(dto.getDmgFromNextday(), entity::setDmgFromNextday);
        applyIfNotNull(dto.getDmgSkipHolidays(), entity::setDmgSkipHolidays);
        applyIfNotNull(dto.getDmgSkipWeekends(), entity::setDmgSkipWeekends);
        applyIfNotNull(dto.getDmgBaseslabAfterFree(), entity::setDmgBaseslabAfterFree);
    }

    private void updateDtnFlags(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        applyIfNotNull(dto.getDtnFromSameday(), entity::setDtnFromSameday);
        applyIfNotNull(dto.getDtnFromNextday(), entity::setDtnFromNextday);
        applyIfNotNull(dto.getDtnSkipHolidays(), entity::setDtnSkipHolidays);
        applyIfNotNull(dto.getDtnSkipWeekends(), entity::setDtnSkipWeekends);
        applyIfNotNull(dto.getDtnBaseslabAfterFree(), entity::setDtnBaseslabAfterFree);
    }

    private void updateAccountingFields(LineTariffUpdateDTO dto, ShipLineTariffHdr entity) {
        applyIfNotNull(dto.getPayableCurrency(), entity::setPayableCurrency);
        applyIfNotNull(dto.getReceivableCurrency(), entity::setReceivableCurrency);
        applyIfNotNull(dto.getDocRef(), entity::setDocRef);
        applyIfNotNull(dto.getCompanyPoid(), entity::setCompanyPoid);
        applyIfNotNull(dto.getSeqno(), entity::setSeqno);
    }

    private <T> void applyIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
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

