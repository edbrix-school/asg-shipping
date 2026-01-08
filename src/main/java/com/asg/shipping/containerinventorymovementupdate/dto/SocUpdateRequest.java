package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocUpdateRequest {
    @NotBlank(message = "blNumber is required")
    @Size(max = 50, message = "blNumber must be <= 50 chars")
    private String blNumber;
}


