package com.asg.shipping.lineprincipalmaster.util;

import com.asg.common.lib.dto.AddressDetailsDTO;
import com.asg.common.lib.dto.AddressTypeMapDTO;
import com.asg.shipping.common.entity.GlobalAddressDetails;
import com.asg.shipping.lineprincipalmaster.dto.*;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
                .linePortRefnos(splitCodes(entity.getLinePortRefno()))
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
                .lineColor(entity.getLineColor())
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
        entity.setLineType("OWN");
        entity.setChamberOfCommerce(dto.getChamberOfCommerce());
        entity.setChamberOfCommerceExpiry(dto.getChamberOfCommerceExpiry());
        entity.setLinePortRefno(resolveLinePortRefno(dto.getLinePortRefno(), dto.getLinePortRefnos()));
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
        entity.setLineColor(dto.getLineColor());
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
        entity.setLinePortRefno(resolveLinePortRefno(dto.getLinePortRefno(), dto.getLinePortRefnos()));
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
        entity.setLineColor(dto.getLineColor());
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

            charge.setCreatedBy(currentUser);
            charge.setCreatedDate(LocalDateTime.now());

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
        charge.setLastModifiedBy(currentUser);
        charge.setLastModifiedDate(LocalDateTime.now());
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

    public List<UserRoleDetailDto> mapUserRoleDetailsToDto(List<ShipLineMasterUserRoleDtl> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return userRoles.stream().map(this::mapUserRoleDetailToDto).collect(Collectors.toList());
    }

    public UserRoleDetailDto mapUserRoleDetailToDto(ShipLineMasterUserRoleDtl userRole) {
        if (userRole == null) return null;
        return UserRoleDetailDto.builder()
                .detRowId(userRole.getDetRowId())
                .userRolePoid(userRole.getUserRolePoid())
                .validUntil(userRole.getValidUntil())
                .build();
    }

    public ShipLineMasterUserRoleDtl mapUserRoleDetailDtoToEntity(UserRoleDetailDto dto, Long linePoid, String currentUser) {
        ShipLineMasterUserRoleDtl userRole = new ShipLineMasterUserRoleDtl();
        userRole.setLinePoid(linePoid);
        userRole.setUserRolePoid(dto.getUserRolePoid());
        userRole.setValidUntil(dto.getValidUntil());
        userRole.setCreatedBy(currentUser);
        userRole.setCreatedDate(LocalDateTime.now());
        return userRole;
    }

    public void updateUserRoleDetailFromDto(UserRoleDetailDto dto, ShipLineMasterUserRoleDtl userRole, String currentUser) {
        userRole.setUserRolePoid(dto.getUserRolePoid());
        userRole.setValidUntil(dto.getValidUntil());
        userRole.setLastModifiedBy(currentUser);
        userRole.setLastModifiedDate(LocalDateTime.now());
    }

    public List<PicDetailDto> mapPicDetailsToDto(List<ShipLineMasterPicDtl> picDetails) {
        if (picDetails == null || picDetails.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return picDetails.stream().map(this::mapPicDetailToDto).collect(Collectors.toList());
    }

    public PicDetailDto mapPicDetailToDto(ShipLineMasterPicDtl picDtl) {
        if (picDtl == null) return null;
        return PicDetailDto.builder()
                .detRowId(picDtl.getDetRowId())
                .departmentPoid(picDtl.getDepartmentPoid())
                .handledUserPoid(picDtl.getHandledUserPoid())
                .periodFrom(picDtl.getPeriodFrom())
                .periodTo(picDtl.getPeriodTo())
                .remarks(picDtl.getRemarks())
                .build();
    }

    public ShipLineMasterPicDtl mapPicDetailDtoToEntity(PicDetailDto dto, Long linePoid, String currentUser) {
        ShipLineMasterPicDtl picDtl = new ShipLineMasterPicDtl();
        picDtl.setLinePoid(linePoid);
        picDtl.setDepartmentPoid(dto.getDepartmentPoid());
        picDtl.setHandledUserPoid(dto.getHandledUserPoid());
        picDtl.setPeriodFrom(dto.getPeriodFrom());
        picDtl.setPeriodTo(dto.getPeriodTo());
        picDtl.setRemarks(dto.getRemarks());
        picDtl.setCreatedBy(currentUser);
        picDtl.setCreatedDate(LocalDateTime.now());
        return picDtl;
    }

    public void updatePicDetailFromDto(PicDetailDto dto, ShipLineMasterPicDtl picDtl, String currentUser) {
        picDtl.setDepartmentPoid(dto.getDepartmentPoid());
        picDtl.setHandledUserPoid(dto.getHandledUserPoid());
        picDtl.setPeriodFrom(dto.getPeriodFrom());
        picDtl.setPeriodTo(dto.getPeriodTo());
        picDtl.setRemarks(dto.getRemarks());
        picDtl.setLastModifiedBy(currentUser);
        picDtl.setLastModifiedDate(LocalDateTime.now());
    }

    public AddressDetailsDTO mapToAddressDetailsDto(GlobalAddressDetails entity) {
        if (entity == null) return null;
        List<String> emails = new ArrayList<>();
        if (entity.getEmail1() != null) emails.add(entity.getEmail1());
        if (entity.getEmail2() != null) emails.add(entity.getEmail2());
        List<String> stateList = (entity.getState() != null && !entity.getState().isBlank())
                ? java.util.Arrays.asList(entity.getState().split(","))
                : null;
        return AddressDetailsDTO.builder()
                .addressPoid(entity.getAddressPoid() != null ? entity.getAddressPoid().toString() : null)
                .addressType(entity.getAddressType())
                .contactPerson(entity.getContactPerson())
                .designation(entity.getDesignation())
                .offTel1(entity.getOffTel1())
                .offTel2(entity.getOffTel2())
                .mobile(entity.getMobile())
                .fax(entity.getFax())
                .email(emails.isEmpty() ? null : emails)
                .website(entity.getWebsite())
                .poBox(entity.getPoBox())
                .offNo(entity.getOffNo())
                .bldg(entity.getBldg())
                .road(entity.getRoad())
                .area(entity.getAreaCity())
                .city(entity.getCity())
                .state(stateList)
                .landMark(entity.getLandMark())
                .verified(entity.getVerified())
                .verifiedBy(entity.getVerifiedBy())
                .verifiedDate(entity.getVerifiedDate() != null ? entity.getVerifiedDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate() : null)
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toLocalDateTime() : null)
                .lastModifiedBy(entity.getLastmodifiedBy())
                .lastModifiedDate(entity.getLastmodifiedDate() != null ? entity.getLastmodifiedDate().toLocalDateTime() : null)
                .whatsappNo(entity.getWhatsappNo())
                .linkedIn(entity.getLinkedin())
                .instagram(entity.getInstagram())
                .facebook(entity.getFacebook())
                .build();
    }

    private String resolveLinePortRefno(String singleCode, List<String> multiCodes) {
        if (multiCodes != null && !multiCodes.isEmpty()) {
            return String.join(",", multiCodes.stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }
        return singleCode;
    }

    private List<String> splitCodes(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toList());
    }
}

