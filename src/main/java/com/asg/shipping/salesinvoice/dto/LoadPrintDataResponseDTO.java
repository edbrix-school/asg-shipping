package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for loading print data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadPrintDataResponseDTO {

    private List<InvoicePrintDetailDto> printDetails;
    private String message;
}

