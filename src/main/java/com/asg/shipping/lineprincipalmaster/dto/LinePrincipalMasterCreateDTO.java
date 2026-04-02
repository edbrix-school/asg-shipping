package com.asg.shipping.lineprincipalmaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating Line Principal Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Principal Master creation request")
public class LinePrincipalMasterCreateDTO {

    @NotBlank(message = "Line Code is mandatory")
    @Size(max = 20, message = "Line code cannot exceed 20 characters")
    @Schema(description = "Line Code", required = true, example = "LINE001")
    private String lineCode;

    @NotBlank(message = "Line Name is mandatory")
    @Size(max = 100, message = "Line name cannot exceed 100 characters")
    @Schema(description = "Line Name", required = true, example = "ABC Shipping Line")
    private String lineName;

    @Size(max = 100, message = "Line name2 cannot exceed 100 characters")
    @Schema(description = "Line Name (Secondary)", example = "ABC Line Ltd")
    private String lineName2;

    @Size(max = 25, message = "Line short name cannot exceed 25 characters")
    @Schema(description = "Line Short Name", example = "ABC")
    private String lineShortName;

    @Size(max = 500, message = "Line address cannot exceed 500 characters")
    @Schema(description = "Line Address", example = "123 Shipping Street")
    private String lineAddress;

    @Schema(description = "Country POID", example = "100")
    private Long countryPoid;

    @Schema(description = "Currency POID", example = "200")
    private Long currencyPoid;

    @Schema(description = "Principal POID", example = "300")
    private Long principalPoid;

    @Schema(description = "Address POID", example = "400")
    private Long addressPoid;

    @Schema(description = "Company POID", example = "600")
    private Long companyPoid;

    @Size(max = 10, message = "BL prefix cannot exceed 10 characters")
    @Schema(description = "BL Prefix", example = "ABC")
    private String blPrefix;

    @Schema(description = "BL Remarks Count", example = "5")
    private Integer blRemarksCount;

    @Schema(description = "Agency Started Date", example = "2020-01-01")
    private LocalDate agencyStartedDate;

    @Schema(description = "Next Renewal Date", example = "2025-01-01")
    private LocalDate nextRenewalDate;

    @Schema(description = "Agency Contract Start", example = "2020-01-01")
    private LocalDate agencyContractStart;

    @Schema(description = "Agency Contract End", example = "2025-01-01")
    private LocalDate agencyContractEnd;

    @Pattern(regexp = "Y|N", message = "Active must be 'Y' or 'N'")
    @Size(max = 1, message = "Active cannot exceed 1 character")
    @Schema(description = "Active Status", example = "Y", allowableValues = {"Y", "N"})
    private String active;

    @Schema(description = "Sequence Number", example = "1")
    private Integer seqno;

    @Pattern(regexp = "Y|N", message = "THC Pay and Collect must be 'Y' or 'N'")
    @Size(max = 1, message = "THC Pay and Collect cannot exceed 1 character")
    @Schema(description = "THC Pay and Collect", example = "Y", allowableValues = {"Y", "N"})
    private String thcPayAndCollect;

    @Schema(description = "Bank Guarantee Amount", example = "100000.00")
    private BigDecimal bankGuaranteeAmt;

    @Schema(description = "Bank Guarantee Period From", example = "2020-01-01")
    private LocalDate bankGuaranteePeriodFrom;

    @Schema(description = "Bank Guarantee Period To", example = "2025-01-01")
    private LocalDate bankGuaranteePeriodTo;

    @Schema(description = "Bank Guarantee Expiry", example = "2025-12-31")
    private LocalDate bankGuaranteeExpiry;

    @Size(max = 25, message = "Bank guarantee number cannot exceed 25 characters")
    @Schema(description = "Bank Guarantee Number", example = "BG001")
    private String bankGuaranteeNo;

    @Schema(description = "Bank Guarantee Bank POID", example = "500")
    private Long bankGuaranteeBankPoid;

    @Size(max = 10, message = "Bank guarantee currency cannot exceed 10 characters")
    @Schema(description = "Bank Guarantee Currency", example = "USD")
    private String bankGuaranteeCurrency;

    @Size(max = 20, message = "Line type cannot exceed 20 characters")
    @Schema(description = "Line Type", example = "CARRIER")
    private String lineType;

    @Schema(description = "Chamber of Commerce", example = "123456")
    private Long chamberOfCommerce;

