package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenExportManifestResponse {

    private Long transactionPoid;
    private Long companyPoid;
    /** Legacy document code passed to DrillDownViewDocument_WithoutLoadingPage — always "100-104" */
    private String documentCode;
}
