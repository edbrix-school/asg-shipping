package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for validating customer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateCustomerRequestDTO {

    @NotNull(message = "Customer POID is required")
    private Long customerPoid;

    private String creditType;
    private String authorizedId;
}

