package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for getting bill company from customer master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetBillCompanyRequestDTO {

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    @NotNull(message = "Customer POID is required")
    private Long customerPoid;
}

