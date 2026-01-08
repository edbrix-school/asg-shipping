package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading print data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadPrintDataRequestDTO {

    @NotNull(message = "Customer POID is required")
    private Long customerPoid;
}

