package com.asg.shipping.linemasterthirdparty.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "ShipLineMasterThirdParty")
@Table(name = "SHIP_LINE_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @AuditIgnore
    @Column(name = "LINE_CODE", length = 20, nullable = false)
    private String lineCode;

    @Column(name = "LINE_NAME", length = 100, nullable = false)
    private String lineName;

    @Column(name = "LINE_NAME2", length = 100)
    private String lineName2;

    @Column(name = "LINE_ADDRESS", length = 500)
    private String lineAddress;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @AuditIgnore
    @Column(name = "BL_PREFIX", length = 10)
    private String blPrefix;

    @AuditIgnore
    @Column(name = "BL_REMARKS_COUNT", precision = 5)
    private Integer blRemarksCount;

    @AuditIgnore
    @Column(name = "AGENCY_STARTED_DATE")
    private LocalDate agencyStartedDate;

    @AuditIgnore
    @Column(name = "NEXT_RENEWAL_DATE")
    private LocalDate nextRenewalDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqno;

    @AuditIgnore
    @Column(name = "DELETED", length = 1)
    private String deleted;

    @AuditIgnore
    @Column(name = "THC_PAY_AND_COLLECT", length = 1)
    private String thcPayAndCollect;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_AMT")
    private BigDecimal bankGuaranteeAmt;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_PERIOD_FROM")
    private LocalDate bankGuaranteePeriodFrom;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_PERIOD_TO")
    private LocalDate bankGuaranteePeriodTo;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_EXPIRY")
    private LocalDate bankGuaranteeExpiry;

    @AuditIgnore
    @Column(name = "LINE_TYPE", length = 20)
    private String lineType;

    @AuditIgnore
    @Column(name = "CHAMBER_OF_COMMERCE", precision = 25)
    private Long chamberOfCommerce;

    @AuditIgnore
    @Column(name = "CHAMBER_OF_COMMERCE_EXPIRY")
    private LocalDate chamberOfCommerceExpiry;

    @AuditIgnore
    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @AuditIgnore
    @Column(name = "LINE_PORT_REFNO", length = 25)
    private String linePortRefno;

    @AuditIgnore
    @Column(name = "LINE_PORT_REGISTER_NAME", length = 25)
    private String linePortRegisterName;

    @AuditIgnore
    @Column(name = "TERMINAL_LINE_CODE", length = 25)
    private String terminalLineCode;

    @AuditIgnore
    @Column(name = "AGENCY_CONTRACT_START")
    private LocalDate agencyContractStart;

    @AuditIgnore
    @Column(name = "AGENCY_CONTRACT_END")
    private LocalDate agencyContractEnd;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_NO", length = 25)
    private String bankGuaranteeNo;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_BANK_POID")
    private Long bankGuaranteeBankPoid;

    @AuditIgnore
    @Column(name = "BL_PRINT_LINER", length = 1)
    private String blPrintLiner;

    @AuditIgnore
    @Column(name = "BANK_GUARANTEE_CURRENCY", length = 10)
    private String bankGuaranteeCurrency;

    @AuditIgnore
    @Column(name = "BL_PRINT_FORMAT", length = 2000)
    private String blPrintFormat;

    @AuditIgnore
    @Column(name = "BL_PRINT_RIDER", length = 2000)
    private String blPrintRider;

    @AuditIgnore
    @Column(name = "CONTAINER_FORM_VHENT", length = 1)
    private String containerFormVhent = "Y";

    @AuditIgnore
    @Column(name = "CONTAINER_FORM_RTN", length = 1)
    private String containerFormRtn = "Y";

    @AuditIgnore
    @Column(name = "LINE_SHORT_NAME", length = 25)
    private String lineShortName;

    @AuditIgnore
    @Column(name = "DO_PRINT_LINE", length = 1)
    private String doPrintLine = "Y";

    @AuditIgnore
    @Column(name = "LINE_VESSEL_TYPE_POID")
    private Long lineVesselTypePoid;

    @AuditIgnore
    @Column(name = "LINE_RANK", precision = 10)
    private Long lineRank;

    @AuditIgnore
    @Column(name = "OTHER_INVOICE_ALLOWED", length = 1)
    private String otherInvoiceAllowed = "N";

    @Column(name = "BILL_TO", length = 50)
    private String billTo;

    @AuditIgnore
    @Column(name = "REPORTING_TYPE", length = 50)
    private String reportingType = "WEEKLY";

    @AuditIgnore
    @Column(name = "REPORTING_DAY", length = 50)
    private String reportingDay = "THRUSDAY";

    @AuditIgnore
    @Column(name = "REPORT_DESCRIPTION", length = 200)
    private String reportDescription;

    @AuditIgnore
    @Column(name = "PRINCIPAL_POID")
    private Long principalPoid;

    @AuditIgnore
    @Column(name = "RCPT_PRINT_LINE", length = 25)
    private String rcptPrintLine = "Y";

    @AuditIgnore
    @Column(name = "LINE_NOTE", length = 1000)
    private String lineNote;

    @AuditIgnore
    @Column(name = "LINE_CATEGORY", length = 20)
    private String lineCategory = "NVO";

    @AuditIgnore
    @Column(name = "MIS_LINE_CATEGORY", length = 100)
    private String misLineCategory;

    @AuditIgnore
    @Column(name = "LINE_COST_POID", length = 100)
    private String lineCostPoid;

    @AuditIgnore
    @Column(name = "PRINCIPAL_DO_REQUIRED", length = 3)
    private String principalDoRequired = "N";

    @AuditIgnore
    @Column(name = "ADDRESS_POID")
    private Long addressPoid;
}
