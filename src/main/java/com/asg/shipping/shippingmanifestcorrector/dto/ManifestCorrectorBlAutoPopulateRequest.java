package com.asg.shipping.shippingmanifestcorrector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for BL after browse auto-population.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorBlAutoPopulateRequest {

    private Long transactionPoid;
}
