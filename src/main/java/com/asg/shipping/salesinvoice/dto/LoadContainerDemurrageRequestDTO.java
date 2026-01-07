package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for loading container demurrage data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadContainerDemurrageRequestDTO {

    @NotNull(message = "BL POID is required")
    private Long blPoid;
}

