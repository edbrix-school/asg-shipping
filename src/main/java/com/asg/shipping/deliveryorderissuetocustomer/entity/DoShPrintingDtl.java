package com.asg.shipping.deliveryorderissuetocustomer.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "DO_SH_PRINTING_DTL")
public class DoShPrintingDtl {

    @Id
    @Column(name = "DO_PRINT_POID", nullable = false)
    private Long doPrintPoid;

    @AuditIgnore
    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOCUMENT_COMPANY_POID")
    private Long documentCompanyPoid;

    @Column(name = "DO_DATE")
    private LocalDate doDate;

    @Column(name = "DO_NO", nullable = false, length = 20)
    private String doNo;

    @AuditIgnore
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @AuditIgnore
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DO_DELETED", length = 1)
    private String doDeleted;

    @Column(name = "DO_PRINTED", length = 1)
    private String doPrinted;

    @Column(name = "CNT_FORM_DLV_PRINTED", length = 1)
    private String cntFormDlvPrinted;

    @Column(name = "CNT_FORM_RTN_PRINTED", length = 1)
    private String cntFormRtnPrinted;

    @Column(name = "REPRINT_DO_DATE")
    private LocalDate reprintDoDate;

    @Column(name = "REPRINT_BY", length = 25)
    private String reprintBy;

    @Column(name = "DO_RELASED_ID_PERSON", length = 100)
    private String doReleasedIdPerson;

    @Column(name = "DO_RELASED_TO_PERSON", length = 100)
    private String doReleasedToPerson;

    @Column(name = "DO_RELASED_ADDRS_PERSON", length = 300)
    private String doReleasedAddrsPerson;

    @Column(name = "ORIGNAL_BL_RELEASE_CR", length = 25)
    private String orignalBlReleaseCr;

    @Column(name = "FIRST_PRINT_DO_DATE")
    private LocalDate firstPrintDoDate;

    @Column(name = "FIRST_PRINT_BY", length = 25)
    private String firstPrintBy;
}
