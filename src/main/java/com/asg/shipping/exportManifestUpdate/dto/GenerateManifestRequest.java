package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

/**
 * Request DTO for generating manifest
 */
@Data
public class GenerateManifestRequest {
    private String manifestType; // "CARGO" or "FREIGHT"
    private Boolean includeCharges; // true for freight manifest, false for cargo manifest
}

