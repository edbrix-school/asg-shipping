package com.asg.shipping.customerautochargeexportbl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for updating Customer Auto Charge Export BL")
public class CustomerAutoChargeExportBLUpdateDTO {

    @Schema(description = "Customer POID", example = "1001")
    private Long customerPoid;

    @Schema(description = "Description", example = "Auto Charge Export BL")
    private String description;

    @Schema(description = "Period From Date", example = "2024-01-01")
    private LocalDate periodFrom;

    @Schema(description = "Period To Date", example = "2024-12-31")
    private LocalDate periodTo;

    @Schema(description = "Transaction Date", example = "2024-01-01")
    private LocalDate transactionDate;

    @Schema(description = "Document Reference", example = "DOC-001")
    private String docRef;

    @Valid
    @Schema(description = "List of charge details")
    private List<CustomerAutoChargeDetailDto> chargeDetails;
}