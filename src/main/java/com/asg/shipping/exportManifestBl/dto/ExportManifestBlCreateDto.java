package com.asg.shipping.exportManifestBl.dto;

import com.asg.shipping.importmanifestupdate.dto.CargoDescriptionRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ChargeRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ContainerRequestDto;
import com.asg.shipping.importmanifestupdate.dto.GeneralCargoRequestDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExportManifestBlCreateDto {

    private Long transactionPoid;
    private String docRef;

    private LocalDate transactionDate;

    @NotNull(message = "Voyage transaction POID is required")
    private Long voyageTransactionPoid;

    @NotBlank(message = "BL number is required")
    @Size(max = 50, message = "BL number must not exceed 50 characters")
    private String blNumber;

    @Size(max = 200, message = "Agent reference must not exceed 200 characters")
    private String agentReference;

    private Long shipperPoid;
    private Long shipperAddressPoid;
    private Long consigneePoid;
    private Long consigneeAddressPoid;
    private Long notifyPoid1;
    private Long notifyAddressPoid1;
    private Long notifyPoid2;
    private Long notifyAddressPoid2;
    private Long notifyPoid3;
    private Long notifyAddressPoid3;
    private Long quotationTransactionPoid;
    private Long salesmanPoid;
    private Long comodityPoid;
    private Long noOfOrgnlBls;

    @Size(max = 150, message = "Export reference must not exceed 150 characters")
    private String exportReference;

    @Size(max = 25, message = "LPO/SRN number must not exceed 25 characters")
    private String lpoSrnNo;

    private LocalDate lpoSrnDate;

    @Size(max = 25, message = "Type of move must not exceed 25 characters")
    private String typeOfMove;

    @Size(max = 25, message = "Pre-carried by must not exceed 25 characters")
    private String preCarriedBy;

    private Long placeOfIssuePoid;
    private LocalDate dateOfIssue;

    @Pattern(regexp = "^[YN]?$", message = "Print freight details must be Y or N")
    private String printFreightDetails;

    private BigDecimal totalVolume;
    private BigDecimal totalNetVolume;
    private BigDecimal totalWeight;
    private BigDecimal totalNetWeight;

    @Size(max = 25, message = "Weight unit must not exceed 25 characters")
    private String weightUnit;

    @Size(max = 6, message = "Unit pack must not exceed 6 characters")
    private String unitPack;

    private BigDecimal totalNoOfPacks;
    private Long placeOfRecieptPoid;
    private Long placeOfDelieveryPoid;
    private Long portOfLoadingPoid;
    private Long portOfDischargePoid;

    @Size(max = 250, message = "Remarks must not exceed 250 characters")
    private String remarks;

    @Size(max = 25, message = "BL status must not exceed 25 characters")
    private String blStatus;

    @Size(max = 25, message = "Cargo type must not exceed 25 characters")
    private String cargoType;

    @Size(max = 25, message = "BL original print value must not exceed 25 characters")
    private String blOrginalPrint;

    private LocalDate blOrginalDate;

    @Size(max = 20, message = "BL printed by must not exceed 20 characters")
    private String blPrintedBy;

    private Long demFreeDays;

    @Size(max = 25, message = "Released status must not exceed 25 characters")
    private String releasedStatus;

    private LocalDate releasedDate;

    @Size(max = 50, message = "Released to person must not exceed 50 characters")
    private String relasedToPerson;

    @Size(max = 50, message = "Released ID person must not exceed 50 characters")
    private String relasedIdPerson;

    @Size(max = 100, message = "Released address person must not exceed 100 characters")
    private String relasedAddrsPerson;

    @Size(max = 20, message = "Relased by must not exceed 20 characters")
    private String relasedBy;

    private Long openDaysAfter;

    @Size(max = 25, message = "Released type must not exceed 25 characters")
    private String releasedType;

    private Long relasedSeqno;

    @Size(max = 20, message = "Released grant by must not exceed 20 characters")
    private String releasedGrantBy;

    private LocalDate releasedGrantDate;

    @Size(max = 200, message = "Released grant reason must not exceed 200 characters")
    private String releasedGrantReason;

    @Size(max = 20, message = "DO number must not exceed 20 characters")
    private String doNo;

    @Size(max = 25, message = "BL issue type must not exceed 25 characters")
    private String blIssueType;

    @Size(max = 350, message = "Shipper EDI name must not exceed 350 characters")
    private String shipperEdiName;

    @Size(max = 500, message = "Shipper EDI address must not exceed 500 characters")
    private String shipperEdiAddress;

    @Size(max = 300, message = "Consignee EDI name must not exceed 300 characters")
    private String consigneeEdiName;

    @Size(max = 500, message = "Consignee EDI address must not exceed 500 characters")
    private String consigneeEdiAddress;

    @Size(max = 300, message = "Notify1 EDI name must not exceed 300 characters")
    private String notify1EdiName;

    @Size(max = 500, message = "Notify1 EDI address must not exceed 500 characters")
    private String notify1EdiAddress;

    @Size(max = 200, message = "Notify2 EDI name must not exceed 200 characters")
    private String notify2EdiName;

    @Size(max = 500, message = "Notify2 EDI address must not exceed 500 characters")
    private String notify2EdiAddress;

    @Size(max = 200, message = "Notify3 EDI name must not exceed 200 characters")
    private String notify3EdiName;

    @Size(max = 500, message = "Notify3 EDI address must not exceed 500 characters")
    private String notify3EdiAddress;

    private Long canNotifyCustomerPoid;

    @Size(max = 25, message = "Booked by must not exceed 25 characters")
    private String bookedBy;

    @Size(max = 25, message = "Freight status must not exceed 25 characters")
    private String freightStatus;

    @Size(max = 25, message = "Hold CAN/DO must not exceed 25 characters")
    private String holdCanDo;

    @Size(max = 100, message = "Hold reason must not exceed 100 characters")
    private String holdReason;

    private Long documentCompanyPoid;
    private Long documentCompanyDivisionPoid;

    @Size(max = 50, message = "BL place receipt must not exceed 50 characters")
    private String blPlaceReceipt;

    @Size(max = 50, message = "BL place load must not exceed 50 characters")
    private String blPlaceLoad;

    @Size(max = 50, message = "BL final destination must not exceed 50 characters")
    private String blFinalDestination;

    private Long bookingPartyPoid;

    @Size(max = 50, message = "BL place discharge description must not exceed 50 characters")
    private String blPlaceDischareDesc;

    @Size(max = 100, message = "Cargo arrival number must not exceed 100 characters")
    private String cargoArrivalNumber;

    @Pattern(regexp = "^[YN]?$", message = "Booked by PP must be Y or N")
    private String bookedByPp;

    @Pattern(regexp = "^[YN]?$", message = "Manually CAN send must be Y or N")
    private String manuallyCanSend;

    @Pattern(regexp = "^[YN]?$", message = "All in one freight must be Y or N")
    private String allInOneFreight;

    @Size(max = 100, message = "Hold remarks must not exceed 100 characters")
    private String holdRemarks;

    @Size(max = 1500, message = "BL consignee address additional must not exceed 1500 characters")
    private String blConsigneeAddressAdd;

    @Size(max = 1000, message = "Avoid cargo alert must not exceed 1000 characters")
    private String avoidCargoAlert;

    private Long agentPoid;

    @Size(max = 100, message = "FF job number hold must not exceed 100 characters")
    private String ffJobNoHold;

    @Pattern(regexp = "^[YN]?$", message = "Issue manual invoice must be Y or N")
    private String issueManualInvoice;

    @Size(max = 10, message = "DO priority must not exceed 10 characters")
    private String doPriority;

    @Pattern(regexp = "^[YN]?$", message = "DO issue auth must be Y or N")
    private String doIssueAuth;

    private Long doIssueAuthPoid;

    @Pattern(regexp = "^[YN]?$", message = "DO contact to consignee must be Y or N")
    private String doCntToConsignee;

    @Pattern(regexp = "^[YN]?$", message = "DO contact to notify must be Y or N")
    private String doCntToNotify;

    @Pattern(regexp = "^[YN]?$", message = "DO contact to others must be Y or N")
    private String doCntToOthers;

    @Size(max = 500, message = "DO contact to others emails must not exceed 500 characters")
    private String doCntToOthersMails;

    @Size(max = 500, message = "DO contact reason failure must not exceed 500 characters")
    private String doCntReasonFailure;

    @Size(max = 500, message = "DO contact to registered emails must not exceed 500 characters")
    private String doCntToRegsMails;

    @Pattern(regexp = "^[C]?$", message = "Delivery sent to must be C")
    private String deliverySentTo;

    private Long ffBillToPoid;

    @Pattern(regexp = "^[YN]?$", message = "Demurrage actual next day must be Y or N")
    private String demActualNextDay;

    @Size(max = 50, message = "Principal DO number must not exceed 50 characters")
    private String principalDoNumber;

    @Pattern(regexp = "^[YN]?$", message = "Stop UCAN alert must be Y or N")
    private String stopUcanAlert;

    @Pattern(regexp = "^[YN]?$", message = "Is MBL must be Y or N")
    private String isMbl;

    @Size(max = 100, message = "Forwarder PIN must not exceed 100 characters")
    private String forwarderPin;

    @Size(max = 1, message = "Manifest email verified must be 1 char")
    private String manifestEmailVerified;

    @Size(max = 1, message = "Email verified with special C must be 1 char")
    private String emailVerifiedWithSpecialC;

    /* ================= GENERAL CARGO DETAILS ================= */
    private List<GeneralCargoRequestDto> generalCargoDetails;

    /* ================= CONTAINERS ================= */
    private List<ContainerRequestDto> containers;

    /* ================= DESCRIPTION & MARKS ================= */
    private List<CargoDescriptionRequestDto> cargoDescriptions;

    /* ================= Charge BL ================= */
    private List<ChargeRequestDto> chargeDetails;
}

