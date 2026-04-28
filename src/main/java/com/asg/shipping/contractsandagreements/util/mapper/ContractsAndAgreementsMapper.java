package com.asg.shipping.contractsandagreements.util.mapper;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementPicDtlDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.dto.ContractRenewalResponse;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;

import java.util.List;

public class ContractsAndAgreementsMapper {
    private ContractsAndAgreementsMapper() {
    }


    public static AdminContractsAgreementHdrDto mapToExportDto(
            AdminContractsAgreementHdr hdr,
            List<AdminContractsAgreementPicDtl> picDtls,
            List<AdminContractsAgreementRenewalEntity> renewalDtls) {

        return AdminContractsAgreementHdrDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .groupPoid(hdr.getGroupPoid())
                .companyPoid(hdr.getCompanyPoid())
                .transactionDate(hdr.getTransactionDate())
                .docRef(hdr.getDocRef())
                .agreementId(hdr.getAgreementId())
                .agreementName(hdr.getAgreementName())
                .agreementType(hdr.getAgreementType())
                .agreementCategory(hdr.getAgreementCategory())
                .agreementDescription(hdr.getAgreementDescription())
                .agreementStatus(hdr.getAgreementStatus())
                .agreementSource(hdr.getAgreementSource())
                .agreementCompanyPoid(hdr.getAgreementCompanyPoid())
                .partyType(hdr.getPartyType())
                .partyPoid(hdr.getPartyPoid())
                .newParty(hdr.getNewParty())
                .newPartyName(hdr.getNewPartyName())
                .linePoid(hdr.getLinePoid())
                .partyContactPerson(hdr.getPartyContactPerson())
                .partyContactEmail(hdr.getPartyContactEmail())
                .partyContactPhone(hdr.getPartyContactPhone())
                .partyAddress(hdr.getPartyAddress())
                .referenceDate(hdr.getReferenceDate())
                .effectiveDate(hdr.getEffectiveDate())
                .expiryDate(hdr.getExpiryDate())
                .noticePeriodDays(hdr.getNoticePeriodDays())
                .renewalType(hdr.getRenewalType())
                .renewalCycle(hdr.getRenewalCycle())
                .renewalDueDate(hdr.getRenewalDueDate())
                .lastRenewalDate(hdr.getLastRenewalDate())
                .totalContractValue(hdr.getTotalContractValue())
                .annualValue(hdr.getAnnualValue())
                .paymentTerms(hdr.getPaymentTerms())
                .paymentFrequency(hdr.getPaymentFrequency())
                .terminated(hdr.getTerminated())
                .terminationDate(hdr.getTerminationDate())
                .terminationReason(hdr.getTerminationReason())
                .agreementCaption(hdr.getAgreementCaption())
                .agreementContent(hdr.getAgreementContent())
                .signatory(hdr.getSignatory())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .agreementContentDetails(mapPicDtlList(picDtls))
                .renewalDetails(mapRenewalDtlList(renewalDtls))
                .build();
    }

    private static List<AdminContractsAgreementPicDtlDto> mapPicDtlList(
            List<AdminContractsAgreementPicDtl> picDtls) {
        if (picDtls == null) {
            return List.of();
        }

        return picDtls.stream()
                .map(pic -> AdminContractsAgreementPicDtlDto.builder()
                        .detRowId(pic.getId().getDetRowId())
                        .departmentPoid(pic.getDepartmentPoid())
                        .handledUserPoid(pic.getHandledUserPoid())
                        .periodFrom(pic.getPeriodFrom())
                        .periodTo(pic.getPeriodTo())
                        .remarks(pic.getRemarks())
                        .build())
                .toList();
    }

    private static AdminContractsAgreementRenewalDto mapRenewalDtlToDto(AdminContractsAgreementRenewalEntity renewal) {
        if (renewal == null) {
            return null;
        }

        return AdminContractsAgreementRenewalDto.builder()
                .detRowId(renewal.getId().getDetRowId())
                .effectiveStartDate(renewal.getEffectiveStartDate())
                .expiryDate(renewal.getExpiryDate())
                .renewalDate(renewal.getRenewalDate())
                .lastUpdatedBy(renewal.getLastModifiedBy())
                .lastUpdatedDate(renewal.getLastModifiedDate())
                .build();
    }

    private static List<AdminContractsAgreementRenewalDto> mapRenewalDtlList(
            List<AdminContractsAgreementRenewalEntity> renewalDtls) {
        if (renewalDtls == null) {
            return List.of();
        }

        return renewalDtls.stream()
                .map(ContractsAndAgreementsMapper::mapRenewalDtlToDto)
                .toList();
    }

