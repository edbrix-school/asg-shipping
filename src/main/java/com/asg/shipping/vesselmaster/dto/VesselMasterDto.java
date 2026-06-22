package com.asg.shipping.vesselmaster.dto;

import com.asg.shipping.common.dto.LovItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Vessel Master details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vessel Master detail response")
public class VesselMasterDto {

    @Schema(description = "Vessel POID (Primary Key)")
    private Long vesselPoid;

    @Schema(description = "Group POID")
    private Long groupPoid;

    @Schema(description = "Vessel Code", required = true)
    private String vesselCode;

    @Schema(description = "Vessel Name", required = true)
    private String vesselName;

    @Schema(description = "Vessel Name (Secondary)")
    private String vesselName2;

    @Schema(description = "Line POID")
    private Long linePoid;

    @Schema(description = "Line details (from LOV)")
    private LovItem lineDet;

    @Schema(description = "Owner")
    private String owner;

    @Schema(description = "Agent POID")
    private Long agentPoid;

    @Schema(description = "Agent details (from LOV)")
    private LovItem agentDet;

    @Schema(description = "Registration Number")
    private String registrationNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Registration Date")
    private LocalDate registrationDate;

    @Schema(description = "Country of Registration")
    private String countryOfRegistration;

    @Schema(description = "Flag of Country")
    private String flagOfCountry;

    @Schema(description = "Vessel Type POID")
    private Long vesselTypePoid;

    @Schema(description = "Vessel Type details (from LOV)")
    private LovItem vesselTypeDet;

    @Schema(description = "Vessel Type Class")
    private String vesselTypeClass;

    @Schema(description = "Gross Register Tonnage (GRT)")
    private BigDecimal grt;

    @Schema(description = "Net Register Tonnage (NRT)")
    private BigDecimal nrt;

    @Schema(description = "Deadweight Tonnage (DWT)")
    private BigDecimal dwt;

    @Schema(description = "Vessel Length")
    private BigDecimal vesselLength;

    @Schema(description = "Beam Width")
    private BigDecimal beam;

    @Schema(description = "Draft Depth")
    private BigDecimal draft;

    @Schema(description = "Number of Hatches")
    private BigDecimal hatches;

    @Schema(description = "Bay Hatch Count")
    private BigDecimal bayhatch;

    @Schema(description = "IMO Number")
    private String imoNumber;

    @Schema(description = "Remarks")
    private String remarks;

    @Schema(description = "Line Name (denormalized)")
    private String lineName;

    @Schema(description = "Active Status (Y/N)")
    private String active;

    @Schema(description = "Sequence Number")
    private Integer seqno;

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

