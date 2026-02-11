package com.asg.shipping.contractsandagreements.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminContractsAgreementRenewalDto {

    private Long detRowId;

    private LocalDateTime effectiveStartDate;

    private LocalDateTime expiryDate;

    private LocalDateTime renewalDate;

    private String lastUpdatedBy;

    private LocalDateTime lastUpdatedDate;
    private String actionType;

}
