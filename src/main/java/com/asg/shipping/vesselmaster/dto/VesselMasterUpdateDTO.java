package com.asg.shipping.vesselmaster.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating Vessel Master
 * Note: VESSEL_CODE is not included as it's updateable only on insert in legacy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vessel Master update request")
public class VesselMasterUpdateDTO {

    @NotBlank(message = "Vessel Name is mandatory")
    @Size(max = 100, message = "Vessel name cannot exceed 100 characters")
    @Schema(description = "Vessel Name", required = true, example = "MV ABC Vessel")
    private String vesselName;

    @Size(max = 100, message = "Vessel name2 cannot exceed 100 characters")
    @Schema(description = "Vessel Name (Secondary)", example = "ABC Vessel Ltd")
    private String vesselName2;

    @Schema(description = "Line POID", example = "100")
    private Long linePoid;

    @Size(max = 100, message = "Owner cannot exceed 100 characters")
    @Schema(description = "Owner", example = "ABC Shipping Company")
    private String owner;

    @Schema(description = "Agent POID", example = "200")
    private Long agentPoid;

    @Size(max = 100, message = "Registration number cannot exceed 100 characters")
    @Schema(description = "Registration Number", example = "REG123456")
    private String registrationNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Registration Date", example = "2020-01-01")
    private LocalDate registrationDate;

    @Size(max = 50, message = "Country of registration cannot exceed 50 characters")
    @Schema(description = "Country of Registration", example = "Panama")
    private String countryOfRegistration;

    @Size(max = 100, message = "Flag of country cannot exceed 100 characters")
    @Schema(description = "Flag of Country", example = "Panama")
    private String flagOfCountry;

    @Schema(description = "Vessel Type POID", example = "300")
    private Long vesselTypePoid;

    @Size(max = 20, message = "Vessel type class cannot exceed 20 characters")
    @Schema(description = "Vessel Type Class", example = "CLASS1")
    private String vesselTypeClass;

    @Positive(message = "GRT must be positive")
    @Schema(description = "Gross Register Tonnage (GRT)", example = "50000.00")
    private BigDecimal grt;

    @Positive(message = "NRT must be positive")
    @Schema(description = "Net Register Tonnage (NRT)", example = "30000.00")
    private BigDecimal nrt;

    @Positive(message = "DWT must be positive")
    @Schema(description = "Deadweight Tonnage (DWT)", example = "60000.00")
    private BigDecimal dwt;

    @Positive(message = "Vessel length must be positive")
    @Schema(description = "Vessel Length", example = "300.50")
    private BigDecimal vesselLength;

    @Positive(message = "Beam must be positive")
    @Schema(description = "Beam Width", example = "40.20")
    private BigDecimal beam;

    @Positive(message = "Draft must be positive")
    @Schema(description = "Draft Depth", example = "12.50")
    private BigDecimal draft;

    @Positive(message = "Hatches must be positive")
    @Schema(description = "Number of Hatches", example = "5.20")
    private BigDecimal hatches;

    @Positive(message = "Bay hatch must be positive")
    @Schema(description = "Bay Hatch Count", example = "10.00")
    private BigDecimal bayhatch;

    @Size(max = 20, message = "IMO number cannot exceed 20 characters")
    @Schema(description = "IMO Number", example = "IMO1234567")
    private String imoNumber;

    @Size(max = 20, message = "Remarks cannot exceed 20 characters")
    @Schema(description = "Remarks", example = "Vessel remarks")
    private String remarks;

    @Pattern(regexp = "Y|N", message = "Active must be 'Y' or 'N'")
    @Size(max = 1, message = "Active cannot exceed 1 character")
    @Schema(description = "Active Status", example = "Y", allowableValues = {"Y", "N"})
    private String active;

    @Schema(description = "Sequence Number", example = "1")
    private Integer seqno;
}

