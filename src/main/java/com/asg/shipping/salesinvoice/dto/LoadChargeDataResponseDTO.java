package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for loading charge data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadChargeDataResponseDTO {

    private List<SalesInvoiceChargesDtlDto> charges;
    private BigDecimal demurrageAmount;
    private List<SalesInvoiceChargesDtlDto> lateCharges;
    private List<SalesInvoiceContainerDtlDto> containers;
}

