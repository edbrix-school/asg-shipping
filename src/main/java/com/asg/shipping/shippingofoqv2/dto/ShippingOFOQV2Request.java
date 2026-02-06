package com.asg.shipping.shippingofoqv2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShippingOFOQV2Request {

    private String docRef;
    @NotBlank(message = "Voyage Number is required")
    private String voyageNo;
    @NotNull(message = "Vessel POID is required")
    private Long vesselPoid;
    @NotNull(message = "Arrival Date is required")
    private LocalDate arrivalDate;
    @NotNull(message = "Rotation Number is required")
    private Long rotationNumber;
    private String remarks;


    private List<OFOQItemDtlDto> lineDetails;


}
