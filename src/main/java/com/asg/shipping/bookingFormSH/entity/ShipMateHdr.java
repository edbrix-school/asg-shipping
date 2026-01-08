package com.asg.shipping.bookingFormSH.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.asg.common.lib.security.util.UserContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_MATE_HDR", uniqueConstraints = {
		@UniqueConstraint(name = "UK_DOCREFFSHIP_MATE_HDR", columnNames = { "DOC_REF" }),
		@UniqueConstraint(name = "UK_BOOKINGNO_MATE_HDR", columnNames = { "BOOKING_ISSUE_NO" }) })
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipMateHdr {

	@Id
	@Column(name = "TRANSACTION_POID", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long transactionPoid;

	@Column(name = "GROUP_POID", nullable = false)
	private Long groupPoid;

	@Column(name = "COMPANY_POID", nullable = false)
	private Long companyPoid;

	@Column(name = "DOC_REF", length = 25, unique = true)
	private String docRef;

	@Column(name = "TRANSACTION_DATE")
	private LocalDate transactionDate;

	@Column(name = "VESSAL_AGENT_NAME", length = 100)
	private String vessalAgentName;

	@Column(name = "SHIPPER_POID", nullable = false)
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

	@Column(name = "QUOTATION_TRANSACTION_POID")
	private Long quotationTransactionPoid;

	@Column(name = "VESSEL_POID", nullable = false)
	private Long vesselPoid;

	@Column(name = "VESSEL_ETA_DATE", nullable = false)
	private LocalDate vesselEtaDate;

	@Column(name = "LINE_POID", nullable = false)
	private Long linePoid;

	@Column(name = "SALESMAN_POID", nullable = false)
	private Long salesmanPoid;

	@Column(name = "COMODITY_POID")
	private Long comodityPoid;

	@Column(name = "TOTAL_VOLUME", precision = 20, scale = 3)
	private BigDecimal totalVolume;

	@Column(name = "TOTAL_WEIGHT", precision = 20, scale = 3)
	private BigDecimal totalWeight;

	@Column(name = "UNIT_PACK", length = 6)
	private String unitPack;

	@Column(name = "TOTAL_NO_OF_PACKS", precision = 7, scale = 2)
	private BigDecimal totalNoOfPacks;

	@Column(name = "PLACE_OF_RECIEPT_POID")
	private Long placeOfRecieptPoid;

	@Column(name = "PLACE_OF_DELIEVERY_POID")
	private Long placeOfDelieveryPoid;

	@Column(name = "PORT_OF_LOADING_POID")
	private Long portOfLoadingPoid;

	@Column(name = "PORT_OF_DISCHARGE_POID")
	private Long portOfDischargePoid;

	@Column(name = "REMARKS", length = 250)
	private String remarks;

	@Column(name = "MATE_STATUS", length = 25)
	private String mateStatus;

	@Column(name = "VOYAGE_NO", length = 20)
	private String voyageNo;

	@Column(name = "BOOKING_ISSUE_NO", length = 50, unique = true)
	private String bookingIssueNo;

	@Column(name = "MATE_LOAD_DATE")
	private LocalDate mateLoadDate;

	@Column(name = "MATE_LOAD_NO", precision = 25, scale = 0)
	private Long mateLoadNo;

	@Column(name = "MATE_LOAD_VOYAGE_POID")
	private Long mateLoadVoyagePoid;

	@Column(name = "ISSUE_TYPE", length = 25)
	private String issueType;

	@Column(name = "DELETED", length = 1)
	private String deleted;

	@Column(name = "CONSIGNEE_NAME", length = 200)
	private String consigneeName;

	@Column(name = "CONSIGNEE_ADDRESS", length = 500)
	private String consigneeAddress;

	@Column(name = "SPLIT_BOOKING_NO")
	private Long splitBookingNo;

	@Column(name = "FINAL_DESTINATION", length = 50)
	private String finalDestination;

	@Column(name = "SHIPPER_DETAILS_MANUALLY", length = 100)
	private String shipperDetailsManually;

	@Column(name = "CREATED_BY", length = 20)
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;

	@Column(name = "LASTMODIFIED_BY", length = 20)
	private String lastModifiedBy;

	@Column(name = "LASTMODIFIED_DATE")
	private LocalDateTime lastModifiedDate;

	@PrePersist
	protected void onCreate() {
		createdDate = LocalDateTime.now();
		if (createdBy == null) {
			createdBy = UserContext.getUserName();
		}
		if (deleted == null) {
			deleted = "N";
		}
		if (mateStatus == null) {
			mateStatus = "OPEN";
		}
		if (issueType == null) {
			issueType = "FULL";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		lastModifiedDate = LocalDateTime.now();
		lastModifiedBy = UserContext.getUserName();

		// Auto-update MATE_STATUS based on ISSUE_TYPE (from trigger logic)
		if (issueType != null && (issueType.equals("LNISSUE") || issueType.equals("SOLD"))) {
			mateStatus = "CLOSED";
		}
	}
}
