package com.asg.shipping.contractsandagreements.entity;
import com.asg.common.lib.entity.BaseEntity;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ADMIN_CONTRACTS_AGREEMENT_PIC_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementPicDtl extends BaseEntity {

    @EmbeddedId
    private AdminContractsAgreementDtlId id;

    @Column(name = "DEPARTMENT_POID")
    private Long departmentPoid;

    @Column(name = "HANDLED_USER_POID")
    private Long handledUserPoid;

    @Column(name = "PERIOD_FROM")
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO")
    private LocalDate periodTo;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}
