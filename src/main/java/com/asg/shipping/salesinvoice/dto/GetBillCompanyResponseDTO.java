package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for getting bill company from customer master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetBillCompanyResponseDTO {

    private Long billCompanyPoid;
    private String billCompanyName;
    private String billCompanyCode;
}

