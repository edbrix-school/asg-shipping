package com.asg.shipping.bookingFormSH.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Booking Form record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormCreateDTO {

    private LocalDate transactionDate;
    private String vessalAgentName;

    @NotNull(message = "Shipper POID is required")
    private Long shipperPoid;

    private Long shipperAddressPoid;
    private Long consigneePoid;
    private Long consigneeAddressPoid;
    private Long notifyPoid1;
    private Long notifyAddressPoid1;
    private Long notifyPoid2;
    private Long notifyAddressPoid2;
    private Long quotationTransactionPoid;

    @NotNull(message = "Vessel POID is required")
    private Long vesselPoid;

    @NotNull(message = "Vessel ETA Date is required")
    private LocalDate vesselEtaDate;

    @NotNull(message = "Line POID is required")
    private Long linePoid;

    @NotNull(message = "Salesman POID is required")
    private Long salesmanPoid;

    private Long comodityPoid;
    private BigDecimal totalVolume;
    private BigDecimal totalWeight;
    private String unitPack;
    private BigDecimal totalNoOfPacks;
    private Long placeOfRecieptPoid;
    private Long placeOfDelieveryPoid;
    private Long portOfLoadingPoid;
    private Long portOfDischargePoid;
    private String remarks;
    private String mateStatus;
    private String voyageNo;
    private String bookingIssueNo;
    private LocalDate mateLoadDate;
    private Long mateLoadNo;
    private Long mateLoadVoyagePoid;
    private String issueType;
    private String consigneeName;
    private String consigneeAddress;
    private Long splitBookingNo;
    private String finalDestination;
    private String shipperDetailsManually;

    // Detail tables
    private List<BookingFormCargoDetailDtoRequest> cargoDetails;
    private List<BookingFormChargesDetailDtoRequest> chargesDetails;
    private List<BookingFormContainerDetailDtoRequest> containerDetails;
}
