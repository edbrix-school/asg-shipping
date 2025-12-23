package com.asg.shipping.vesseltypemaster.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Vessel Type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VesselTypeCreateDTO {

    @NotBlank(message = "Vessel type code is required")
    @Size(max = 20, message = "Vessel type code must not exceed 20 characters")
    private String vesselTypeCode;

    @NotBlank(message = "Vessel type name is required")
    @Size(max = 100, message = "Vessel type name must not exceed 100 characters")
    private String vesselTypeName;

    @Size(max = 100, message = "Vessel type name 2 must not exceed 100 characters")
    private String vesselTypeName2;

    @Pattern(regexp = "^[YN]$", message = "Active must be Y or N")
    private String active;

    @Positive(message = "Sequence number must be positive")
    private Integer seqno;
}