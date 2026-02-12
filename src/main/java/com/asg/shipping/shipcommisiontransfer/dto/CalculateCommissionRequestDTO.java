package com.asg.shipping.shipcommisiontransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for calculate commission operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateCommissionRequestDTO {

    private Boolean recalculateAll;
    private List<Long> selectedDetailIds;
}
