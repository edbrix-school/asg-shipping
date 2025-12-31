package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailVerificationRequestDto {
    private boolean verified;
    private boolean verifiedWithSpecialC;
}
