package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesInvoiceContainerDtlResponseDTO {

    private Long blPoid;

    private String containerSocYn;

    private String containerNo;

    private LocalDate dmFrmDate;

    private LocalDate dmToDate;

    private BigDecimal dmDays;

    private BigDecimal dmChargeAmt;
}