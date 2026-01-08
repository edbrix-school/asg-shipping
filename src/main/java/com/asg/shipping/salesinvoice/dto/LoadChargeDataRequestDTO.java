package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading charge data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadChargeDataRequestDTO {

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    @NotBlank(message = "BL type invoice is required")
    private String blTypeInvoice;
}

