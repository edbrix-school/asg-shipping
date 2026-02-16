package com.asg.shipping.exportManifestUpdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for quotation after browse
 */
@Data
public class QuotationAfterBrowseRequest {
    @NotNull(message = "Quotation Transaction POID is required")
    private Long quotationTransactionPoid;
}

