package com.asg.shipping.customerautochargeexportbl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Customer Auto Charge Detail DTO")
public class CustomerAutoChargeDetailDto {

    @Schema(description = "Detail Row ID", example = "1")
    private Long detRowId;

    @Schema(description = "Charge Code POID", example = "100")
    private Long chargeCodePoid;

    @Schema(description = "Charge Type", example = "FREIGHT")
    private String type;

    @Schema(description = "Charge Applicable", example = "Y")
    private String chargeApplicable;

    @Schema(description = "IMCO Class Type", example = "CLASS_1")
    private String imcoClassType;

    @Schema(description = "OOG Type", example = "OOG_TYPE_1")
    private String oogType;

    @Schema(description = "Others Type", example = "OTHER")
    private String othersType;

    @Schema(description = "Currency Code", example = "USD")
    private String currencyCode;

    @Schema(description = "Currency Exchange Rate", example = "1.0")
    private BigDecimal currencyExchange;

    @Schema(description = "Amount for 20ft Container", example = "100.00")
    private BigDecimal amount20;

    @Schema(description = "Amount for 40ft Container", example = "150.00")
    private BigDecimal amount40;

    @Schema(description = "Amount for Other Container", example = "120.00")
    private BigDecimal amountOther;

    @Schema(description = "Cost Amount for 20ft Container", example = "80.00")
    private BigDecimal amount20Cost;

    @Schema(description = "Cost Amount for 40ft Container", example = "120.00")
    private BigDecimal amount40Cost;

    @Schema(description = "Cost Amount for Other Container", example = "100.00")
    private BigDecimal amountOtherCost;

    @Schema(description = "Amount for 53ft Container", example = "180.00")
    private BigDecimal amount53;

    @Schema(description = "Cost Amount for 53ft Container", example = "140.00")
    private BigDecimal amount53Cost;

    private String actionType;
}
