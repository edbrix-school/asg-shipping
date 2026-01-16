package com.asg.shipping.shippingmanifestcorrectoraddupdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading demurrage refund charges
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadDemurrageRefundRequest {

    @NotNull(message = "BL POID is required")
    private Long blPoid;
}
