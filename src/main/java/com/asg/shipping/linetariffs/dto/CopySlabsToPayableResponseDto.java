package com.asg.shipping.linetariffs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopySlabsToPayableResponseDto {

    private boolean requiresConfirmation;
    private boolean copied;
}