package com.asg.shipping.salesinvoice.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Sales Invoice Shipping header
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesInvoiceShippingDto {

    private Long transactionPoid;
    private Long groupPoid;
    private LovGetListDto groupDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String docRef;
    private LocalDate transactionDate;
    private LocalDate invDate;
    private String jobnoPoid;
    private Long customerPoid;
    private LovGetListDto customerDet;
    private Long customerAddrPoid;
    private String currencyCode;
    private LovGetListDto currencyDet;
    private BigDecimal invAmount;
    private Integer creditDays;
    private LocalDate dueDate;
    private String relasedIdPerson;
    private String relasedToPerson;
    private String relasedAddrsPerson;
    private Long blPoid;
    private LovGetListDto blDet;
    private Long printCustomerPoid;
    private String blReleaseTypeOffice;
    private String orignalBlReleaseType;
    private String blTypeInvoice;
    private LovGetListDto blTypeInvoiceDet;
    private String lpoSrnNo;
    private LocalDate lpoSrnDate;
    private BigDecimal currencyRate;
    private String invoiceAgainst;
    private String ffJobNo;
    private String ffPjNo;
    private Long bookingPartyPoid;
    private LovGetListDto bookingPartyDet;
    private String ownInvoiceNo;
    private String invoiceTo;
    private String invoiceType;
    private String authorizedId;
    private String zeroValueInvoice;
    private String ccRef;
    private Long printInvoiceBankPoid;
    private LovGetListDto printInvoiceBankDet;
    private String tinNumber;
    private String verifiedByAccount;
    private String changePosting;
    private String emailSent;
    private String reportGenerated;
    private LocalDate invoiceDeliveryDate;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Detail records
    private List<SalesInvoiceContainerDtlDto> containerDetails;
    private List<SalesInvoiceChargesDtlDto> chargesDetails;
}

