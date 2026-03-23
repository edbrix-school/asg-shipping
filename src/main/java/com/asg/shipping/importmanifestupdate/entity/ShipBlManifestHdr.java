package com.asg.shipping.importmanifestupdate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SHIP_BL_MANIFEST_HDR")
public class ShipBlManifestHdr {


    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    /* ================= BASIC INFO ================= */

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDateTime transactionDate;

    @Column(name = "VOYAGE_TRANSACTION_POID", nullable = false)
    private Long voyageTransactionPoid;

    @Column(name = "BL_NUMBER", length = 50)
    private String blNumber;

    @Column(name = "AGENT_REFERENCE", length = 200)
    private String agentReference;

    /* ================= PARTY DETAILS ================= */

    @Column(name = "SHIPPER_POID")
    private Long shipperPoid;

    @Column(name = "SHIPPER_ADDRESS_POID")
    private Long shipperAddressPoid;

    @Column(name = "CONSIGNEE_POID")
    private Long consigneePoid;

    @Column(name = "CONSIGNEE_ADDRESS_POID")
    private Long consigneeAddressPoid;

    @Column(name = "NOTIFY_POID_1")
    private Long notifyPoid1;

    @Column(name = "NOTIFY_ADDRESS_POID_1")
    private Long notifyAddressPoid1;

    @Column(name = "NOTIFY_POID_2")
    private Long notifyPoid2;

    @Column(name = "NOTIFY_ADDRESS_POID_2")
    private Long notifyAddressPoid2;

    @Column(name = "NOTIFY_POID_3")
    private Long notifyPoid3;

    @Column(name = "NOTIFY_ADDRESS_POID_3")
    private Long notifyAddressPoid3;

    /* ================= QUOTATION / SALES ================= */

    @Column(name = "QUOTATION_TRANSACTION_POID")
    private Long quotationTransactionPoid;

    @Column(name = "SALESMAN_POID", nullable = false)
    private Long salesmanPoid;

    @Column(name = "COMODITY_POID")
    private Long comodityPoid;

    @Column(name = "NO_OF_ORGNL_BLS")
    private Long noOfOrgnlBls;

    /* ================= EXPORT / ISSUE DETAILS ================= */

    @Column(name = "EXPORT_REFERENCE", length = 150)
    private String exportReference;

    @Column(name = "LPO_SRN_NO", length = 25)
    private String lpoSrnNo;

    @Column(name = "LPO_SRN_DATE")
    private LocalDateTime lpoSrnDate;

    @Column(name = "TYPE_OF_MOVE", length = 25)
    private String typeOfMove;

    @Column(name = "PRE_CARRIED_BY", length = 25)
    private String preCarriedBy;

    @Column(name = "PLACE_OF_ISSUE_POID")
    private Long placeOfIssuePoid;

    @Column(name = "DATE_OF_ISSUE")
    private LocalDateTime dateOfIssue;

    @Column(name = "PRINT_FREIGHT_DETAILS", length = 1)
    private String printFreightDetails;

    /* ================= VOLUME / WEIGHT ================= */

    @Column(name = "TOTAL_VOLUME")
    private BigDecimal totalVolume;

    @Column(name = "TOTAL_NET_VOLUME")
    private BigDecimal totalNetVolume;

    @Column(name = "TOTAL_WEIGHT")
    private BigDecimal totalWeight;

    @Column(name = "TOTAL_NET_WEIGHT")
    private BigDecimal totalNetWeight;

    @Column(name = "WEIGHT_UNIT", length = 25)
    private String weightUnit;

    @Column(name = "UNIT_PACK", length = 6)
    private String unitPack;

    @Column(name = "TOTAL_NO_OF_PACKS")
    private BigDecimal totalNoOfPacks;

    /* ================= PORT DETAILS ================= */

    @Column(name = "PLACE_OF_RECIEPT_POID")
    private Long placeOfReceiptPoid;

    @Column(name = "PLACE_OF_DELIEVERY_POID")
    private Long placeOfDeliveryPoid;

    @Column(name = "PORT_OF_LOADING_POID")
    private Long portOfLoadingPoid;

    @Column(name = "PORT_OF_DISCHARGE_POID")
    private Long portOfDischargePoid;

