package com.asg.shipping.shipcommisiontransfer.dto;


import com.asg.shipping.common.dto.LovItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for SHIP_BL_COMMISSION_HDR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipCommissionTransferDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private LocalDate transactionDate;
    private Long voyageTransactionPoid;
    private LovItem voyageTransactionPoidDet; // LOV data
    private String remarks;
    private String deleted;
    private BigDecimal currencyExchange;
    private String currencyCode;
    private Long fdaTransactionPoid;
    private LovItem fdaTransactionPoidDet; // LOV data
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Detail records
    private List<ShipCommissionDetailDto> commissionDetails;
}
