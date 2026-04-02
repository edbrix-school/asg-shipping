package com.asg.shipping.lineprincipalmaster.dto;

import com.asg.shipping.common.dto.LovItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Line Principal Master details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Principal Master detail response")
public class LinePrincipalMasterDto {

    @Schema(description = "Line POID (Primary Key)")
    private Long linePoid;

    @Schema(description = "Group POID")
    private Long groupPoid;

    @Schema(description = "Company POID")
    private Long companyPoid;

    @Schema(description = "Company details (from LOV)")
    private LovItem companyDet;

    @Schema(description = "Line Code", required = true)
    private String lineCode;

    @Schema(description = "Line Name", required = true)
    private String lineName;

    @Schema(description = "Line Name (Secondary)")
    private String lineName2;

    @Schema(description = "Line Short Name")
    private String lineShortName;

    @Schema(description = "Line Address")
    private String lineAddress;

    @Schema(description = "Country POID")
    private Long countryPoid;

    @Schema(description = "Country details (from LOV)")
    private LovItem countryDet;

    @Schema(description = "Currency POID")
    private Long currencyPoid;

    @Schema(description = "Currency details (from LOV)")
    private LovItem currencyDet;

    @Schema(description = "Principal POID")
    private Long principalPoid;

    @Schema(description = "Principal details (from LOV)")
    private LovItem principalDet;

    @Schema(description = "Address POID")
    private Long addressPoid;

    @Schema(description = "BL Prefix")
    private String blPrefix;

    @Schema(description = "BL Remarks Count")
    private Integer blRemarksCount;

    @Schema(description = "Agency Started Date")
    private LocalDate agencyStartedDate;

    @Schema(description = "Next Renewal Date")
    private LocalDate nextRenewalDate;

    @Schema(description = "Agency Contract Start")
    private LocalDate agencyContractStart;

    @Schema(description = "Agency Contract End")
    private LocalDate agencyContractEnd;

    @Schema(description = "Active Status (Y/N)")
    private String active;

    @Schema(description = "Sequence Number")
    private Integer seqno;

    @Schema(description = "THC Pay and Collect (Y/N)")
    private String thcPayAndCollect;

    @Schema(description = "Bank Guarantee Amount")
    private BigDecimal bankGuaranteeAmt;

    @Schema(description = "Bank Guarantee Period From")
    private LocalDate bankGuaranteePeriodFrom;

    @Schema(description = "Bank Guarantee Period To")
    private LocalDate bankGuaranteePeriodTo;

    @Schema(description = "Bank Guarantee Expiry")
    private LocalDate bankGuaranteeExpiry;

    @Schema(description = "Bank Guarantee Number")
    private String bankGuaranteeNo;

    @Schema(description = "Bank Guarantee Bank POID")
    private Long bankGuaranteeBankPoid;

    @Schema(description = "Bank details (from LOV)")
    private LovItem bankDet;

    @Schema(description = "Bank Guarantee Currency")
    private String bankGuaranteeCurrency;

    @Schema(description = "Line Type")
    private String lineType;

    @Schema(description = "Chamber of Commerce")
    private Long chamberOfCommerce;

    @Schema(description = "Chamber of Commerce Expiry")
    private LocalDate chamberOfCommerceExpiry;

    @Schema(description = "Line Port Reference (Tradelane)")
    private String linePortRefno;

    @Schema(description = "Tradelane details (from LOV)")
    private LovItem tradelaneDet;

    @Schema(description = "Line Port Register Name")
    private String linePortRegisterName;

    @Schema(description = "Terminal Line Code")
    private String terminalLineCode;

    @Schema(description = "BL Print Liner (Y/N)")
    private String blPrintLiner;

    @Schema(description = "BL Print Format")
    private String blPrintFormat;

    @Schema(description = "BL Print Rider")
    private String blPrintRider;

    @Schema(description = "Container Form Delivery Print (Y/N)")
    private String containerFormVhent;

    @Schema(description = "Container Form Return Print (Y/N)")
    private String containerFormRtn;

    @Schema(description = "DO Print Line (Y/N)")
    private String doPrintLine;

    @Schema(description = "Receipt Print Line")
    private String rcptPrintLine;

    @Schema(description = "Line Note")
    private String lineNote;

    @Schema(description = "MIS Line Category")
    private String misLineCategory;

    @Schema(description = "Line Cost POID")
    private String lineCostPoid;

    @Schema(description = "Principal DO Required")
    private String principalDoRequired;

    @Schema(description = "Line Category")
    private String lineCategory;

    @Schema(description = "Bill To (Principal Code)")
    private String billTo;

    @Schema(description = "Bill To details (from LOV)")
    private LovItem billToDet;

    @Schema(description = "Reporting Type")
    private String reportingType;

    @Schema(description = "Reporting Day")
    private String reportingDay;

    @Schema(description = "Report Description")
    private String reportDescription;

    @Schema(description = "Charge Details")
    private List<ChargeDetailDto> charges;

    @Schema(description = "Container Type Details")
    private List<ContainerTypeDetailDto> containerTypes;

    @Schema(description = "Created By")
    private String createdBy;

    @Schema(description = "Created Date")
    private LocalDateTime createdDate;

    @Schema(description = "Last Modified By")
    private String lastModifiedBy;

    @Schema(description = "Last Modified Date")
    private LocalDateTime lastModifiedDate;

    @Schema(description = "Deleted Flag (Y/N)")
    private String deleted;
}

