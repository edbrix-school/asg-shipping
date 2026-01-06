package com.asg.shipping.regionmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegionMasterRequest {

    private Long regionPoid; // for update

    @NotBlank(message = "Region code is mandatory")
    @Size(max = 20, message = "Region code must not exceed 20 characters")
    private String regionCode;

    @NotBlank(message = "Region name is mandatory")
    @Size(max = 100, message = "Region name must not exceed 100 characters")
    private String regionName;

    @Size(max = 1, message = "Active flag must be Y or N")
    private String active;

    private Long seqno;
}

