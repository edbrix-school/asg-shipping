package com.asg.shipping.lineprofile.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileLineDetailsResponse {
    private Long linePoid;
    private String lineCode;
    private String lineName;
    private Long countryPoid;
    private LovGetListDto countryDet;
    private Long agencyPoid;
    private LovGetListDto agencyTypeDet;

}

