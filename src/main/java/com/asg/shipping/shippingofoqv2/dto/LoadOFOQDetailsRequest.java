package com.asg.shipping.shippingofoqv2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadOFOQDetailsRequest {
    
    @NotNull(message = "Voyage number is required")
    private String voyageNo;
    
    @NotNull(message = "Vessel POID is required")
    private Long vesselPoid;
    
    @NotNull(message = "Arrival date is required")
    private LocalDateTime arrivalDate;
    
    private String docRef;
    private Long transactionPoid;
}