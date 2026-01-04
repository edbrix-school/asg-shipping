package com.asg.shipping.tradelanemaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipTradelaneResponse {

    private Long tradeLanePoid;
    private String tradeLaneCode;
    private String tradeLaneName;
    private String tradeLaneName2;
    private Long regionPoid;
    private Boolean active;
    private Integer seqNo;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
