package com.asg.shipping.vesseltypemaster.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Vessel Type
 * Note: vesselTypeCode is not included as it's not updateable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VesselTypeUpdateDTO {

    @NotBlank(message = "Vessel type name is required")
    @Size(max = 100, message = "Vessel type name must not exceed 100 characters")
    private String vesselTypeName;

    @Size(max = 100, message = "Vessel type name 2 must not exceed 100 characters")
    private String vesselTypeName2;

    @Pattern(regexp = "^[YN]$", message = "Active must be Y or N")
    private String active;


    private Integer seqno;

    private Long costCentrePoid;
}