    /* ================= STATUS / PRINT ================= */

    @Column(name = "REMARKS", length = 250)
    private String remarks;

    @Column(name = "BL_STATUS", length = 25)
    private String blStatus;

    @Column(name = "BL_ORGINAL_PRINT", length = 1)
    private String blOrginalPrint;

    @Column(name = "BL_ORGINAL_DATE")
    private LocalDateTime blOrginalDate;

    @Column(name = "BL_PRINTED_BY", length = 20)
    private String blPrintedBy;

    @Column(name = "UNIQUE_BLNO", length = 50)
    private String uniqueBlno;

    /* ================= DEMURRAGE ================= */

    @Column(name = "DEM_RATE")
    private BigDecimal demRate;

    @Column(name = "DEM_FREE_DAYS")
    private Long demFreeDays;

    /* ================= RELEASE DETAILS ================= */

    @Column(name = "RELEASED_STATUS", length = 25)
    private String releasedStatus;

    @Column(name = "RELEASED_DATE")
    private LocalDateTime releasedDate;

    @Column(name = "RELASED_TO_PERSON", length = 50)
    private String relasedToPerson;

    @Column(name = "RELASED_ID_PERSON", length = 50)
    private String relasedIdPerson;

    @Column(name = "RELASED_ADDRS_PERSON", length = 100)
    private String relasedAddrsPerson;

    @Column(name = "RELASED_BY", length = 20)
    private String relasedBy;

    @Column(name = "OPEN_DAYS_AFTER")
    private Long openDaysAfter;

    @Column(name = "RELEASED_TYPE", length = 25)
    private String releasedType;

    @Column(name = "RELASED_SEQNO")
    private Long relasedSeqno;

    @Column(name = "RELEASED_GRANT_BY", length = 20)
    private String releasedGrantBy;

    @Column(name = "RELEASED_GRANT_DATE")
    private LocalDateTime releasedGrantDate;

    @Column(name = "RELEASED_GRANT_REASON", length = 200)
    private String releasedGrantReason;

    /* ================= AUDIT ================= */

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    /* ================= TYPE & DOC ================= */

    @Column(name = "CARGO_TYPE", length = 25)
    private String cargoType;

    @Column(name = "BL_TYPE", length = 10)
    private String blType;

