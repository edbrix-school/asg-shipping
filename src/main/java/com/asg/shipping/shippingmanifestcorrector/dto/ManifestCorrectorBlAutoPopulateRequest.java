package com.asg.shipping.shippingmanifestcorrector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Request DTO for BL after browse auto-population.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorBlAutoPopulateRequest {

    private Long transactionPoid;

    /** JSON array of poids: [605925, 605898, 605848] */
    private List<Long> blPoids;

    /** Semicolon-separated poid string: "605925;605898;605848" */
    private String blPoidsStr;

    public List<Long> resolvedBlPoids() {
        if (blPoids != null && !blPoids.isEmpty()) {
            return blPoids;
        }
        if (blPoidsStr != null && !blPoidsStr.isBlank()) {
            return Arrays.stream(blPoidsStr.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
