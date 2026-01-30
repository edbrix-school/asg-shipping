package com.asg.shipping.vesselvoyagecreation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoyageBlRow {
    private Long transactionPoid; // voyage poid
    private String blType;
    private String blIssueType;
    private String blNumber;
    private String shipperEdiName;
    private String consigneeEdiName;
    private String notify1EdiName;
    private String notify2EdiName;
    private String salesman;
    private Long blPoid;
    private String drilldownLinkInfo;
    private String isReffer;
    private String isImco;
    private String doPrinted;
}