    @Column(name = "DO_NO", length = 20)
    private String doNo;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "BL_ISSUE_TYPE", length = 25)
    private String blIssueType;

    /* ================= EDI DETAILS ================= */

    @Column(name = "SHIPPER_EDI_NAME", length = 350)
    private String shipperEdiName;

    @Column(name = "SHIPPER_EDI_ADDRESS", length = 500)
    private String shipperEdiAddress;

    @Column(name = "CONSIGNEE_EDI_NAME", length = 300)
    private String consigneeEdiName;

    @Column(name = "CONSIGNEE_EDI_ADDRESS", length = 500)
    private String consigneeEdiAddress;

    @Column(name = "NOTIFY1_EDI_NAME", length = 300)
    private String notify1EdiName;

    @Column(name = "NOTIFY1_EDI_ADDRESS", length = 500)
    private String notify1EdiAddress;

    @Column(name = "NOTIFY2_EDI_NAME", length = 200)
    private String notify2EdiName;

    @Column(name = "NOTIFY2_EDI_ADDRESS", length = 500)
    private String notify2EdiAddress;

    @Column(name = "NOTIFY3_EDI_NAME", length = 200)
    private String notify3EdiName;

    @Column(name = "NOTIFY3_EDI_ADDRESS", length = 500)
    private String notify3EdiAddress;

    /* ================= CAN / HOLD ================= */

    @Column(name = "CAN_NOTIFY_CUSTOMER_POID")
    private Long canNotifyCustomerPoid;

    @Column(name = "BOOKED_BY", length = 25)
    private String bookedBy;

    @Column(name = "FREIGHT_STATUS", length = 25)
    private String freightStatus;

    @Column(name = "HOLD_CAN_DO", length = 25)
    private String holdCanDo;

    @Column(name = "HOLD_REASON", length = 100)
    private String holdReason;

    @Column(name = "CAN_SENT_QUEUE", length = 25)
    private String canSentQueue;

    @Column(name = "CAN_SENT_DATE")
    private LocalDateTime canSentDate;

    @Column(name = "CAN_SENT_BY", length = 20)
    private String canSentBy;

    /* ================= DOCUMENT COMPANY ================= */

    @Column(name = "DOCUMENT_COMPANY_POID")
    private Long documentCompanyPoid;

    @Column(name = "DOCUMENT_COMPANY_DIVISION_POID")
    private Long documentCompanyDivisionPoid;

    /* ================= PLACE TEXT ================= */

    @Column(name = "BL_PLACE_RECEIPT", length = 50)
    private String blPlaceReceipt;

    @Column(name = "BL_PLACE_LOAD", length = 50)
    private String blPlaceLoad;

    @Column(name = "BL_FINAL_DESTINATION", length = 50)
    private String blFinalDestination;

    @Column(name = "BOOKING_PARTY_POID")
    private Long bookingPartyPoid;

    @Column(name = "BL_PLACE_DISCHARE_DESC", length = 50)
    private String blPlaceDischargeDesc;

    @Column(name = "CARGO_ARRIVAL_NUMBER", length = 100)
    private String cargoArrivalNumber;

    /* ================= FLAGS ================= */

    @Column(name = "BOOKED_BY_PP", length = 1)
    private String bookedByPp;

    @Column(name = "MANUALLY_CAN_SEND", length = 1)
    private String manuallyCanSend;

    @Column(name = "ALL_IN_ONE_FREIGHT", length = 1)
    private String allInOneFreight;

    @Column(name = "HOLD_REMARKS", length = 100)
    private String holdRemarks;

    @Column(name = "BL_CONSIGNEE_ADDRESS_ADD", length = 1500)
    private String blConsigneeAddressAdd;

    @Column(name = "AVOID_CARGO_ALERT", length = 1000)
    private String avoidCargoAlert;

    @Column(name = "AGENT_POID")
    private Long agentPoid;

    @Column(name = "FF_JOB_NO_HOLD", length = 100)
    private String ffJobNoHold;

    @Column(name = "ISSUE_MANUAL_INVOICE", length = 1)
    private String issueManualInvoice;

    @Column(name = "DO_PRIORITY", length = 10)
    private String doPriority;

    @Column(name = "DO_ISSUE_AUTH", length = 1)
    private String doIssueAuth;

    @Column(name = "DO_ISSUE_AUTH_POID")
    private Long doIssueAuthPoid;

    @Column(name = "DO_CNT_TO_CONSIGNEE", length = 1)
    private String doCntToConsignee;

    @Column(name = "DO_CNT_TO_NOTIFY", length = 1)
    private String doCntToNotify;

    @Column(name = "DO_CNT_TO_OTHERS", length = 1)
    private String doCntToOthers;

    @Column(name = "DO_CNT_TO_OTHERS_MAILS", length = 500)
    private String doCntToOthersMails;

    @Column(name = "DO_CNT_REASON_FAILURE", length = 500)
    private String doCntReasonFailure;

    @Column(name = "DO_CNT_TO_REGS_MAILS", length = 500)
    private String doCntToRegsMails;

    @Column(name = "DELIVERY_SENT_TO", length = 1)
    private String deliverySentTo;

    @Column(name = "FF_BILL_TO_POID")
    private Long ffBillToPoid;

    @Column(name = "DEM_ACTUAL_NEXT_DAY", length = 1)
    private String demActualNextDay;

    @Column(name = "PRINCIPAL_DO_NUMBER", length = 50)
    private String principalDoNumber;

    @Column(name = "STOP_UCAN_ALERT", length = 1)
    private String stopUcanAlert;

    @Column(name = "IS_MBL", length = 1)
    private String isMbl;

    @Column(name = "FORWARDER_PIN", length = 100)
    private String forwarderPin;

    @Column(name = "MANIFEST_EMAIL_VERIFIED", length = 1)
    private String manifestEmailVerified;

    @Column(name = "EMAIL_VERIFIED_WITH_SPECIAL_C", length = 1)
    private String emailVerifiedWithSpecialC;
}

