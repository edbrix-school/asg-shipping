package com.asg.shipping.salesinvoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.*;

/**
 * Entity class for AR_SH_SALES_INVOICE_HDR table
 */
@Entity
@Table(name = "AR_SH_SALES_INVOICE_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class ArShSalesInvoiceHdr {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 30)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "INV_DATE")
    private LocalDate invDate;

    @Column(name = "JOBNO_POID", length = 50)
    private String jobnoPoid;

    @Column(name = "CUSTOMER_POID", nullable = false)
    private Long customerPoid;

    @Column(name = "CUSTOMER_ADDR_POID")
    private Long customerAddrPoid;

    @Column(name = "CURRENCY_CODE", length = 10)
    private String currencyCode;

    @Column(name = "INV_AMOUNT", precision = 18, scale = 3)
    private BigDecimal invAmount;

    @Column(name = "CREDIT_DAYS")
    private Integer creditDays;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Column(name = "RELASED_ID_PERSON", length = 50)
    private String relasedIdPerson;

    @Column(name = "RELASED_TO_PERSON", length = 50)
    private String relasedToPerson;

    @Column(name = "RELASED_ADDRS_PERSON", length = 100)
    private String relasedAddrsPerson;

    @Column(name = "BL_POID")
    private Long blPoid;

    @Column(name = "PRINT_CUSTOMER_POID")
    private Long printCustomerPoid;

    @Column(name = "BL_RELEASE_TYPE_OFFICE", length = 20)
    private String blReleaseTypeOffice;

    @Column(name = "ORIGNAL_BL_RELEASE_TYPE", length = 20)
    private String orignalBlReleaseType;

    @Column(name = "BL_TYPE_INVOICE", length = 25)
    private String blTypeInvoice;

    @Column(name = "LPO_SRN_NO", length = 25)
    private String lpoSrnNo;

    @Column(name = "LPO_SRN_DATE")
    private LocalDate lpoSrnDate;

    @Column(name = "CURRENCY_RATE", precision = 18, scale = 6)
    private BigDecimal currencyRate;

    @Column(name = "INVOICE_AGAINST", length = 25)
    private String invoiceAgainst;

    @Column(name = "FF_JOB_NO", length = 25)
    private String ffJobNo;

    @Column(name = "FF_PJ_NO", length = 25)
    private String ffPjNo;

    @Column(name = "BOOKING_PARTY_POID")
    private Long bookingPartyPoid;

    @Column(name = "OWN_INVOICE_NO", length = 25)
    @Builder.Default
    private String ownInvoiceNo = "Y";

    @Column(name = "INVOICE_TO", length = 10)
    @Builder.Default
    private String invoiceTo = "C";

    @Column(name = "INVOICE_TYPE", length = 100)
    @Builder.Default
    private String invoiceType = "MANUAL";

    @Column(name = "AUTHORIZED_ID", length = 100)
    private String authorizedId;

    @Column(name = "ZERO_VALUE_INVOICE", length = 1)
    @Builder.Default
    private String zeroValueInvoice = "N";

    @Column(name = "CC_REF", length = 100)
    private String ccRef;

    @Column(name = "PRINT_INVOICE_BANK_POID")
    private Long printInvoiceBankPoid;

    @Column(name = "TIN_NUMBER", length = 100)
    private String tinNumber;

    @Column(name = "VERIFIED_BY_ACCOUNT", length = 10)
    @Builder.Default
    private String verifiedByAccount = "N";

    @Column(name = "CHANGE_POSTING", length = 1)
    @Builder.Default
    private String changePosting = "N";

    @Column(name = "EMAIL_SENT", length = 1)
    @Builder.Default
    private String emailSent = "N";

    @Column(name = "REPORT_GENERATED", length = 1)
    @Builder.Default
    private String reportGenerated = "N";

    @Column(name = "INVOICE_DELIVERY_DATE")
    private LocalDate invoiceDeliveryDate;

    @Column(name = "DELETED", length = 1)
    @Builder.Default
    private String deleted = "N";

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = getCurrentUser();
        }
        if (deleted == null) {
            deleted = "N";
        }
        if (ownInvoiceNo == null) {
            ownInvoiceNo = "Y";
        }
        if (invoiceTo == null) {
            invoiceTo = "C";
        }
        if (invoiceType == null) {
            invoiceType = "MANUAL";
        }
        if (zeroValueInvoice == null) {
            zeroValueInvoice = "N";
        }
        if (verifiedByAccount == null) {
            verifiedByAccount = "N";
        }
        if (changePosting == null) {
            changePosting = "N";
        }
        if (emailSent == null) {
            emailSent = "N";
        }
        if (reportGenerated == null) {
            reportGenerated = "N";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        if (lastModifiedBy == null) {
            lastModifiedBy = getCurrentUser();
        }
    }

}

