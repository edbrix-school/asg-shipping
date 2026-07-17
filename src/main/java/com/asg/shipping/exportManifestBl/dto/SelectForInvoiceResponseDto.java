package com.asg.shipping.exportManifestBl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectForInvoiceResponseDto {
    private Long blPoid;
    private String documentId;
    private String documentName;
    private Long existingInvoiceTransactionPoid;
}