    @Schema(description = "Chamber of Commerce Expiry", example = "2025-12-31")
    private LocalDate chamberOfCommerceExpiry;

    @Size(max = 25, message = "Line port reference cannot exceed 25 characters")
    @Schema(description = "Line Port Reference (Tradelane)", example = "TRADE001")
    private String linePortRefno;

    @Size(max = 25, message = "Line port register name cannot exceed 25 characters")
    @Schema(description = "Line Port Register Name", example = "Port Register")
    private String linePortRegisterName;

    @Size(max = 25, message = "Terminal line code cannot exceed 25 characters")
    @Schema(description = "Terminal Line Code", example = "TLC001")
    private String terminalLineCode;

    @Pattern(regexp = "Y|N", message = "BL Print Liner must be 'Y' or 'N'")
    @Size(max = 1, message = "BL Print Liner cannot exceed 1 character")
    @Schema(description = "BL Print Liner", example = "Y", allowableValues = {"Y", "N"})
    private String blPrintLiner;

    @Size(max = 2000, message = "BL print format cannot exceed 2000 characters")
    @Schema(description = "BL Print Format", example = "Format text")
    private String blPrintFormat;

    @Size(max = 2000, message = "BL print rider cannot exceed 2000 characters")
    @Schema(description = "BL Print Rider", example = "Rider text")
    private String blPrintRider;

    @Pattern(regexp = "Y|N", message = "Container Form Vhent must be 'Y' or 'N'")
    @Size(max = 1, message = "Container Form Vhent cannot exceed 1 character")
    @Schema(description = "Container Form Delivery Print", example = "Y", allowableValues = {"Y", "N"})
    private String containerFormVhent;

    @Pattern(regexp = "Y|N", message = "Container Form Rtn must be 'Y' or 'N'")
    @Size(max = 1, message = "Container Form Rtn cannot exceed 1 character")
    @Schema(description = "Container Form Return Print", example = "Y", allowableValues = {"Y", "N"})
    private String containerFormRtn;

    @Pattern(regexp = "Y|N", message = "DO Print Line must be 'Y' or 'N'")
    @Size(max = 1, message = "DO Print Line cannot exceed 1 character")
    @Schema(description = "DO Print Line", example = "Y", allowableValues = {"Y", "N"})
    private String doPrintLine;

    @Size(max = 25, message = "Receipt print line cannot exceed 25 characters")
    @Schema(description = "Receipt Print Line", example = "RCPT001")
    private String rcptPrintLine;

    @Size(max = 1000, message = "Line note cannot exceed 1000 characters")
    @Schema(description = "Line Note", example = "Line notes")
    private String lineNote;

    @Size(max = 100, message = "MIS line category cannot exceed 100 characters")
    @Schema(description = "MIS Line Category", example = "NVOC")
    private String misLineCategory;

    @Size(max = 100, message = "Line cost POID cannot exceed 100 characters")
    @Schema(description = "Line Cost POID", example = "COST001")
    private String lineCostPoid;

    @Size(max = 3, message = "Principal DO required cannot exceed 3 characters")
    @Schema(description = "Principal DO Required", example = "YES")
    private String principalDoRequired;

    @Size(max = 20, message = "Line category cannot exceed 20 characters")
    @Schema(description = "Line Category", example = "CATEGORY1")
    private String lineCategory;

    @Size(max = 50, message = "Bill to cannot exceed 50 characters")
    @Schema(description = "Bill To (Principal Code)", example = "BILL001")
    private String billTo;

    @Size(max = 50, message = "Reporting type cannot exceed 50 characters")
    @Schema(description = "Reporting Type", example = "WEEKLY")
    private String reportingType;

    @Size(max = 50, message = "Reporting day cannot exceed 50 characters")
    @Schema(description = "Reporting Day", example = "MONDAY")
    private String reportingDay;

    @Size(max = 200, message = "Report description cannot exceed 200 characters")
    @Schema(description = "Report Description", example = "Weekly report")
    private String reportDescription;

    @Valid
    @Schema(description = "Charge Details")
    private List<ChargeDetailDto> charges;

    @Valid
    @Schema(description = "Container Type Details")
    private List<ContainerTypeDetailDto> containerTypes;

    @Valid
    @Schema(description = "User Role Details")
    private List<UserRoleDetailDto> userRoles;

    @Valid
    @Schema(description = "PIC Details")
    private List<PicDetailDto> picDetails;
}

