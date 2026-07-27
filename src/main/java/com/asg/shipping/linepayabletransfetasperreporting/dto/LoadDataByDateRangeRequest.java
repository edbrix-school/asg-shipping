package com.asg.shipping.linepayabletransfetasperreporting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for loading data by date range
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadDataByDateRangeRequest {

    @NotNull(message = "Line POID is required")
    private Long linePoid;

    @NotNull(message = "BL type is required")
    private String blType;

    @NotNull(message = "Report start date is required")
    private LocalDate reportStartDate;

    @NotNull(message = "Report end date is required")
    private LocalDate reportEndDate;

    /**
     * Charge filter passed to PROC_SHIP_REPORT_LINE_DATEWISE. Only ALL, FRTTHC and OTHERS are
     * accepted, and an absent value means ALL.
     *
     * <p>Legacy derives it from the page's two checkboxes, both of which start ticked:
     * Frt/Thc and Others both ticked gives ALL, only Frt/Thc gives FRTTHC, only Others gives
     * OTHERS, and neither ticked falls back to ALL. The caller owns that derivation.
     */
    @Builder.Default
    private String chargeFilter = "ALL";
}
