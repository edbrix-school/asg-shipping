package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for updating Sales Invoice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesInvoiceShippingUpdateDTO {

    private LocalDate transactionDate;
    private LocalDate invDate;
    private String jobnoPoid;
    private Long customerPoid;
    private Long customerAddrPoid;
    private String currencyCode;
    private BigDecimal currencyRate;
    private Integer creditDays;
    private String invoiceAgainst;
    private String lpoSrnNo;
    private LocalDate lpoSrnDate;
    private String authorizedId;
    private String zeroValueInvoice;
    private String invoiceType;
    private Long printCustomerPoid;
    private String blReleaseTypeOffice;
    private String orignalBlReleaseType;
    private String ccRef;
    private Long printInvoiceBankPoid;
    private Long bookingPartyPoid;
    private String ownInvoiceNo;
    private String invoiceTo;
    private LocalDate invoiceDeliveryDate;

    private List<SalesInvoiceContainerDtlDto> containerDetails;
    private List<SalesInvoiceChargesDtlDto> chargesDetails;
}

