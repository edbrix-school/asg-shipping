package com.asg.shipping.importmanifestbl.dto;

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
public class ImportManifestBlDto {

    private Long transactionPoid;
    private String docId;
    private LocalDate transactionDate;

    private Long vesselVoyagePoid;
    private LovItem vesselVoyagePoidDet;
    private String blNumber;
    private String blType;
    private LovItem blTypeDet;
    private String blIssueType;
    private LovItem blIssueTypeDet;
    private String cargo;
    private LovItem cargoDet;
    private Long originalBlsCount;
    private String freight;
    private Long quotationPoid;
    private LovItem quotationDet;
    private Long salesmanPoid;
    private LovItem salesmanDet;
    private String issueType;

    private String bookedByPrincipal;
    private String freightAllInOne;
    private String remarks;

    private String createdBy;
    private java.time.LocalDateTime createdDate;

    private String shipperName;
    private String shipperAddress;

    private String consigneeName;
    private Long consigneePoid;
    private LovItem consigneeDet;

    private String manifestEmailVerified;
    private String noRecentShipmentVerified;

    private Long bookingCustomerPoid;
    private LovItem bookingCustomerDet;
    private String ediAddress;

    private String preCarried;
    private Long loadPortPoid;
    private LovItem loadPortDet;
    private Long receiptPortPoid;
    private LovItem receiptPortDet;
    private Long deliveryPortPoid;
    private LovItem deliveryPortDet;
    private Long dischargePortPoid;
    private LovItem dischargePortDet;

    private Long commodityPoid;
    private LovItem commodityDet;

    private BigDecimal grossWeight;
    private BigDecimal cbm;
    private String weight;
    private String packTypes;
    private BigDecimal numberOfPacks;

    private String notifyName;
    private Long notify1Poid;
    private LovItem notify1Det;

    private String notifyEdiAddress;
    private List<AddressDetailsDto> addressDetails;

    private String holdCanAuto;
    private String manualCanSend;

    private String holdReason;
    private LovItem holdReasonDet;
    private String holdRemarks;

    private String simpleCargoDescription;
    private String simpleCargoMarks;

    private List<DescriptionAndMarksDto> descriptionsAndMarks;
    private List<MafiDetailsDto> mafiDetails;
    private OtherNotifyDto otherNotifies;
    private List<GeneralCargoDto> generalCargoDetails;
    private List<ContainerDto> containers;
    private List<ChargeDto> charges;
    private List<ChargeOtherDto> otherCharges;
    private List<PartBlDto> partBls;
}
