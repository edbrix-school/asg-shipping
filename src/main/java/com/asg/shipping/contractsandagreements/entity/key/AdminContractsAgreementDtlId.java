package com.asg.shipping.contractsandagreements.entity.key;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminContractsAgreementDtlId implements Serializable {

    @Column(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;
}

