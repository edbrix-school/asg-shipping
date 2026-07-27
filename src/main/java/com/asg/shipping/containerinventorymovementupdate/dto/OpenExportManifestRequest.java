package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenExportManifestRequest {

    @NotBlank(message = "exportBlNumber is required")
    private String exportBlNumber;

    @NotNull(message = "companyPoid is required")
    private Long companyPoid;
}
