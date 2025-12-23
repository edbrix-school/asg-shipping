package com.asg.shipping.linemasterthirdparty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineMasterThirdPartyListResponse {
    private Long linePoid;
    private String lineCode;
    private String lineName;
    private String lineName2;
    private String lineAddress;
}
