package com.asg.shipping.importmanifestbl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class ImportManifestBlDto {

    private Long transactionPoid;
    private String docId;

    private Long vesselVoyagePoid;
    private String blNumber;
    private String blType;
    private String cargo;
    private Long originalBlsCount;
    private String freight;
    private Long quotationPoid;
    private Long salesmanPoid;
    private String issueType;

    private String bookedByPrincipal;
    private String freightAllInOne;
    private String remarks;

    private String shipperName;
    private String shipperAddress;

    private String consigneeName;
    private Long consigneePoid;

    private String manifestEmailVerified;
    private String noRecentShipmentVerified;

    private Long bookingCustomerPoid;
    private String ediAddress;

    private String preCarried;
    private Long loadPortPoid;
    private Long receiptPortPoid;
    private Long deliveryPortPoid;
    private Long dischargePortPoid;

    private Long commodityPoid;

    private BigDecimal grossWeight;
    private BigDecimal cbm;
    private String weight;
    private String  packTypes;
    private BigDecimal numberOfPacks;

    private String notifyName;
    private Long notify1Poid;

    private String notifyEdiAddress;
    private List<AddressDetailsDto> addressDetails;

    private String  holdCanAuto;
    private String manualCanSend;

    private String holdTypePoid;
    private String holdRemarks;

    private  DescriptionAndMarksDto descriptionsAndMarks;
    private List<MafiDetailsDto> mafiDetails;
    private List<OtherNotifyDto> otherNotifies;
    private List<GeneralCargoDto> generalCargoDetails;
    private List<ContainerDto> containers;
    private List<ChargeDto> charges;
    private List<ChargeOtherDto> otherCharges;
    private List<PartBlDto> partBls;
}
