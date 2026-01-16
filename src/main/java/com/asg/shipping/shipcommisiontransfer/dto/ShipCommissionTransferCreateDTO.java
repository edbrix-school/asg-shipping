package com.asg.shipping.shipcommisiontransfer.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Ship Commission Transfer record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipCommissionTransferCreateDTO {

    private LocalDate transactionDate;
    private Long voyageTransactionPoid;
    private String remarks;
    private BigDecimal currencyExchange;
    private String currencyCode;
    private Long fdaTransactionPoid;

    // Detail records
    private List<ShipCommissionDetailDto> commissionDetails;
}
