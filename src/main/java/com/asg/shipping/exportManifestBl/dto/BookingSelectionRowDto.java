package com.asg.shipping.exportManifestBl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingSelectionRowDto {

    private Long mateTransactionPoid;
    private Long containerDetRowId;
    private Long voyageTransactionPoid;
    private String voyageNo;
    private String vesselName;
    private Long linePoid;
    private String lineCode;
    private String bookingIssueNo;
    private String containerNo;
    private String equipmentSize;
    private String equipmentIsoType;
    private String sizeOrOw;
    private Long destinationPortPoid;
    private String destinationPortCode;
    private String destinationPortName;
    private Long salesmanPoid;
    private String salesmanCode;
    private String salesmanName;
    /** Default N for popup; UI sets Y when row is checked before Load Selected. */
    private String isSelected;
}
