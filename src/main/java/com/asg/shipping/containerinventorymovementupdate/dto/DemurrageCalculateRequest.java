package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemurrageCalculateRequest {
    @NotNull(message = "transactionPoid is required")
    private Long transactionPoid;

    @NotBlank(message = "containerNo is required")
    @Size(max = 50, message = "containerNo must be <= 50 chars")
    private String containerNo;

    /**
     * Date or datetime string. SRS rule: cannot be previous date.
     * DB function expects first 10 chars to be date.
     */
    @NotBlank(message = "demDt is required")
    private String demDt;
}


