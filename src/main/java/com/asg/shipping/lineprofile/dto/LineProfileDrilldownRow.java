package com.asg.shipping.lineprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileDrilldownRow {
    private Long   transactionPoid;
    private Long   companyPoid;
    private String recordFetchType;
}
