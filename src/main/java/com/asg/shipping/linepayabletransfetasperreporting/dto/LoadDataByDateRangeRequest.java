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
}
