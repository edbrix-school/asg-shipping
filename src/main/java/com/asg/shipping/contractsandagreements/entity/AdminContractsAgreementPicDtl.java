package com.asg.shipping.contractsandagreements.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ADMIN_CONTRACTS_AGREEMENT_PIC_DTL"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementPicDtl {

    @EmbeddedId
    private AdminContractsAgreementDtlId id;


    @Column(name = "DEPARTMENT_POID")
    private Long departmentPoid;

    @Column(name = "HANDLED_USER_POID")
    private Long handledUserPoid;

    @Column(name = "PERIOD_FROM")
    private LocalDateTime periodFrom;

    @Column(name = "PERIOD_TO")
    private LocalDateTime periodTo;

    @Column(name = "REMARKS", length = 500)
    private String remarks;


    @AuditIgnore
    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @AuditIgnore
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
