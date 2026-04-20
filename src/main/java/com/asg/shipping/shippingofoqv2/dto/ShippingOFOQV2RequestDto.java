package com.asg.shipping.shippingofoqv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingOFOQV2RequestDto {

    @Schema(description = "Document reference number", example = "ASG00001")
    private String docRef;

    @NotBlank(message = "Voyage Number is required")
    @Schema(description = "Voyage number", example = "442", requiredMode = Schema.RequiredMode.REQUIRED)
    private String voyageNo;

    @NotNull(message = "Vessel POID is required")
    @Schema(description = "Vessel master POID", example = "27961", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long vesselPoid;

    @NotNull(message = "Arrival Date is required")
    @Schema(description = "Vessel arrival date (yyyy-MM-dd)", example = "2024-10-17", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate arrivalDate;

    @NotNull(message = "Rotation Number is required")
    @Schema(description = "Rotation number", example = "2400004950", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long rotationNumber;

    @Schema(description = "Remarks or comments", example = "Test OFOQ creation")
    private String remarks;
}
