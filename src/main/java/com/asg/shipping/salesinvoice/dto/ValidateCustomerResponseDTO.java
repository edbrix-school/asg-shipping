package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for customer validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateCustomerResponseDTO {

    private Boolean valid;
    private Integer creditDays;
    private List<String> warnings;
    private String errorMessage;
}

