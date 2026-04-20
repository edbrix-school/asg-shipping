package com.asg.shipping.lineprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileAgreementDetailsResponse {
    private Long agreementPoid;
    private String agreementId;
    private String agreementType;
    private String agreementStatus;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String renewalCycle;
}

