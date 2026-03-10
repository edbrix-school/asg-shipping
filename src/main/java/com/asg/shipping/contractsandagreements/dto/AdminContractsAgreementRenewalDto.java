package com.asg.shipping.contractsandagreements.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminContractsAgreementRenewalDto {

    private Long detRowId;

    private LocalDate effectiveStartDate;

    private LocalDate expiryDate;

    private LocalDate renewalDate;

    private String lastUpdatedBy;

    private LocalDateTime lastUpdatedDate;
    private String actionType;

}
