package com.asg.shipping.contractsandagreements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementHdrDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String agreementId;
    @NotBlank(message = "Agreement Name is required")
    private String agreementName;
    @NotBlank(message = "Agreement Type is required")
    private String agreementType;
    private String agreementCategory;
    private String agreementDescription;
    @NotBlank(message = "Agreement Status is required")
    private String agreementStatus;
    @NotBlank(message = "Agreement Source is required")
    private String agreementSource;

    private Long agreementCompanyPoid;
    private String partyType;
    @NotNull(message = "Party Name is required")
    private Long partyPoid;
    private String newParty;
    private String newPartyName;
    private Long linePoid;
    private String partyContactPerson;
    private String partyContactEmail;
    @NotBlank(message = "Phone Number is required")
    private String partyContactPhone;
    private String partyAddress;

    private LocalDate referenceDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Integer noticePeriodDays;
    @NotBlank(message = "Renewal Type is required")
    private String renewalType;
    @NotBlank(message = "Renewal Cycle is required")
    private String renewalCycle;
    private LocalDate renewalDueDate;
    private LocalDate lastRenewalDate;

    private BigDecimal totalContractValue;
    private BigDecimal annualValue;
    private String paymentTerms;
    private String paymentFrequency;

    private String terminated;
    private LocalDate terminationDate;
    private String terminationReason;

    private String agreementCaption;
    private String agreementContent;
    private String signatory;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private List<AdminContractsAgreementRenewalDto> renewalDetails;
    private List<AdminContractsAgreementPicDtlDto> agreementContentDetails;

}
