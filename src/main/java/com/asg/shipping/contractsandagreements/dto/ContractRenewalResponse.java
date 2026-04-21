package com.asg.shipping.contractsandagreements.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractRenewalResponse {
    private LocalDate effectiveStartDate;
    private LocalDate expiryDate;
    private LocalDate renewalDate;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedOn;
    private boolean isNewlyCreated;
}
