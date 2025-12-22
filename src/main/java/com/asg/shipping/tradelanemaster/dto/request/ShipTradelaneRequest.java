package com.asg.shipping.tradelanemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipTradelaneRequest {

    @NotBlank(message = "Trade Lane Code is required")
    private String tradeLaneCode;

    @NotBlank(message = "Trade Lane Name is required")
    private String tradeLaneName;

    private String tradeLaneName2;

    private Long regionPoid;

    @NotNull(message = "Active status is required")
    private Boolean active;

    @NotNull(message = "Sequence number is required")
    private Integer seqNo;
}
