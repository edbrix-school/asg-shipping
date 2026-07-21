package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating booking party
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingPartyRequestDTO {

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    @NotNull(message = "Customer POID is required")
    private Long customerPoid;

    @NotNull(message = "Booking Party POID is required")
    private Long bookingPartyPoid;
}

