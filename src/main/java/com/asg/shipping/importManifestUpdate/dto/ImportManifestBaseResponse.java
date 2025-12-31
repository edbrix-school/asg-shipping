package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportManifestBaseResponse {

    private String transactionPoId;
    private String docRef;
    private ImportManifestBaseGeneralResponseDto general;
    private ImportManifestShipperResponse shipper;

}
