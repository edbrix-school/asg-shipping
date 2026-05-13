package com.asg.shipping.shippingmanifestcorrector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Container Reprint Auto-fill
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerReprintResponse {

    private String blNumber;
    private List<ManifestCorrectorContainerDtlDto> containers;
    private List<ManifestCorrectorChargeDtlDto> charges;
}