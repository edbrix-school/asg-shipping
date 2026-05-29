package com.asg.shipping.bookingFormSH.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Booking Form record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormUpdateDTO {

	private LocalDate transactionDate;
	private String vessalAgentName;
	private Long shipperPoid;
	private Long shipperAddressPoid;
	private Long consigneePoid;
	private Long consigneeAddressPoid;
	private Long notifyPoid1;
	private Long notifyAddressPoid1;
	private Long notifyPoid2;
	private Long notifyAddressPoid2;
	private Long quotationTransactionPoid;
	private Long vesselPoid;
	private LocalDate vesselEtaDate;
	private Long linePoid;
	private Long salesmanPoid;
	private Long comodityPoid;
	private Double totalVolume;
	private Double totalWeight;
	private String unitPack;
	private Double totalNoOfPacks;
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
	private List<BookingFormStuffingLoadDetailDtoRequest> stuffingDetails;
}
