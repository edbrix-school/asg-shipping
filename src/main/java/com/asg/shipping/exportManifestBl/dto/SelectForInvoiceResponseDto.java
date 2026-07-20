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
    private String approvalStatus;
    private Long existingInvoiceTransactionPoid;
    private Long invoiceTransactionPoid;
    private String targetApiPath;
    private String message;
}
