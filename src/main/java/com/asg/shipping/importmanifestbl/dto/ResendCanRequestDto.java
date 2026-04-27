package com.asg.shipping.importmanifestbl.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendCanRequestDto {
    @NotNull(message = "Transaction POID is required")
    private Long transactionPoId;
    private String updateDemurrage; // "Y", "N", or "C"
}
