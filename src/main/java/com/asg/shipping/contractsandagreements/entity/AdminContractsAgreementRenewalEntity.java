package com.asg.shipping.contractsandagreements.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ADMIN_CONTRACTS_AGREEMENT_RENEWAL_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementRenewalEntity extends BaseEntity {

    @EmbeddedId
    private AdminContractsAgreementDtlId id;

    @Column(name = "EFFECTIVE_START_DATE")
    private LocalDate effectiveStartDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "RENEWAL_DATE")
    private LocalDate renewalDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}
