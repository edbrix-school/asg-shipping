package com.asg.shipping.contractsandagreements.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractRenewalRequest {
    private Long transactionPoid;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private LocalDate lastRenewalDate;
}
