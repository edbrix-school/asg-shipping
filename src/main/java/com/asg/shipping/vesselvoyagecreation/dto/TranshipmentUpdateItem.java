package com.asg.shipping.vesselvoyagecreation.dto;

import com.asg.shipping.exportManifestUpdate.dto.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranshipmentUpdateItem {
    private ActionType actionType;
    private Long detRowId;
    private String containerNo;
    private String containerType;
    private String sealNo;
    private String sealNo2;
    private String sealNo3;
    private String sealKindCode;
    private String sealKindCode1;
    private String isoCode;
    private String status;
    private String origin;
    private String pol;
    private String isLoaded;
    private Long loadTransactionPoid;
    private String isRefer;
    private String refferTemp;
    private String refferHum;
    private String refferVent;
    private String imcoClassActual;
    private String imo;
    private String imoCode1;
    private String unNo1;
    private String imoCode2;
    private String unNo2;
    private String loadWeightKg;
    private String weightKg;
    private String weightTon;
    private String oogH;
    private String oogL;
    private String oogLW;
    private String oogRW;
    private String oogB;
    private String oogF;
    private String oogA;
    private String oogType;
    private String hsCode;
    private String hsShortname;
    private String slot;
    private String blading;
    private String outboundVessel;
    private String loadOrigin;
    private String loadFinalDestination;
}










