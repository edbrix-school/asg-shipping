package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for creating FF job
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFFJobResponseDTO {

    private Long ffJobPoid;
    private String status;
    private String statusCode;
}

