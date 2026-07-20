package com.asg.shipping.exportManifestBl.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoadBookingRequest {

    private Long voyageTransactionPoid;
    private Long linePoid;
    private List<Long> selectedBookingIds;
    private List<BookingSelectionItemDto> selections;
}
