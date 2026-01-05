package com.asg.shipping.linemasterthirdparty.entity;

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
public class ShipLineMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

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

    @Column(name = "BL_PREFIX", length = 10)
    private String blPrefix;

    @Column(name = "BL_REMARKS_COUNT", precision = 5)
    private Integer blRemarksCount;

    @Column(name = "AGENCY_STARTED_DATE")
    private LocalDate agencyStartedDate;

    @Column(name = "NEXT_RENEWAL_DATE")
    private LocalDate nextRenewalDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "THC_PAY_AND_COLLECT", length = 1)
    private String thcPayAndCollect;

    @Column(name = "BANK_GUARANTEE_AMT")
    private BigDecimal bankGuaranteeAmt;

    @Column(name = "BANK_GUARANTEE_PERIOD_FROM")
    private LocalDate bankGuaranteePeriodFrom;

    @Column(name = "BANK_GUARANTEE_PERIOD_TO")
    private LocalDate bankGuaranteePeriodTo;

    @Column(name = "BANK_GUARANTEE_EXPIRY")
    private LocalDate bankGuaranteeExpiry;

    @Column(name = "LINE_TYPE", length = 20)
    private String lineType;

    @Column(name = "CHAMBER_OF_COMMERCE", precision = 25)
    private Long chamberOfCommerce;

    @Column(name = "CHAMBER_OF_COMMERCE_EXPIRY")
    private LocalDate chamberOfCommerceExpiry;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "LINE_PORT_REFNO", length = 25)
    private String linePortRefno;

    @Column(name = "LINE_PORT_REGISTER_NAME", length = 25)
    private String linePortRegisterName;

    @Column(name = "TERMINAL_LINE_CODE", length = 25)
    private String terminalLineCode;

    @Column(name = "AGENCY_CONTRACT_START")
    private LocalDate agencyContractStart;

    @Column(name = "AGENCY_CONTRACT_END")
    private LocalDate agencyContractEnd;

    @Column(name = "BANK_GUARANTEE_NO", length = 25)
    private String bankGuaranteeNo;

    @Column(name = "BANK_GUARANTEE_BANK_POID")
    private Long bankGuaranteeBankPoid;

    @Column(name = "BL_PRINT_LINER", length = 1)
    private String blPrintLiner;

    @Column(name = "BANK_GUARANTEE_CURRENCY", length = 10)
    private String bankGuaranteeCurrency;

    @Column(name = "BL_PRINT_FORMAT", length = 2000)
    private String blPrintFormat;

    @Column(name = "BL_PRINT_RIDER", length = 2000)
    private String blPrintRider;

    @Column(name = "CONTAINER_FORM_VHENT", length = 1)
    private String containerFormVhent = "Y";

    @Column(name = "CONTAINER_FORM_RTN", length = 1)
    private String containerFormRtn = "Y";

    @Column(name = "LINE_SHORT_NAME", length = 25)
    private String lineShortName;

    @Column(name = "DO_PRINT_LINE", length = 1)
    private String doPrintLine = "Y";

    @Column(name = "LINE_VESSEL_TYPE_POID")
    private Long lineVesselTypePoid;

    @Column(name = "LINE_RANK", precision = 10)
    private Long lineRank;

    @Column(name = "OTHER_INVOICE_ALLOWED", length = 1)
    private String otherInvoiceAllowed = "N";

    @Column(name = "BILL_TO", length = 50)
    private String billTo;

    @Column(name = "REPORTING_TYPE", length = 50)
    private String reportingType = "WEEKLY";

    @Column(name = "REPORTING_DAY", length = 50)
    private String reportingDay = "THRUSDAY";

    @Column(name = "REPORT_DESCRIPTION", length = 200)
    private String reportDescription;

    @Column(name = "PRINCIPAL_POID")
    private Long principalPoid;

    @Column(name = "RCPT_PRINT_LINE", length = 25)
    private String rcptPrintLine = "Y";

    @Column(name = "LINE_NOTE", length = 1000)
    private String lineNote;

    @Column(name = "LINE_CATEGORY", length = 20)
    private String lineCategory = "NVO";

    @Column(name = "MIS_LINE_CATEGORY", length = 100)
    private String misLineCategory;

    @Column(name = "LINE_COST_POID", length = 100)
    private String lineCostPoid;

    @Column(name = "PRINCIPAL_DO_REQUIRED", length = 3)
    private String principalDoRequired = "N";

    @Column(name = "ADDRESS_POID")
    private Long addressPoid;
}
