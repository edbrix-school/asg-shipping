package com.asg.shipping.vvc.transhipment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranshipmentUpdateItem {
    private Long detRowId;
    private String containerNo;
    private String containerType;
    private String isoCode;
    private String status;
    private String origin;
    private String pol;
    private String isLoaded;
    private Long loadTransactionPoid;
    private String isRefer;
    private String refferTemp;
    private String imoCode1;
    private String unNo1;
    private String imoCode2;
    private String unNo2;
    private String loadWeightKg;
    private String weightTon;
    private String oogH;
    private String oogL;
    private String oogLW;
    private String oogRW;
    private String hsCode;
    private String hsShortname;
    private String slot;
    private String blading;
    private String outboundVessel;
    private String loadOrigin;
    private String loadFinalDestination;
}


