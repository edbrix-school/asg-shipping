package com.asg.shipping.agentmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipAgentMasterResponseDto {
    private Long agentPoid;
    private Long groupPoid;

    private String agentName;
    private String agentName2;
    private String contactPerson;
    private String details;

    private Long linePoid;
    private Long portPoid;


    private List<String> email;
    private String contactNo;
    private String faxNo;

    private String remarks;
    private Integer seqNo;
    private String active;
    private Long countryPoid;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
