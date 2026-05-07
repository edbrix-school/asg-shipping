package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading BL data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadBlDataRequestDTO {

    @NotNull(message = "LOV name is required")
    @NotBlank
    private String lovName;

    private Long transactionPoid;
}

