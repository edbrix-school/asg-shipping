package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class ExportManifestBlResponse {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private LocalDate transactionDate;
    private Long voyageTransactionPoid;
    private LovItem voyageTransactionDet; // LOV: VESSAL_VOYAGE
    private String blNumber;
    private String agentReference;
    private Long shipperPoid;
    private LovItem shipperDet; // LOV: CUSTOMER_MASTER
    private Long shipperAddressPoid;
    private LovItem shipperAddressDet; // LOV: CUSTOMER_MASTER
    private Long consigneePoid;
    private LovItem consigneeDet; // LOV: CUSTOMER_MASTER
    private Long consigneeAddressPoid;
    private LovItem consigneeAddressDet; // LOV: CUSTOMER_MASTER
    private Long notifyPoid1;
    private LovItem notify1Det; // LOV: CUSTOMER_MASTER
    private Long notifyAddressPoid1;
    private LovItem notify1AddressDet; // LOV: CUSTOMER_MASTER
    private Long notifyPoid2;
    private LovItem notify2Det; // LOV: CUSTOMER_MASTER
    private Long notifyAddressPoid2;
    private LovItem notify2AddressDet; // LOV: CUSTOMER_MASTER
    private Long quotationTransactionPoid;
    private LovItem quotationTransactionDet; // LOV: SHIP_QUOTATION_EXPORT
    private Long salesmanPoid;
    private LovItem salesmanDet; // LOV: SALESMAN
    private Long comodityPoid;
    private LovItem comodityDet; // LOV: COMODITY
    private Long noOfOrgnlBls;
    private String exportReference;
    private String lpoSrnNo;
    private LocalDate lpoSrnDate;
    private String typeOfMove;
    private String preCarriedBy;
    private Long placeOfIssuePoid;
    private LovItem placeOfIssueDet; // LOV: PORT_MASTER
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
    private LovItem placeOfRecieptDet; // LOV: PORT_MASTER
    private Long placeOfDelieveryPoid;
    private LovItem placeOfDelieveryDet; // LOV: PORT_MASTER
    private Long portOfLoadingPoid;
    private LovItem portOfLoadingDet; // LOV: PORT_MASTER
    private Long portOfDischargePoid;
    private LovItem portOfDischargeDet; // LOV: PORT_MASTER
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
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String cargoType;
    private String blType;
    private String doNo;
    private String docRef;
    private String deleted;
    private String blIssueType;
    private Long notifyPoid3;
    private LovItem notify3Det; // LOV: CUSTOMER_MASTER
    private Long notifyAddressPoid3;
    private LovItem notify3AddressDet; // LOV: CUSTOMER_MASTER
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
    private LovItem canNotifyCustomerDet; // LOV: CUSTOMER_MASTER
    private String bookedBy;
    private String freightStatus;
    private String holdCanDo;
    private String holdReason;
    private String canSentQueue;
    private LocalDateTime canSentDate;
    private String canSentBy;
    private Long documentCompanyPoid;
    private LovItem documentCompanyDet; // LOV: COMPANY
    private Long documentCompanyDivisionPoid;
    private LovItem documentCompanyDivisionDet; // LOV: SHIP_DIVISION_PRINT
    private String blPlaceReceipt;
    private String blPlaceLoad;
    private String blFinalDestination;
    private Long bookingPartyPoid;
    private LovItem bookingPartyDet; // LOV: CUSTOMER_MASTER
    private String blPlaceDischareDesc;
    private String cargoArrivalNumber;
    private String bookedByPp;
    private String manuallyCanSend;
    private String allInOneFreight;
    private String holdRemarks;
    private String blConsigneeAddressAdd;
    private String avoidCargoAlert;
    private Long agentPoid;
    private LovItem agentDet; // LOV: AGENT_MASTER

    private List<GeneralCargoDetailDto> generalCargoDetails;

}

