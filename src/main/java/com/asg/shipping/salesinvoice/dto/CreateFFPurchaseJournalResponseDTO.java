package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for creating FF purchase journal
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFFPurchaseJournalResponseDTO {

    private String ffPjNo;
    private String ffJobNo;
    private String status;
}

