package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportManifestShipperResponse {

    private Long shipperPoid;
    private Long shipperAddressPoid;
    private Long consigneePoid;
    private Boolean manifestEmailVerified;







}
