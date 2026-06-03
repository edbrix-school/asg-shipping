package com.asg.shipping.containertypes.dto;

import com.asg.shipping.containertypes.validator.DecimalPrecision;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new Container Type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerTypeCreateDTO {

    @NotBlank(message = "Container type code is required")
    @Size(max = 20, message = "Container type code must not exceed 20 characters")
    private String containerTypeCode;

    @NotBlank(message = "Container type name is required")
    @Size(max = 100, message = "Container type name must not exceed 100 characters")
    private String containerTypeName;

    @NotBlank(message = "Container type size is required")
    @Size(max = 20, message = "Container type size must not exceed 20 characters")
    private String containerTypeSize;

    @NotBlank(message = "Container type ISO name is required")
    @Size(max = 100, message = "Container type ISO name must not exceed 100 characters")
    private String containerTypeIsoName;

    @DecimalPrecision(scale = 3, message = "Container cargo weight must not exceed 3 decimal places")
    private BigDecimal containerCargoWeight;

    @DecimalPrecision(scale = 3, message = "Container tare weight must not exceed 3 decimal places")
    private BigDecimal containerTareWeight;

    @NotNull(message = "TEU factor is required")
    @DecimalPrecision(scale = 3, message = "TEU factor must not exceed 3 decimal places")
    private BigDecimal containerTeuFactor;

    @NotBlank(message = "Container type category is required")
    @Size(max = 20, message = "Container type category must not exceed 20 characters")
    private String containerTypeCategory;

    private Long containerGrpPoid;

    @Size(max = 10, message = "APMT type code must not exceed 10 characters")
    private String containerApmtTypeCode;

    @Pattern(regexp = "^[YN]$", message = "Active must be Y or N")
    private String active;

    private Integer seqno;
}


