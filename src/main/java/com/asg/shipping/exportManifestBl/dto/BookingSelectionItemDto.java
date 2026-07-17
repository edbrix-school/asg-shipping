package com.asg.shipping.exportManifestBl.dto;

import lombok.Data;

@Data
public class BookingSelectionItemDto {

    private Long transactionPoid;
    private String voyageNo;
    private String containerNo;
    private String bookingIssueNo;
    private String isSelected;
}
