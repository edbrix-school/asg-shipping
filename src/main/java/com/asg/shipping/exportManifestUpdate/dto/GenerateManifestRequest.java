package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

/**
 * Request DTO for generating manifest
 */
@Data
public class GenerateManifestRequest {
    private Boolean freightCargo; // true for freight manifest, false for cargo manifest

    public String getFreightCargoParam() {
        return Boolean.TRUE.equals(freightCargo) ? "TRUE" : "FALSE";
    }

    public boolean isCargoManifest() {
        return !Boolean.TRUE.equals(freightCargo);
    }
}

