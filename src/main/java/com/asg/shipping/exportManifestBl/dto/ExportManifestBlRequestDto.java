package com.asg.shipping.exportManifestBl.dto;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.dto.ChargeRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ContainerRequestDto;
import com.asg.shipping.importmanifestupdate.dto.GeneralCargoRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportManifestBlRequestDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private LocalDate transactionDate;
    private Long voyageTransactionPoid;
    private String blNumber;
    private String agentReference;
    private BigDecimal shipperPoid;
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
    private LovItem comodityDet; 
    private Long noOfOrgnlBls;
    private String exportReference;
    private String lpoSrnNo;
    private LocalDate lpoSrnDate;
    private String typeOfMove;
    private String preCarriedBy;
    private Long placeOfIssuePoid;
    private LocalDate dateOfIssue;
    private String printFreightDetails;
    private BigDecimal totalVolume;
    private BigDecimal totalNetVolume;
    private BigDecimal totalWeight;
    private BigDecimal totalNetWeight;
    private String weightUnit;
    private String unitPack;
    private BigDecimal totalNoOfPacks;
    private Long placeOfRecieptPoid;
    private Long placeOfDelieveryPoid;
    private Long portOfLoadingPoid;
    private Long portOfDischargePoid;
    private String remarks;
    private String blStatus;
    private String blOrginalPrint;
    private LocalDate blOrginalDate;
    private String blPrintedBy;
    private String uniqueBlno;
    private BigDecimal demRate;
    private Long demFreeDays;
    private String releasedStatus;
    private LocalDate releasedDate;
    private String relasedToPerson;
    private String relasedIdPerson;
    private String relasedAddrsPerson;
    private String relasedBy;
    private Long openDaysAfter;
    private String releasedType;
    private Long relasedSeqno;
    private String releasedGrantBy;
    private LocalDate releasedGrantDate;
    private String releasedGrantReason;
    private String cargoType;
    private String blType;
    private String doNo;
    private String deleted;
    private String blIssueType;
    private String shipperEdiName;
    private String shipperEdiAddress;
    private String consigneeEdiName;
    private String consigneeEdiAddress;
    private String notify1EdiName;
    private String notify1EdiAddress;
    private String notify2EdiName;
    private String notify2EdiAddress;
    private String notify3EdiName;
    private String notify3EdiAddress;
    private Long canNotifyCustomerPoid;
    private String bookedBy;
    private String freightStatus;
    private String holdCanDo;
    private String holdReason;
    private String canSentQueue;
    private LocalDateTime canSentDate;
    private String canSentBy;
    private Long documentCompanyPoid;
    private Long documentCompanyDivisionPoid;
    private String blPlaceReceipt;
    private String blPlaceLoad;
    private String blFinalDestination;
    private Long bookingPartyPoid;
    private String blPlaceDischareDesc;
    private String cargoArrivalNumber;
    private String bookedByPp;
    private String manuallyCanSend;
    private String allInOneFreight;
    private String holdRemarks;
    private String blConsigneeAddressAdd;
    private String avoidCargoAlert;
    private Long agentPoid;
    private String ffJobNoHold;
    private String issueManualInvoice;
    private String doPriority;
    private String doIssueAuth;
    private Long doIssueAuthPoid;
    private String doCntToConsignee;
    private String doCntToNotify;
    private String doCntToOthers;
    private String doCntToOthersMails;
    private String doCntReasonFailure;
    private String doCntToRegsMails;
    private String deliverySentTo;
    private Long ffBillToPoid;
    private String demActualNextDay;
    private String principalDoNumber;
    private String stopUcanAlert;
    private String isMbl;
    private String forwarderPin;
    private String manifestEmailVerified;
    private String emailVerifiedWithSpecialC;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    /* ================= GENERAL CARGO DETAILS ================= */
    private List<GeneralCargoRequestDto> generalCargoDetails;

    /* ================= CONTAINERS ================= */
    private List<ContainerRequestDto> containers;

    /* ================= DESCRIPTION & MARKS ================= */
    private String simpleCargoDescription;
    private String simpleCargoMarks;

    /* ================= Charge BL ================= */
    private List<ChargeRequestDto> chargeDetails;
}

