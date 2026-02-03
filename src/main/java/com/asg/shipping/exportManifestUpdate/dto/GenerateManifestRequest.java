package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

/**
 * Request DTO for generating manifest
 */
@Data
public class GenerateManifestRequest {
    private Boolean freightCargo; // true for freight manifest, false for cargo manifest
}

