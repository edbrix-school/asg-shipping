package com.asg.shipping.importmanifestupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailVerificationResponseDto {
    private String status;
    private boolean success;
    private boolean warning;
}
