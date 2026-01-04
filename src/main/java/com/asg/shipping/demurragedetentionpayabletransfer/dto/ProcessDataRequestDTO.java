package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for processing and loading containers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessDataRequestDTO {

    private Long linePoid;
    private String blType;
    private LocalDate emptyFromDate;
    private LocalDate emptyToDate;
}
