package com.asg.shipping.importmanifestbl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeDefaultsRequestDto {
    private Long chargePoid;
    private LocalDate transactionDate;
}
