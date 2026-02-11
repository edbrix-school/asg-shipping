package com.asg.shipping.contractsandagreements.entity;

import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ADMIN_CONTRACTS_AGREEMENT_RENEWAL_DTL"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementRenewalEntity {

    @EmbeddedId
    private AdminContractsAgreementDtlId id;


    @Column(name = "EFFECTIVE_START_DATE")
    private LocalDateTime effectiveStartDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    @Column(name = "RENEWAL_DATE")
    private LocalDateTime renewalDate;


    @Column(name = "LAST_UPDATED_BY", length = 50)
    private String lastUpdatedBy;

    @Column(name = "LAST_UPDATED_DATE")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}
