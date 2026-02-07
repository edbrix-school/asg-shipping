package com.asg.shipping.shippingofoqv2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OFOQAmendBlRequestDto {

    @NotBlank(message = "Voyage Number is required")
    private String voyageNo;
    @NotNull(message = "Vessel POID is required")
    private Long vesselPoid;
    @NotNull(message = "Arrival Date is required")
    private LocalDate arrivalDate;
    @NotNull(message = "Rotation Number is required")
    private Long rotationNumber;
    private String functionalReference;
    private Long transactionPoid;
    private String docReference;
    private String blNumber;
}
