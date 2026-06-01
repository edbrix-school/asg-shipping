package com.asg.shipping.groupcontainertypes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Container Group
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerGroupUpdateDTO {

    @NotBlank(message = "Container group code is required")
    @Size(max = 20, message = "Container group code must not exceed 20 characters")
    private String containerGrpCode;

    @NotBlank(message = "Container group name is required")
    @Size(max = 100, message = "Container group name must not exceed 100 characters")
    private String containerGrpName;

    @Pattern(regexp = "^[YN]$", message = "Active must be Y or N")
    private String active;


    private Integer seqno;
}

