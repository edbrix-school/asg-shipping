package com.asg.shipping.demurragedetentionpayabletransfer.util;

import com.asg.shipping.demurragedetentionpayabletransfer.dto.*;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class DemurrageDetentionPayableTransferMapper {

    /**
     * Convert Header Entity to DTO
     */
    public DemurrageDetentionPayableTransferDto mapToDto(ShipDemDetnTransferHdr entity) {
        if (entity == null) {
            return null;
        }

        return DemurrageDetentionPayableTransferDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .linePoid(entity.getLinePoid())
                .blType(entity.getBlType())
                .emptyFromDate(entity.getEmptyFromDate())
                .emptyToDate(entity.getEmptyToDate())
                .payableGlPoid(entity.getPayableGlPoid())
                .incomeGlPoid(entity.getIncomeGlPoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(DemurrageDetentionPayableTransferCreateDTO dto, ShipDemDetnTransferHdr entity, Long groupPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setLinePoid(dto.getLinePoid());
        entity.setBlType(dto.getBlType());
        entity.setEmptyFromDate(dto.getEmptyFromDate());
        entity.setEmptyToDate(dto.getEmptyToDate());
        entity.setPayableGlPoid(dto.getPayableGlPoid());
        entity.setIncomeGlPoid(dto.getIncomeGlPoid());

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
    public void mapUpdateDTOToEntity(DemurrageDetentionPayableTransferUpdateDTO dto, ShipDemDetnTransferHdr entity) {
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getLinePoid() != null) {
            entity.setLinePoid(dto.getLinePoid());
        }
        if (dto.getBlType() != null) {
            entity.setBlType(dto.getBlType());
        }
        if (dto.getEmptyFromDate() != null) {
            entity.setEmptyFromDate(dto.getEmptyFromDate());
        }
        if (dto.getEmptyToDate() != null) {
            entity.setEmptyToDate(dto.getEmptyToDate());
        }
        if (dto.getPayableGlPoid() != null) {
            entity.setPayableGlPoid(dto.getPayableGlPoid());
        }
        if (dto.getIncomeGlPoid() != null) {
            entity.setIncomeGlPoid(dto.getIncomeGlPoid());
        }

        // Update audit fields
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    // Detail mapping methods - Transfer DTL
    public DemurrageDetentionTransferDetailDto mapTransferDtlToDto(ShipDemDetnTransferDtl entity) {
        if (entity == null) return null;
        return DemurrageDetentionTransferDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .mainfestTransactionPoid(entity.getMainfestTransactionPoid())
                .linePoid(entity.getLinePoid())
                .jobNo(entity.getJobNo())
                .consignee(entity.getConsignee())
                .notify(entity.getNotify())
                .blNumber(entity.getBlNumber())
                .containerNo(entity.getContainerNo())
                .sailDate(entity.getSailDate())
                .arrivalDate(entity.getArrivalDate())
                .emptyIn(entity.getEmptyIn())
                .equipmentIsoType(entity.getEquipmentIsoType())
                .demurrageAcutal(entity.getDemurrageAcutal())
                .extraFreeDays(entity.getExtraFreeDays())
                .extraFreeDaysPrnpls(entity.getExtraFreeDaysPrnpls())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalCollectedDays(entity.getTotalCollectedDays())
                .totalCollectedAmt(entity.getTotalCollectedAmt())
                .totalShortExcessAmount(entity.getTotalShortExcessAmount())
                .totalPayableAmount(entity.getTotalPayableAmount())
                .totalIncomeAmount(entity.getTotalIncomeAmount())
                .netIncomeAmt(entity.getNetIncomeAmt())
                .isSelect(entity.getIsSelect())
                .build();
    }

    public ShipDemDetnTransferDtl mapTransferDtlFromDto(DemurrageDetentionTransferDetailDto dto, Long transactionPoid) {
        if (dto == null) return null;
        return ShipDemDetnTransferDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(dto.getDetRowId())
                .mainfestTransactionPoid(dto.getMainfestTransactionPoid())
                .linePoid(dto.getLinePoid())
                .jobNo(dto.getJobNo())
                .consignee(dto.getConsignee())
                .notify(dto.getNotify())
                .blNumber(dto.getBlNumber())
                .containerNo(dto.getContainerNo() != null ? dto.getContainerNo().trim() : null)
                .sailDate(dto.getSailDate())
                .arrivalDate(dto.getArrivalDate())
                .emptyIn(dto.getEmptyIn())
                .equipmentIsoType(dto.getEquipmentIsoType())
                .demurrageAcutal(dto.getDemurrageAcutal())
                .extraFreeDays(dto.getExtraFreeDays())
                .extraFreeDaysPrnpls(dto.getExtraFreeDaysPrnpls())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .totalCollectedDays(dto.getTotalCollectedDays())
                .totalCollectedAmt(dto.getTotalCollectedAmt())
                .totalShortExcessAmount(dto.getTotalShortExcessAmount())
                .totalPayableAmount(dto.getTotalPayableAmount())
                .totalIncomeAmount(dto.getTotalIncomeAmount())
                .netIncomeAmt(dto.getNetIncomeAmt())
                .isSelect(dto.getIsSelect() != null ? dto.getIsSelect() : "n")
                .build();
    }

    // Detail mapping methods - Bill DTL
    public DemurrageDetentionTransferBillDetailDto mapBillDtlToDto(ShipDemDtnTransferBillDtl entity) {
        if (entity == null) return null;
        return DemurrageDetentionTransferBillDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .glPoid(entity.getGlPoid())
                .glCompanyPoid(entity.getGlCompanyPoid())
                .billRefType(entity.getBillRefType())
                .billRefno(entity.getBillRefno())
                .billDueDate(entity.getBillDueDate())
                .description(entity.getDescription())
                .drAmt(entity.getDrAmt())
                .crAmt(entity.getCrAmt())
                .containerNo(entity.getContainerNo())
                .billwiseBalance(entity.getBillwiseBalance())
                .checkall(entity.getCheckall())
                .build();
    }

    public ShipDemDtnTransferBillDtl mapBillDtlFromDto(DemurrageDetentionTransferBillDetailDto dto, Long transactionPoid) {
        if (dto == null) return null;
        return ShipDemDtnTransferBillDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(dto.getDetRowId())
                .glPoid(dto.getGlPoid())
                .glCompanyPoid(dto.getGlCompanyPoid())
                .billRefType(dto.getBillRefType())
                .billRefno(dto.getBillRefno())
                .billDueDate(dto.getBillDueDate())
                .description(dto.getDescription())
                .drAmt(dto.getDrAmt())
                .crAmt(dto.getCrAmt())
                .containerNo(dto.getContainerNo())
                .billwiseBalance(dto.getBillwiseBalance())
                .checkall(dto.getCheckall() != null ? dto.getCheckall() : "N")
                .build();
    }

    // Helper methods to map lists
    public List<DemurrageDetentionTransferDetailDto> mapTransferDtlListToDto(List<ShipDemDetnTransferDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapTransferDtlToDto).collect(Collectors.toList());
    }

    public List<DemurrageDetentionTransferBillDetailDto> mapBillDtlListToDto(List<ShipDemDtnTransferBillDtl> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::mapBillDtlToDto).collect(Collectors.toList());
    }
}