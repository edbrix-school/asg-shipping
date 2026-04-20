package com.asg.shipping.linepayabletransfetasperreporting.util;


import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingCreateDTO;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingDtlDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingUpdateDTO;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtl;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferHdr;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class LinePayableTransferReportingMapper {

    /**
     * Convert Header Entity to DTO
     */
    public LinePayableTransferReportingDto mapToDto(ShipLineReportTransferHdr entity) {
        if (entity == null) {
            return null;
        }

        return LinePayableTransferReportingDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .transactionDate(entity.getTransactionDate())
                .linePoid(entity.getLinePoid())
                .blType(entity.getBlType())
                .reportStartDate(entity.getReportStartDate())
                .reportEndDate(entity.getReportEndDate())
                .docRef(entity.getDocRef())
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
    public void mapCreateDTOToEntity(LinePayableTransferReportingCreateDTO dto, ShipLineReportTransferHdr entity,
                                     Long groupPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate(): DateUtil.getCurrentDateInUserTimeZone());
        entity.setLinePoid(dto.getLinePoid());
        entity.setBlType(dto.getBlType());
        entity.setReportStartDate(dto.getReportStartDate());
        entity.setReportEndDate(dto.getReportEndDate());
        entity.setDocRef(dto.getDocRef());
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(LinePayableTransferReportingUpdateDTO dto, ShipLineReportTransferHdr entity) {
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getLinePoid() != null) {
            entity.setLinePoid(dto.getLinePoid());
        }
        if (dto.getBlType() != null) {
            entity.setBlType(dto.getBlType());
        }
        if (dto.getReportStartDate() != null) {
            entity.setReportStartDate(dto.getReportStartDate());
        }
        if (dto.getReportEndDate() != null) {
            entity.setReportEndDate(dto.getReportEndDate());
        }
        if (dto.getDocRef() != null) {
            entity.setDocRef(dto.getDocRef());
        }
    }

    /**
     * Convert Detail Entity to DTO
     */
    public LinePayableTransferReportingDtlDto mapDtlToDto(ShipLineReportTransferDtl entity) {
        if (entity == null) {
            return null;
        }

        return LinePayableTransferReportingDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .mainfestTransactionPoid(entity.getMainfestTransactionPoid())
                .blNumber(entity.getBlNumber())
                .acutalAmount(entity.getAcutalAmount())
                .totalAmountTransfer(entity.getTotalAmountTransfer())
                .isSelect(entity.getIsSelect())
                .chargePoid(entity.getChargePoid())
                .freightType(entity.getFreightType())
                .currencyCode(entity.getCurrencyCode())
                .currencyExchange(entity.getCurrencyExchange())
                .currencyAmount(entity.getCurrencyAmount())
                .build();
    }

    /**
     * Map Detail DTO to Entity
     */
    public ShipLineReportTransferDtl mapDtlFromDto(LinePayableTransferReportingDtlDto dto, Long transactionPoid, Long detRowId) {
        if (dto == null) {
            return null;
        }

        return ShipLineReportTransferDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .mainfestTransactionPoid(dto.getMainfestTransactionPoid())
                .blNumber(dto.getBlNumber())
                .acutalAmount(dto.getAcutalAmount())
                .totalAmountTransfer(dto.getTotalAmountTransfer() != null ? dto.getTotalAmountTransfer() : dto.getAcutalAmount())
                .isSelect(dto.getIsSelect() != null ? dto.getIsSelect() : "N")
                .chargePoid(dto.getChargePoid())
                .freightType(dto.getFreightType())
                .currencyCode(dto.getCurrencyCode())
                .currencyExchange(dto.getCurrencyExchange())
                .currencyAmount(dto.getCurrencyAmount())
                .build();
    }

    /**
     * Convert list of Detail Entities to DTOs
     */
    public List<LinePayableTransferReportingDtlDto> mapDtlListToDto(List<ShipLineReportTransferDtl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::mapDtlToDto)
                .collect(Collectors.toList());
    }
}
