package com.asg.shipping.salesinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for loading container demurrage data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadContainerDemurrageResponseDTO {

    private List<SalesInvoiceContainerDtlDto> containers;
}

