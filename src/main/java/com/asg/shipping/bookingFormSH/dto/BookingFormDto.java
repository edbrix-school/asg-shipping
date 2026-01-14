package com.asg.shipping.bookingFormSH.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.asg.shipping.common.dto.LovItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SHIP_MATE_HDR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormDto {

	private Long transactionPoid;
	private Long groupPoid;
	private Long companyPoid;
	private String docRef;
	private LocalDate transactionDate;
	private String vessalAgentName;
	private Long shipperPoid;
//	private LovItem shipperPoidDet; // LOV data
	private Long shipperAddressPoid;
//	private LovItem shipperAddressPoidDet; // LOV data
	private Long consigneePoid;
//	private LovItem consigneePoidDet; // LOV data
	private Long consigneeAddressPoid;
//	private LovItem consigneeAddressPoidDet; // LOV data
	private Long notifyPoid1;
//	private LovItem notifyPoid1Det; // LOV data
	private Long notifyAddressPoid1;
//	private LovItem notifyAddressPoid1Det; // LOV data
	private Long notifyPoid2;
//	private LovItem notifyPoid2Det; // LOV data
	private Long notifyAddressPoid2;
//	private LovItem notifyAddressPoid2Det; // LOV data
	private Long quotationTransactionPoid;
	private LovItem quotationTransactionPoidDet; // LOV data
	private Long vesselPoid;
	private LovItem vesselPoidDet; // LOV data
	private LocalDate vesselEtaDate;
	private Long linePoid;
	private LovItem linePoidDet; // LOV data
	private Long salesmanPoid;
	private LovItem salesmanPoidDet; // LOV data
	private Long comodityPoid;
	private LovItem comodityPoidDet; // LOV data
	private BigDecimal totalVolume;
	private BigDecimal totalWeight;
	private String unitPack;
	private BigDecimal totalNoOfPacks;
	private Long placeOfRecieptPoid;
	private LovItem placeOfRecieptPoidDet; // LOV data
	private Long placeOfDelieveryPoid;
	private LovItem placeOfDelieveryPoidDet; // LOV data
	private Long portOfLoadingPoid;
	private LovItem portOfLoadingPoidDet; // LOV data
	private Long portOfDischargePoid;
	private LovItem portOfDischargePoidDet; // LOV data
	private String remarks;
	private String mateStatus;
	private String voyageNo;
	private String bookingIssueNo;
	private LocalDate mateLoadDate;
	private Long mateLoadNo;
	private Long mateLoadVoyagePoid;
	private LovItem mateLoadVoyagePoidDet; // LOV data
	private String issueType;
	private String deleted;
	private String consigneeName;
	private String consigneeAddress;
	private Long splitBookingNo;
	private String finalDestination;
	private String shipperDetailsManually;

	// Detail tables
	private List<BookingFormCargoDetailDto> cargoDetails;
	private List<BookingFormChargesDetailDto> chargesDetails;
	private List<BookingFormContainerDetailDto> containerDetails;
}
