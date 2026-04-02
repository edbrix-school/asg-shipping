package com.asg.shipping.lineprincipalmaster.util;

import com.asg.shipping.lineprincipalmaster.dto.*;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.common.entity.ShipLineMasterType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class LinePrincipalMasterMapper {

    /**
     * Convert Entity to DTO
     */
    public LinePrincipalMasterDto mapToDto(ShipLineMaster entity) {
        if (entity == null) {
            return null;
        }

        return LinePrincipalMasterDto.builder()
                .linePoid(entity.getLinePoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .lineCode(entity.getLineCode())
                .lineName(entity.getLineName())
                .lineName2(entity.getLineName2())
                .lineShortName(entity.getLineShortName())
                .lineAddress(entity.getLineAddress())
                .countryPoid(entity.getCountryPoid())
                .currencyPoid(entity.getCurrencyPoid())
                .principalPoid(entity.getPrincipalPoid())
                .addressPoid(entity.getAddressPoid())
                .blPrefix(entity.getBlPrefix())
                .blRemarksCount(entity.getBlRemarksCount())
                .agencyStartedDate(entity.getAgencyStartedDate())
                .nextRenewalDate(entity.getNextRenewalDate())
                .agencyContractStart(entity.getAgencyContractStart())
                .agencyContractEnd(entity.getAgencyContractEnd())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .thcPayAndCollect(entity.getThcPayAndCollect())
                .bankGuaranteeAmt(entity.getBankGuaranteeAmt())
                .bankGuaranteePeriodFrom(entity.getBankGuaranteePeriodFrom())
                .bankGuaranteePeriodTo(entity.getBankGuaranteePeriodTo())
                .bankGuaranteeExpiry(entity.getBankGuaranteeExpiry())
                .bankGuaranteeNo(entity.getBankGuaranteeNo())
                .bankGuaranteeBankPoid(entity.getBankGuaranteeBankPoid())
                .bankGuaranteeCurrency(entity.getBankGuaranteeCurrency())
                .lineType(entity.getLineType())
                .chamberOfCommerce(entity.getChamberOfCommerce())
                .chamberOfCommerceExpiry(entity.getChamberOfCommerceExpiry())
                .linePortRefno(entity.getLinePortRefno())
                .linePortRegisterName(entity.getLinePortRegisterName())
                .terminalLineCode(entity.getTerminalLineCode())
                .blPrintLiner(entity.getBlPrintLiner())
                .blPrintFormat(entity.getBlPrintFormat())
                .blPrintRider(entity.getBlPrintRider())
                .containerFormVhent(entity.getContainerFormVhent())
                .containerFormRtn(entity.getContainerFormRtn())
                .doPrintLine(entity.getDoPrintLine())
                .rcptPrintLine(entity.getRcptPrintLine())
                .lineNote(entity.getLineNote())
                .misLineCategory(entity.getMisLineCategory())
                .lineCostPoid(entity.getLineCostPoid())
                .principalDoRequired(entity.getPrincipalDoRequired())
                .lineCategory(entity.getLineCategory())
                .billTo(entity.getBillTo())
                .reportingType(entity.getReportingType())
                .reportingDay(entity.getReportingDay())
                .reportDescription(entity.getReportDescription())
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
    public void mapCreateDTOToEntity(LinePrincipalMasterCreateDTO dto, ShipLineMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setLineCode(dto.getLineCode());
        entity.setLineName(dto.getLineName());
        entity.setLineName2(dto.getLineName2());
        entity.setLineShortName(dto.getLineShortName());
        entity.setLineAddress(dto.getLineAddress());
        entity.setCountryPoid(dto.getCountryPoid());
        entity.setCurrencyPoid(dto.getCurrencyPoid());
        entity.setPrincipalPoid(dto.getPrincipalPoid());
        entity.setAddressPoid(dto.getAddressPoid());
        entity.setBlPrefix(dto.getBlPrefix());
        entity.setBlRemarksCount(dto.getBlRemarksCount());
        entity.setAgencyStartedDate(dto.getAgencyStartedDate());
        entity.setNextRenewalDate(dto.getNextRenewalDate());
        entity.setAgencyContractStart(dto.getAgencyContractStart());
        entity.setAgencyContractEnd(dto.getAgencyContractEnd());
        entity.setSeqno(dto.getSeqno());
        entity.setThcPayAndCollect(dto.getThcPayAndCollect());
        entity.setBankGuaranteeAmt(dto.getBankGuaranteeAmt());
        entity.setBankGuaranteePeriodFrom(dto.getBankGuaranteePeriodFrom());
        entity.setBankGuaranteePeriodTo(dto.getBankGuaranteePeriodTo());
        entity.setBankGuaranteeExpiry(dto.getBankGuaranteeExpiry());
        entity.setBankGuaranteeNo(dto.getBankGuaranteeNo());
        entity.setBankGuaranteeBankPoid(dto.getBankGuaranteeBankPoid());
        entity.setBankGuaranteeCurrency(dto.getBankGuaranteeCurrency());
        entity.setLineType(dto.getLineType());
        entity.setChamberOfCommerce(dto.getChamberOfCommerce());
        entity.setChamberOfCommerceExpiry(dto.getChamberOfCommerceExpiry());
        entity.setLinePortRefno(dto.getLinePortRefno());
        entity.setLinePortRegisterName(dto.getLinePortRegisterName());
        entity.setTerminalLineCode(dto.getTerminalLineCode());
        entity.setBlPrintLiner(dto.getBlPrintLiner());
        entity.setBlPrintFormat(dto.getBlPrintFormat());
        entity.setBlPrintRider(dto.getBlPrintRider());
        entity.setContainerFormVhent(dto.getContainerFormVhent());
        entity.setContainerFormRtn(dto.getContainerFormRtn());
        entity.setDoPrintLine(dto.getDoPrintLine());
        entity.setRcptPrintLine(dto.getRcptPrintLine());
        entity.setLineNote(dto.getLineNote());
        entity.setLineCostPoid(dto.getLineCostPoid());
        entity.setPrincipalDoRequired(dto.getPrincipalDoRequired());
        entity.setLineCategory(dto.getLineCategory());
        entity.setBillTo(dto.getBillTo());
        entity.setReportingType(dto.getReportingType());
        entity.setReportingDay(dto.getReportingDay());
        entity.setReportDescription(dto.getReportDescription());

        // Set active status (default to Y if not provided)
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        } else {
            entity.setActive("Y");
        }

        // Set MIS line category (default to NVOC if not provided)
        if (dto.getMisLineCategory() != null && !dto.getMisLineCategory().isEmpty()) {
            entity.setMisLineCategory(dto.getMisLineCategory());
        } else {
            entity.setMisLineCategory("NVOC");
        }

        // Set audit fields

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Entity
     */
    public void mapUpdateDTOToEntity(LinePrincipalMasterUpdateDTO dto, ShipLineMaster entity, Long groupPoid, Long userPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setLineCode(dto.getLineCode());
        entity.setLineName(dto.getLineName());
        entity.setLineName2(dto.getLineName2());
        entity.setLineShortName(dto.getLineShortName());
        entity.setLineAddress(dto.getLineAddress());
        entity.setCountryPoid(dto.getCountryPoid());
        entity.setCurrencyPoid(dto.getCurrencyPoid());
        entity.setPrincipalPoid(dto.getPrincipalPoid());
        entity.setAddressPoid(dto.getAddressPoid());
        entity.setBlPrefix(dto.getBlPrefix());
        entity.setBlRemarksCount(dto.getBlRemarksCount());
        entity.setAgencyStartedDate(dto.getAgencyStartedDate());
        entity.setNextRenewalDate(dto.getNextRenewalDate());
        entity.setAgencyContractStart(dto.getAgencyContractStart());
        entity.setAgencyContractEnd(dto.getAgencyContractEnd());
        entity.setSeqno(dto.getSeqno());
        entity.setThcPayAndCollect(dto.getThcPayAndCollect());
        entity.setBankGuaranteeAmt(dto.getBankGuaranteeAmt());
        entity.setBankGuaranteePeriodFrom(dto.getBankGuaranteePeriodFrom());
        entity.setBankGuaranteePeriodTo(dto.getBankGuaranteePeriodTo());
        entity.setBankGuaranteeExpiry(dto.getBankGuaranteeExpiry());
        entity.setBankGuaranteeNo(dto.getBankGuaranteeNo());
        entity.setBankGuaranteeBankPoid(dto.getBankGuaranteeBankPoid());
        entity.setBankGuaranteeCurrency(dto.getBankGuaranteeCurrency());
        entity.setLineType(dto.getLineType());
        entity.setChamberOfCommerce(dto.getChamberOfCommerce());
        entity.setChamberOfCommerceExpiry(dto.getChamberOfCommerceExpiry());
        entity.setLinePortRefno(dto.getLinePortRefno());
        entity.setLinePortRegisterName(dto.getLinePortRegisterName());
        entity.setTerminalLineCode(dto.getTerminalLineCode());
        entity.setBlPrintLiner(dto.getBlPrintLiner());
        entity.setBlPrintFormat(dto.getBlPrintFormat());
        entity.setBlPrintRider(dto.getBlPrintRider());
        entity.setContainerFormVhent(dto.getContainerFormVhent());
        entity.setContainerFormRtn(dto.getContainerFormRtn());
        entity.setDoPrintLine(dto.getDoPrintLine());
        entity.setRcptPrintLine(dto.getRcptPrintLine());
        entity.setLineNote(dto.getLineNote());
        entity.setLineCostPoid(dto.getLineCostPoid());
        entity.setPrincipalDoRequired(dto.getPrincipalDoRequired());
        entity.setLineCategory(dto.getLineCategory());
        entity.setBillTo(dto.getBillTo());
        entity.setReportingType(dto.getReportingType());
        entity.setReportingDay(dto.getReportingDay());
        entity.setReportDescription(dto.getReportDescription());

        // Set active status
        if (dto.getActive() != null && !dto.getActive().isEmpty()) {
            entity.setActive(dto.getActive());
        }

        // Set MIS line category
        if (dto.getMisLineCategory() != null && !dto.getMisLineCategory().isEmpty()) {
            entity.setMisLineCategory(dto.getMisLineCategory());
        }

        // Update audit fields (do not update createdBy/createdDate)
    }

    /**
     * Map charge detail entities to DTOs
     */
    public List<ChargeDetailDto> mapChargeDetailsToDto(List<ShipLineMasterChargeDtl> charges) {
        if (charges == null || charges.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return charges.stream()
                .map(this::mapChargeDetailToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map charge detail entity to DTO
     */
    public ChargeDetailDto mapChargeDetailToDto(ShipLineMasterChargeDtl charge) {
        if (charge == null) {
            return null;
        }

        return ChargeDetailDto.builder()
                .detRowId(charge.getDetRowId())
                .chargePoid(charge.getChargePoid())
                .lineChargeCode(charge.getLineChargeCode())
                .lineChargeDescription(charge.getLineChargeDescription())
                .validUntil(charge.getValidUntil())
                .remunCommissionCharge(charge.getRemunCommissionCharge())
                .excludedFromEdi(charge.getExcludedFromEdi())
                .defaultPrintGroupEdi(charge.getDefaultPrintGroupEdi())
                .wkyrptIncludeAs(charge.getWkyrptIncludeAs())
                .build();
    }

    /**
     * Map charge detail DTO to entity
     */
    public ShipLineMasterChargeDtl mapChargeDetailDtoToEntity(ChargeDetailDto dto, Long linePoid, String currentUser) {
        ShipLineMasterChargeDtl charge = ShipLineMasterChargeDtl.builder()
                .linePoid(linePoid)
                .chargePoid(dto.getChargePoid())
                .lineChargeCode(dto.getLineChargeCode())
                .lineChargeDescription(dto.getLineChargeDescription())
                .validUntil(dto.getValidUntil())
                .remunCommissionCharge(dto.getRemunCommissionCharge())
                .excludedFromEdi(dto.getExcludedFromEdi())
                .defaultPrintGroupEdi(dto.getDefaultPrintGroupEdi())
                .wkyrptIncludeAs(dto.getWkyrptIncludeAs())
                .build();

        return charge;
    }

    /**
     * Update charge detail entity from DTO
     */
    public void updateChargeDetailFromDto(ChargeDetailDto dto, ShipLineMasterChargeDtl charge, String currentUser) {
        charge.setChargePoid(dto.getChargePoid());
        charge.setLineChargeCode(dto.getLineChargeCode());
        charge.setLineChargeDescription(dto.getLineChargeDescription());
        charge.setValidUntil(dto.getValidUntil());
        charge.setRemunCommissionCharge(dto.getRemunCommissionCharge());
        charge.setExcludedFromEdi(dto.getExcludedFromEdi());
        charge.setDefaultPrintGroupEdi(dto.getDefaultPrintGroupEdi());
        charge.setWkyrptIncludeAs(dto.getWkyrptIncludeAs());
    }

    /**
     * Map container type detail entities to DTOs
     */
    public List<ContainerTypeDetailDto> mapContainerTypeDetailsToDto(List<ShipLineMasterType> containerTypes) {
        if (containerTypes == null || containerTypes.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return containerTypes.stream()
                .map(this::mapContainerTypeDetailToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map container type detail entity to DTO
     */
    public ContainerTypeDetailDto mapContainerTypeDetailToDto(ShipLineMasterType containerType) {
        if (containerType == null) {
            return null;
        }

        return ContainerTypeDetailDto.builder()
                .detRowId(containerType.getDetRowId())
                .containerTypePoid(containerType.getContainerTypePoid())
                .validUntil(containerType.getValidUntil())
                .build();
    }

    /**
     * Map container type detail DTO to entity
     */
    public ShipLineMasterType mapContainerTypeDetailDtoToEntity(ContainerTypeDetailDto dto, Long linePoid, String currentUser) {
        ShipLineMasterType containerType = new ShipLineMasterType();
        containerType.setLinePoid(linePoid);
        containerType.setContainerTypePoid(dto.getContainerTypePoid());
        containerType.setValidUntil(dto.getValidUntil());
        containerType.setCreatedBy(currentUser);
        containerType.setCreatedDate(LocalDateTime.now());

        return containerType;
    }

    /**
     * Update container type detail entity from DTO
     */
    public void updateContainerTypeDetailFromDto(ContainerTypeDetailDto dto, ShipLineMasterType containerType, String currentUser) {
        containerType.setContainerTypePoid(dto.getContainerTypePoid());
        containerType.setValidUntil(dto.getValidUntil());
        containerType.setLastModifiedBy(currentUser);
        containerType.setLastModifiedDate(LocalDateTime.now());
    }
}

