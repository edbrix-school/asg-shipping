package com.asg.shipping.importmanifestupdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailVerificationRequestDto {
    @NotNull(message = "Transaction POID is required")
    private Long transactionPoId;
    
    private boolean verified;
    private boolean verifiedWithSpecialC;
}
