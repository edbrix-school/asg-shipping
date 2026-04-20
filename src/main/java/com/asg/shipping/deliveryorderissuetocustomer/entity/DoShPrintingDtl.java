package com.asg.shipping.deliveryorderissuetocustomer.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "DO_SH_PRINTING_DTL")
public class DoShPrintingDtl extends BaseEntity {

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
