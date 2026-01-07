package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for customer address
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressResponseDTO {

    private String contactPerson;
    private String email1;
    private String mobile;
}

