package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

/**
 * Request DTO for generating BL print
 */
@Data
public class GenerateBlPrintRequest {
    private String draftOriginal; // "N" for original, "Y" for draft

    public String getDraftOriginalParam() {
        return "Y".equalsIgnoreCase(draftOriginal) ? "Y" : "N";
    }
}

