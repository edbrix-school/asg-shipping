package com.asg.shipping.containertypeportchargestariff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOverlapResponseDto {
    private boolean overlapping;
    private List<OverlapConflictDto> conflicts;
}