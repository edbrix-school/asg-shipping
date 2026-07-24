package com.asg.shipping.exportManifestUpdate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for Export Manifest BL Header.
 *
 * String @Size limits mirror the column widths on SHIP_BL_MANIFEST_HDR so oversized
 * values are rejected with a clean 400 before reaching the database (ORA-12899).
 */
@Data
public class ExportManifestBlRequest {

    @NotNull(message = "Voyage Transaction POID is required")
    private Long voyageTransactionPoid;

    @Size(max = 10, message = "BL Type must not exceed 10 characters")
    private String blType; // Should be "EXPORT"

    @Size(max = 25, message = "Cargo Type must not exceed 25 characters")
    private String cargoType;

    private Long quotationTransactionPoid;

    @NotNull(message = "Salesman POID is required")
    private Long salesmanPoid;

    @Size(max = 50, message = "BL Number must not exceed 50 characters")
    private String blNumber;

    private Long noOfOrgnlBls;

    @Size(max = 25, message = "Freight Status must not exceed 25 characters")
    private String freightStatus;

    @Size(max = 25, message = "Type of Move must not exceed 25 characters")
    private String typeOfMove;

    private Long demFreeDays;

    private BigDecimal demRate;

    @Size(max = 1, message = "Booked By PP must be a single character (Y/N)")
    private String bookedByPp;

    @Size(max = 200, message = "Agent Reference must not exceed 200 characters")
    private String agentReference;

    @Size(max = 150, message = "Export Reference must not exceed 150 characters")
    private String exportReference;

    @Size(max = 250, message = "Remarks must not exceed 250 characters")
    private String remarks;

    private Long comodityPoid;

    private BigDecimal totalNetVolume;

    private BigDecimal totalWeight;

    private BigDecimal totalNetWeight;

    @Size(max = 25, message = "Weight Unit must not exceed 25 characters")
    private String weightUnit;

    @Size(max = 6, message = "Unit Pack must not exceed 6 characters")
    private String unitPack;

    private BigDecimal totalNoOfPacks;

    @Size(max = 25, message = "BL Issue Type must not exceed 25 characters")
    private String blIssueType;

    private Long documentCompanyPoid;

    private Long documentCompanyDivisionPoid;

    private Long portOfLoadingPoid;

    private Long placeOfRecieptPoid;

    private Long portOfDischargePoid;

    private Long placeOfDelieveryPoid;

    @Size(max = 50, message = "BL Place of Receipt must not exceed 50 characters")
    private String blPlaceReceipt;

    @Size(max = 50, message = "BL Place of Load must not exceed 50 characters")
    private String blPlaceLoad;

    @Size(max = 50, message = "BL Final Destination must not exceed 50 characters")
    private String blFinalDestination;

    @Size(max = 50, message = "BL Place of Discharge Description must not exceed 50 characters")
    private String blPlaceDischareDesc;

    private BigDecimal shipperPoid;

    private Long shipperAddressPoid;

    private Long consigneePoid;

    private Long consigneeAddressPoid;

    private Long notifyPoid1;

    private Long notifyAddressPoid1;

    private Long notifyPoid2;

    private Long notifyAddressPoid2;

    private Long bookingPartyPoid;

    @Size(max = 350, message = "Shipper EDI Name must not exceed 350 characters")
    private String shipperEdiName;

    @Size(max = 500, message = "Shipper EDI Address must not exceed 500 characters")
    private String shipperEdiAddress;

    @Size(max = 300, message = "Consignee EDI Name must not exceed 300 characters")
    private String consigneeEdiName;

    @Size(max = 500, message = "Consignee EDI Address must not exceed 500 characters")
    private String consigneeEdiAddress;

    @Size(max = 300, message = "Notify 1 EDI Name must not exceed 300 characters")
    private String notify1EdiName;

    @Size(max = 500, message = "Notify 1 EDI Address must not exceed 500 characters")
    private String notify1EdiAddress;

    @Size(max = 200, message = "Notify 2 EDI Name must not exceed 200 characters")
    private String notify2EdiName;

    @Size(max = 500, message = "Notify 2 EDI Address must not exceed 500 characters")
    private String notify2EdiAddress;

    @Size(max = 1, message = "All In One Freight must be a single character (Y/N)")
    private String allInOneFreight;

    @Size(max = 25, message = "LPO/SRN No must not exceed 25 characters")
    private String lpoSrnNo;

    private LocalDate lpoSrnDate;

    @Size(max = 25, message = "Pre Carried By must not exceed 25 characters")
    private String preCarriedBy;

    private Long placeOfIssuePoid;

    private LocalDate dateOfIssue;

    @Size(max = 1, message = "Print Freight Details must be a single character (Y/N)")
    private String printFreightDetails;

    private BigDecimal totalVolume;

    @Size(max = 1, message = "Original BL must be a single character (Y/N)")
    private String originalBl;

    // Additional fields sent by FE at root level
    @Size(max = 1, message = "BL Original Print must be a single character (Y/N)")
    private String blOrginalPrint;   // "Y"/"N" — preferred over originalBl

    @Size(max = 25, message = "Hold Can Do must not exceed 25 characters")
    private String holdCanDo;        // "Y"/"N"

    @Size(max = 100, message = "Hold Reason must not exceed 100 characters")
    private String holdReason;       // e.g. "5"

    @Size(max = 25, message = "BL Status must not exceed 25 characters")
    private String blStatus;         // "OPEN" / "CLOSED" etc.

    @Size(max = 25, message = "Released Status must not exceed 25 characters")
    private String releasedStatus;   // "NONE" etc.

    private String shipperAddressType;
}
