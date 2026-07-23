package com.asg.shipping.exportManifestUpdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for Export Manifest BL Header
 */
@Data
public class ExportManifestBlRequest {

    @NotNull(message = "Voyage Transaction POID is required")
    private Long voyageTransactionPoid;

    private String blType; // Should be "EXPORT"

    private String cargoType;

    private Long quotationTransactionPoid;

    @NotNull(message = "Salesman POID is required")
    private Long salesmanPoid;

    private String blNumber;

    private Long noOfOrgnlBls;

    private String freightStatus;

    private String typeOfMove;

    private Long demFreeDays;

    private BigDecimal demRate;

    private String bookedByPp;

    private String agentReference;

    private String exportReference;

    private String remarks;

    private Long comodityPoid;

    private BigDecimal totalNetVolume;

    private BigDecimal totalWeight;

    private BigDecimal totalNetWeight;

    private String weightUnit;

    private String unitPack;

    private BigDecimal totalNoOfPacks;

    private String blIssueType;

    private Long documentCompanyPoid;

    private Long documentCompanyDivisionPoid;

    private Long portOfLoadingPoid;

    private Long placeOfRecieptPoid;

    private Long portOfDischargePoid;

    private Long placeOfDelieveryPoid;

    private String blPlaceReceipt;

    private String blPlaceLoad;

    private String blFinalDestination;

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

    private String shipperEdiName;

    private String shipperEdiAddress;

    private String consigneeEdiName;

    private String consigneeEdiAddress;

    private String notify1EdiName;

    private String notify1EdiAddress;

    private String notify2EdiName;

    private String notify2EdiAddress;

    private String allInOneFreight;

    private String lpoSrnNo;

    private LocalDate lpoSrnDate;

    private String preCarriedBy;

    private Long placeOfIssuePoid;

    private LocalDate dateOfIssue;

    private String printFreightDetails;

    private BigDecimal totalVolume;

    private String originalBl;

    // Additional fields sent by FE at root level
    private String blOrginalPrint;   // "Y"/"N" — preferred over originalBl
    private String holdCanDo;        // "Y"/"N"
    private String holdReason;       // e.g. "5"
    private String blStatus;         // "OPEN" / "CLOSED" etc.
    private String releasedStatus;   // "NONE" etc.

    private String shipperAddressType;
}

