package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for validation
 */
@Data
public class ValidationResponse {
    private Boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ValidationResponse() {
        this.valid = true;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}

