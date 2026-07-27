package com.asg.shipping.linepayabletransfetasperreporting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for the legacy "Apply New Exchange Rate" action.
 *
 * <p>Legacy applies the rate in the grid before the document is saved, so the rows to recalculate
 * travel with the request instead of being read from the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyExchangeRateRequest {

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotNull(message = "Currency rate is required")
    private BigDecimal currencyExchange;

    @Valid
    private List<LinePayableTransferReportingDtlDto> details;
}
