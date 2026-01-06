package com.asg.shipping.customerautochargeexportbl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request DTO for creating Customer Auto Charge Export BL")
public class CustomerAutoChargeExportBLCreateDTO {

    @NotNull(message = "Customer POID is required")
    @Schema(description = "Customer POID", example = "1001", required = true)
    private Long customerPoid;

    @NotNull(message = "Description is required")
    @NotBlank(message = "Description cannot be blank")
    @Schema(description = "Description", example = "Auto Charge Export BL", required = true)
    private String description;

    @NotNull(message = "Period from date is required")
    @Schema(description = "Period From Date", example = "2024-01-01", required = true)
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    @Schema(description = "Period To Date", example = "2024-12-31", required = true)
    private LocalDate periodTo;

    @Schema(description = "Transaction Date", example = "2024-01-01")
    private LocalDate transactionDate;

    @Schema(description = "Document Reference", example = "DOC-001")
    private String docRef;

    @Valid
    @Schema(description = "List of charge details")
    private List<CustomerAutoChargeDetailDto> chargeDetails;
}