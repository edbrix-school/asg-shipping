package com.asg.shipping.lineprincipalmaster.dto;

import com.asg.shipping.common.dto.LovItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Charge Detail (used in request and response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Master Charge Detail")
public class ChargeDetailDto {

    @Schema(description = "Detail Row ID (for updates, null for new charges)")
    private Long detRowId;

    @Schema(description = "Row action type", example = "ISUPDATED")
    private String actionType;

    @Schema(description = "Charge POID")
    private Long chargePoid;

    @Schema(description = "Charge details (from LOV)")
    private LovItem chargeDet;

    @Schema(description = "Line Charge Code")
    private String lineChargeCode;

    @Schema(description = "Line Charge Description")
    private String lineChargeDescription;

    @Schema(description = "Valid Until Date")
    private LocalDate validUntil;

    @Schema(description = "Remuneration Commission Charge (Y/N)")
    private String remunCommissionCharge;

    @Schema(description = "Excluded From EDI")
    private String excludedFromEdi;

    @Schema(description = "Default Print Group EDI")
    private String defaultPrintGroupEdi;

    @Schema(description = "Weekly Report Include As (Charge POID)")
    private Long wkyrptIncludeAs;

    @Schema(description = "Weekly Report Include As details (from LOV)")
    private LovItem wkyrptIncludeAsDet;
}

