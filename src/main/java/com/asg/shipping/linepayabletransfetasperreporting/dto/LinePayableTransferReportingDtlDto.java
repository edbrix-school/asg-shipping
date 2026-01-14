package com.asg.shipping.linepayabletransfetasperreporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Line Payable Transfer As Per Reporting Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinePayableTransferReportingDtlDto {

    private Long detRowId;
    private Long mainfestTransactionPoid;
    private String blNumber;
    private BigDecimal acutalAmount;
    private BigDecimal totalAmountTransfer;
    private String isSelect;
    private Long chargePoid;
    private String chargeCode;
    private String chargeDescription;
    private String freightType;
    private String currencyCode;
    private String currencyName;
    private BigDecimal currencyExchange;
    private BigDecimal currencyAmount;
}
