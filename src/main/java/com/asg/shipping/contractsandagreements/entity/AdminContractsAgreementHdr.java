package com.asg.shipping.contractsandagreements.entity;


import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ADMIN_CONTRACTS_AGREEMENT_HDR"
)
public class AdminContractsAgreementHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private Long transactionPoid;


    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDateTime transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "AGREEMENT_ID", length = 100)
    private String agreementId;

    @Column(name = "AGREEMENT_NAME", length = 200)
    private String agreementName;

    @Column(name = "AGREEMENT_TYPE", length = 50)
    private String agreementType;

    @Column(name = "AGREEMENT_CATEGORY", length = 50)
    private String agreementCategory;

    @Column(name = "AGREEMENT_DESCRIPTION", length = 1000)
    private String agreementDescription;

    @Column(name = "AGREEMENT_STATUS", length = 30)
    private String agreementStatus;

    @Column(name = "AGREEMENT_SOURCE", length = 20)
    private String agreementSource;


    @Column(name = "AGREEMENT_COMPANY_POID")
    private Long agreementCompanyPoid;

    @Column(name = "PARTY_TYPE", length = 50)
    private String partyType;

    @Column(name = "PARTY_POID")
    private Long partyPoid;

    @Column(name = "NEW_PARTY", length = 1)
    private String newParty;

    @Column(name = "NEW_PARTY_NAME", length = 200)
    private String newPartyName;

    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "PARTY_CONTACT_PERSON", length = 100)
    private String partyContactPerson;

    @Column(name = "PARTY_CONTACT_EMAIL", length = 100)
    private String partyContactEmail;

    @Column(name = "PARTY_CONTACT_PHONE", length = 30)
    private String partyContactPhone;

    @Column(name = "PARTY_ADDRESS", length = 1000)
    private String partyAddress;


    @Column(name = "REFERENCE_DATE")
    private LocalDateTime referenceDate;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDateTime effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    @Column(name = "NOTICE_PERIOD_DAYS")
    private Integer noticePeriodDays;

    @Column(name = "RENEWAL_TYPE", length = 30)
    private String renewalType;

    @Column(name = "RENEWAL_CYCLE", length = 30)
    private String renewalCycle;

    @Column(name = "RENEWAL_DUE_DATE")
    private LocalDateTime renewalDueDate;

    @Column(name = "LAST_RENEWAL_DATE")
    private LocalDateTime lastRenewalDate;


    @Column(name = "TOTAL_CONTRACT_VALUE", precision = 18, scale = 2)
    private BigDecimal totalContractValue;

    @Column(name = "ANNUAL_VALUE", precision = 18, scale = 2)
    private BigDecimal annualValue;

    @Column(name = "PAYMENT_TERMS", length = 50)
    private String paymentTerms;

    @Column(name = "PAYMENT_FREQUENCY", length = 30)
    private String paymentFrequency;



    @Column(name = "TERMINATED", length = 1)
    private String terminated;

    @Column(name = "TERMINATION_DATE")
    private LocalDateTime terminationDate;

    @Column(name = "TERMINATION_REASON", length = 500)
    private String terminationReason;


    @Column(name = "AGREEMENT_CAPTION", length = 200)
    private String agreementCaption;

    @Lob
    @Column(name = "AGREEMENT_CONTENT")
    private String agreementContent;

    @Column(name = "SIGNATORY", length = 500)
    private String signatory;


    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}
