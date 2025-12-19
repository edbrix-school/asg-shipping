package com.asg.shipping.agentMaster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipAgentMasterRequestDto {

    @NotBlank(message = "Agent Name is mandatory")
    private String agentName;

    private String agentName2;

    private String contactPerson;

    @NotBlank(message = "Address is mandatory")
    private String details;

    //private List<Long> linePoid;

    private Long linePoid;

    //private List<Long> portPoid;

    private Long portPoid;

    private String email;

    private String contactNo;

    private String faxNo;

    private String remarks;

    private Integer seqNo;

    private String active;
}
