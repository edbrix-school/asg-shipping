package com.asg.shipping.commoditymaster.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommodityMasterResponse {

    private Long commodityPoid;
    private Long groupPoid;
    private String commodityCode;
    private String commodityName;
    private String commodityName2;
    private String active;
    private Long seqno;
    private String createdBy;
    private Timestamp createdDate;
    private String lastmodifiedBy;
    private Timestamp lastmodifiedDate;
    private String deleted;
}