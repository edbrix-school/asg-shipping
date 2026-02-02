package com.asg.shipping.shippingofoqv2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQItemDtlDto {
    
    private Long detRowId;
    private Long vesselVoyagePoid;
    private String lineName;
    private String vesselName;
    private String voyageNo;
    private String jobNo;
    private LocalDate arrivalDate;
    private LocalDate sailDate;
    private String checked;
    private String drillDownLinkInfo;
    private Long linePoid;


}