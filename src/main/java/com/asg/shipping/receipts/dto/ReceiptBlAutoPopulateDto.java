package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptBlAutoPopulateDto {

    private Long blPoid;
    private Long companyPoid;
    private String blReleaseType;
    private String originalBlReleaseType;
    private BigDecimal printCustomerPoid;
    private BigDecimal chequeCompanyPoid;
    private String remarks;
}
