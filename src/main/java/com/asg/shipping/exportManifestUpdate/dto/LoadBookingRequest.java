package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for loading booking data from MATE
 */
@Data
public class LoadBookingRequest {
    private Long voyageTransactionPoid;
    private Long linePoid;
    private List<Long> selectedBookingIds;
}