    public static void updateHdrEntity(
            AdminContractsAgreementHdrDto dto,
            AdminContractsAgreementHdr entity) {
        if (dto == null || entity == null)
            return;

        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setAgreementName(dto.getAgreementName());
        entity.setAgreementType(dto.getAgreementType());
        entity.setAgreementCategory(dto.getAgreementCategory());
        entity.setAgreementDescription(dto.getAgreementDescription());
        entity.setAgreementStatus(dto.getAgreementStatus());
        entity.setAgreementSource(dto.getAgreementSource());
        entity.setAgreementCompanyPoid(dto.getAgreementCompanyPoid());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyPoid(dto.getPartyPoid());
        entity.setNewParty(dto.getNewParty());
        entity.setNewPartyName(dto.getNewPartyName());
        entity.setLinePoid(dto.getLinePoid());
        entity.setDeleted("N");
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setPartyContactPerson(dto.getPartyContactPerson());
        entity.setPartyContactEmail(dto.getPartyContactEmail());
        entity.setPartyContactPhone(dto.getPartyContactPhone());
        entity.setPartyAddress(dto.getPartyAddress());
        entity.setReferenceDate(dto.getReferenceDate());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setNoticePeriodDays(dto.getNoticePeriodDays());
        entity.setRenewalType(dto.getRenewalType());
        entity.setRenewalCycle(dto.getRenewalCycle());
        entity.setRenewalDueDate(dto.getRenewalDueDate());
        entity.setLastRenewalDate(dto.getLastRenewalDate());
        entity.setTotalContractValue(dto.getTotalContractValue());
        entity.setAnnualValue(dto.getAnnualValue());
        entity.setPaymentTerms(dto.getPaymentTerms());
        entity.setPaymentFrequency(dto.getPaymentFrequency());
        entity.setTerminated(dto.getTerminated());
        entity.setTerminationDate(dto.getTerminationDate());
        entity.setTerminationReason(dto.getTerminationReason());
        entity.setAgreementCaption(dto.getAgreementCaption());
        entity.setAgreementContent(dto.getAgreementContent());
        entity.setSignatory(dto.getSignatory());
    }

    /* ---------------- PIC DETAILS ---------------- */

    public static void updatePicDtlEntity(
            AdminContractsAgreementPicDtlDto dto,
            AdminContractsAgreementPicDtl entity) {
        if (dto == null || entity == null)
            return;

        entity.setDepartmentPoid(dto.getDepartmentPoid());
        entity.setHandledUserPoid(dto.getHandledUserPoid());
        entity.setPeriodFrom(dto.getPeriodFrom());
        entity.setPeriodTo(dto.getPeriodTo());
        entity.setRemarks(dto.getRemarks());
    }

    /* ---------------- RENEWAL DETAILS ---------------- */

    public static void updateRenewalDtlEntity(
            AdminContractsAgreementRenewalDto dto,
            AdminContractsAgreementRenewalEntity entity) {
        if (dto == null || entity == null)
            return;

        entity.setEffectiveStartDate(dto.getEffectiveStartDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setRenewalDate(dto.getRenewalDate());
    }

    public static AdminContractsAgreementRenewalEntity mapRenewalDtlDto(
            AdminContractsAgreementRenewalDto dto) {
        if (dto == null)
            return null;

        AdminContractsAgreementRenewalEntity entity = new AdminContractsAgreementRenewalEntity();

        // reuse update logic to avoid duplication
        updateRenewalDtlEntity(dto, entity);
        entity.setDeleted("N");

        return entity;
    }

    public static AdminContractsAgreementPicDtl mapPicDtlDtoToEntity(
            AdminContractsAgreementPicDtlDto dto) {
        if (dto == null)
            return null;

        AdminContractsAgreementPicDtl entity = new AdminContractsAgreementPicDtl();
        updatePicDtlEntity(dto, entity);

        return entity;
    }

    public static ContractRenewalResponse mapToContractRenewalResponse(AdminContractsAgreementRenewalEntity entity) {
        if (entity == null) {
            return null;
        }

        return ContractRenewalResponse.builder()
                .effectiveStartDate(entity.getEffectiveStartDate())
                .expiryDate(entity.getExpiryDate())
                .renewalDate(entity.getRenewalDate())
                .lastUpdatedBy(entity.getLastModifiedBy())
                .lastUpdatedOn(entity.getLastModifiedDate())
                .isNewlyCreated(true)
                .build();
    }
}
