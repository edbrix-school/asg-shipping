package com.asg.shipping.shippingFFChargeMaster.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeDto {

    private Long chargePoid;
    private Long groupPoid;
    private String chargeCode;
    private String chargeName;
    private String chargeName2;
    private String chargeRevenueType;
    private String chargeType;
    private String chargeApplicableType;
    private String divisionCode;
    private Long chargeGlRevenue;
    //private LovGetListDto chargeGlRevenueDet;
    private Long chargeGlCost;
    //private LovGetListDto chargeGlCostDet;
    private Long chargeGlWip;
    //private LovGetListDto chargeGlWipDet;
    private Long chargePayableGl;
    //private LovGetListDto chargePayableGlDet;
    private Long fdaGlRevenue;
    //private LovGetListDto fdaGlRevenueDet;
    private Long fdaGlCost;
    //private LovGetListDto fdaGlCostDet;
    private Long directRevenueGl;
    //private LovGetListDto directRevenueGlDet; // LOV data for Direct Revenue GL
    private Long directCostOfSaleGl;
    //private LovGetListDto directCostOfSaleGlDet; // LOV data for Direct Cost of Sale GL
    private Long directPayableGl;
    //private LovGetListDto directPayableGlDet; // LOV data for Direct Payable GL
    private Long taxPoid;
    //private LovGetListDto taxDet; // LOV data for Output Tax
    private Long inputTaxPoid;
    //private LovGetListDto inputTaxDet; // LOV data for Input Tax
    private Long chargeGroupPoid;
    //private LovGetListDto chargeGroupDet; // LOV data for Charge Group
    private Long shFfChargeMap;
    //private LovGetListDto shFfChargeMapDet; // LOV data for SH To FF Charge Map
    private Long shFfChargeGlPoid;
    //private LovGetListDto shFfChargeGlPoidDet; // LOV data for SH to FF GL POID
    private String active;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private Long shFfChargeGlPoidRev;
    private Long oldChargeGlRevenue;
    private Long oldChargeGlCost;
    private String visibleInFf;

    private List<ShippingChargeLineResponseDto> shippingChargeLineResponseDtoList;
}
