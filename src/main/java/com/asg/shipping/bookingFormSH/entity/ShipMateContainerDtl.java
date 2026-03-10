package com.asg.shipping.bookingFormSH.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.common.lib.security.util.UserContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_MATE_CONTAINER_DTL", uniqueConstraints = {
		@UniqueConstraint(name = "UK_SHIP_MATCNTDTL", columnNames = { "TRANSACTION_POID", "CONTAINER_NO" }) })
@IdClass(ShipMateContainerDtlId.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipMateContainerDtl extends BaseEntity {

	@Id
	@Column(name = "TRANSACTION_POID", nullable = false)
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID", nullable = false)
	private Long detRowId;

	@Column(name = "CONTAINER_NO", length = 25)
	private String containerNo;

	@Column(name = "EQUIPMENT_SEAL_NO", length = 25)
	private String equipmentSealNo;

	@Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
	private String equipmentIsoType;

	@Column(name = "EQUIPMENT_TYPE", length = 20)
	private String equipmentType;

	@Column(name = "EQUIPMENT_SIZE", length = 20)
	private String equipmentSize;

	@Column(name = "QUANTITY", precision = 20, scale = 3)
	private BigDecimal quantity;

	@Column(name = "GRS_VOLUME", precision = 20, scale = 3)
	private BigDecimal grsVolume;

	@Column(name = "GRS_WEIGHT", precision = 20, scale = 3)
	private BigDecimal grsWeight;

	@Column(name = "NET_VOLUME", precision = 20, scale = 3)
	private BigDecimal netVolume;

	@Column(name = "NET_WEIGHT", precision = 20, scale = 3)
	private BigDecimal netWeight;

	@Column(name = "NO_OF_PACKS", precision = 20, scale = 3)
	private BigDecimal noOfPacks;

	@Column(name = "PACK_UNIT", length = 6)
	private String packUnit;

	@Column(name = "COMODITY_POID")
	private Long comodityPoid;

	@Column(name = "DESTINATION_PORT_POID")
	private Long destinationPortPoid;

	@Column(name = "IMO", length = 200)
	private String imo;

	@Column(name = "OOG_L", length = 20)
	private String oogL;

	@Column(name = "OOG_B", length = 20)
	private String oogB;

	@Column(name = "OOG_H", length = 20)
	private String oogH;

	@Column(name = "REFFER_TEMP", length = 20)
	private String refferTemp;

	@Column(name = "REFFER_HUM", length = 20)
	private String refferHum;

	@Column(name = "REFFER_VENT", length = 20)
	private String refferVent;

	@Column(name = "CARGO_DESCRIPTION", length = 200)
	private String cargoDescription;

	@Column(name = "EQUIPMENT_SHIPPER_OWN", length = 1)
	private String equipmentShipperOwn;

	@Column(name = "ISSUE_TO_SHIPPER")
	private LocalDate issueToShipper;

	@Column(name = "RETURN_FROM_SHIPPER")
	private LocalDate returnFromShipper;

	@Column(name = "RELEASE_ALLOCATION", length = 25)
	private String releaseAllocation;

	@Column(name = "IS_IMCO", length = 1)
	private String isImco;

	@Column(name = "IS_OOG", length = 1)
	private String isOog;

	@Column(name = "IS_REFER", length = 1)
	private String isRefer;

	@Column(name = "REFER_TYPE", length = 50)
	private String referType;

	@Column(name = "OOG_L_W", length = 20)
	private String oogLW;

	@Column(name = "OOG_R_W", length = 20)
	private String oogRW;

	@Column(name = "OOG_F", length = 20)
	private String oogF;

	@Column(name = "OOG_A", length = 20)
	private String oogA;

	@Column(name = "IS_SPLIT", length = 1)
	private String isSplit;

	@Column(name = "IMCO_CLASS_TYPE", length = 25)
	private String imcoClassType;

	@Column(name = "OOG_TYPE", length = 25)
	private String oogType;

	@Column(name = "VGM_WEIGHT", precision = 20, scale = 3)
	private BigDecimal vgmWeight;

	@Column(name = "VGM_DOC_ID", length = 20)
	private String vgmDocId;

	@Column(name = "VGM_DATE")
	private LocalDate vgmDate;

	@Column(name = "VGM_EDI", length = 1)
	private String vgmEdi;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
	private ShipMateHdr shipMateHdr;

	@PrePersist
	protected void onCreate() {

		if (equipmentShipperOwn == null) {
			equipmentShipperOwn = "N";
		}
		if (releaseAllocation == null) {
			releaseAllocation = "ALLOCATED";
		}
		if (isSplit == null) {
			isSplit = "N";
		}
		if (vgmEdi == null) {
			vgmEdi = "N";
		}
		// Trim and clean container number (from trigger logic)
		if (containerNo != null) {
			containerNo = containerNo.trim().replace(" ", "");
		}
	}

	@PreUpdate
	protected void onUpdate() {
		// Trim and clean container number (from trigger logic)
		if (containerNo != null) {
			containerNo = containerNo.trim().replace(" ", "");
		}
	}
}
