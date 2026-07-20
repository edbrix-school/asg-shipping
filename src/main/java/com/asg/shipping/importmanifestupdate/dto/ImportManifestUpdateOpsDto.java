package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportManifestUpdateOpsDto {

    private Long transactionPoid;
    private String docRef;
    private LocalDate transactionDate;
    private String ffJobNoHold;
    private Long voyageTransactionPoid;
    private LovItem voyageTransactionPoidDet;
    private String blNumber;
    private String blType;
    private LovItem blTypeDet;
    private String blIssueType;
    private LovItem blIssueTypeDet;
    private String cargoType;
    private LovItem cargoTypeDet;
    private Long documentCompanyPoid;
    private Long documentCompanyDivisionPoid;
    private Long noOfOrgnlBls;
    private String typeOfMove;
    private Long demFreeDays;
    private BigDecimal demRate;
    private String bookedByPp;
    private String allInOneFreight;
    private String agentReference;
    private String exportReference;
    private String remarks;
    private Long quotationTransactionPoid;
    private LovItem quotationTransactionDet;
    private Long salesmanPoid;
    private LovItem salesmanDet;

    // Shipper / Consignee
    private String shipperEdiName;
    private String shipperEdiAddress;
    private String consigneeEdiName;
    private String consigneeEdiAddress;
    private Long consigneePoid;
    private LovItem consigneeDet;
    private Long bookingPartyPoid;
    private LovItem bookingPartyDet;

    // Port details / Pre-carried
    private String preCarriedBy;
    private Long placeOfReceiptPoid;
    private LovItem placeOfReceiptDet;
    private Long placeOfDeliveryPoid;
    private LovItem placeOfDeliveryDet;
    private Long portOfLoadingPoid;
    private LovItem portOfLoadingDet;
    private Long portOfDischargePoid;
    private LovItem portOfDischargeDet;

    // Gross/Net Weight/CBM/Packs/Commodity
    private Long comodityPoid;
    private LovItem comodityDet;
    private BigDecimal totalNetVolume;
    private BigDecimal totalWeight;
    private BigDecimal totalNetWeight;
    private String weightUnit;
    private String unitPack;
    private BigDecimal totalNoOfPacks;

    // Notify parties
    private String notify1EdiName;
    private String notify1EdiAddress;
    private String notify2EdiName;
    private String notify2EdiAddress;
    private String notify3EdiName;
    private String notify3EdiAddress;
    private Long notifyPoid1;
    private LovItem notifyPoid1Det;
    private Long notifyPoid2;
    private Long notifyPoid3;

    // Hold/CAN
    private String holdReason;
    private LovItem holdReasonDet;
    private String holdCanDo;
    private String holdRemarks;
    private String manuallyCanSend;
    private String manifestEmailVerified;
    private String emailVerifiedWithSpecialC;
    private String doNo;
    private String freightStatus;

    // Detail lists using request DTOs
    private List<GeneralCargoRequestDto> generalCargoDetails;
    private String simpleCargoDescription;
    private String simpleCargoMarks;
    private List<CargoDescriptionRequestDto> cargoDescriptions;
    private List<ContainerRequestDto> containers;
    private List<ChargeRequestDto> chargeDetails;
    private List<PartBlRequestDto> partBls;
    private List<NotifyPartyRequestDto> addressDetails;
    private List<MafiRequestDto> mafiDetails;
}
