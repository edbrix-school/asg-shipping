package com.asg.shipping.bookingFormSH.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_MATE_CARGO_DTL")
@IdClass(ShipMateCargoDtlId.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipMateCargoDtl {

	@Id
	@Column(name = "TRANSACTION_POID", nullable = false)
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID", nullable = false)
	private Long detRowId;

	@Column(name = "CARGO_DESCRIPTION", length = 500)
	private String cargoDescription;

	@Column(name = "EQUIPMENT_TYPE", length = 20)
	private String equipmentType;

	@Column(name = "EQUIPMENT_SIZE", length = 20)
	private String equipmentSize;

	@Column(name = "QUANTITY", precision = 20, scale = 3)
	private BigDecimal quantity;

	@Column(name = "VOLUME", precision = 20, scale = 3)
	private BigDecimal volume;

	@Column(name = "WEIGHT", precision = 20, scale = 3)
	private BigDecimal weight;

	@Column(name = "EQUIPMENT_ISO_TYPE", length = 20)
	private String equipmentIsoType;

	@Column(name = "IS_IMCO", length = 1)
	private String isImco;

	@Column(name = "IMO", length = 20)
	private String imo;

	@Column(name = "IS_OOG", length = 1)
	private String isOog;

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

	@Column(name = "CREATED_BY", length = 20)
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;

	@Column(name = "LASTMODIFIED_BY", length = 20)
	private String lastModifiedBy;

	@Column(name = "LASTMODIFIED_DATE")
	private LocalDateTime lastModifiedDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
	private ShipMateHdr shipMateHdr;

	@PrePersist
	protected void onCreate() {
		createdDate = LocalDateTime.now();
		if (createdBy == null) {
			createdBy = UserContext.getUserName();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		lastModifiedDate = LocalDateTime.now();
		lastModifiedBy = UserContext.getUserName();
	}
}
