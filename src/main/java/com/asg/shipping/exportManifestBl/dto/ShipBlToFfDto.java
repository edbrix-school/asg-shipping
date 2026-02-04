package com.asg.shipping.exportManifestBl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlToFfDto {

    private Long rnumid;
    private String masterBlNo;
    private Long shippingManifestPoid;
    private String shippingInvoice;
    private String ffJobno;
    private String ffInvoice;
    private String ffPj;
    private String deleted;
}

