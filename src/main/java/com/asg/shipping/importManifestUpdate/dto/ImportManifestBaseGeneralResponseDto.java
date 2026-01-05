package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportManifestBaseGeneralResponseDto {

    private Long voyageId;
    private String blNumber;
    private String blType;
    private String cargoType;
    private Long noOfOriginalBls;
    private String freightType;
    private Long quotationTransactionPoid;
    private Long salesmanPoid;
    private String blIssueType;
    private Boolean bookedByPp;
    private String allInOneFreight;
    private String remarks;







}
