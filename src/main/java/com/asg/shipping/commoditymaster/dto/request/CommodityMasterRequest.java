package com.asg.shipping.commoditymaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommodityMasterRequest {

    private Long commodityPoid;

    @NotBlank(message = "Commodity Name is required")
    @Size(max = 100, message = "Commodity Name must not exceed 100 characters")
    private String commodityName;

    @Size(max = 100, message = "Commodity Name 2 must not exceed 100 characters")
    private String commodityName2;

    @Pattern(regexp = "^(Y|N)$", message = "Active must be either Y or N")
    private String active;

    private Long seqno;
}