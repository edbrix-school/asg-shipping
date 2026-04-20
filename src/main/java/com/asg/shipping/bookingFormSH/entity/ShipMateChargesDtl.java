package com.asg.shipping.bookingFormSH.entity;

import java.math.BigDecimal;

import com.asg.common.lib.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_MATE_CHARGES_DTL")
@IdClass(ShipMateChargesDtlId.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShipMateChargesDtl extends BaseEntity {

	@Id
	@Column(name = "TRANSACTION_POID", nullable = false)
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID", nullable = false)
	private Long detRowId;

	@Column(name = "CHARGE_POID")
	private Long chargePoid;

	@Column(name = "CURRENCY_EXCHANGE", precision = 20, scale = 3)
	private BigDecimal currencyExchange;

	@Column(name = "QUANTITY", precision = 20, scale = 3)
	private BigDecimal quantity;

	@Column(name = "PER_QUANTITY_AMOUNT", precision = 20, scale = 3)
	private BigDecimal perQuantityAmount;

	@Column(name = "PAID_AT_PORT_POID")
	private Long paidAtPortPoid;

	@Column(name = "BUY_PERCHARGE", precision = 20, scale = 3)
	private BigDecimal buyPercharge;

	@Column(name = "CURRENCY_CODE", length = 50)
	private String currencyCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
	private ShipMateHdr shipMateHdr;

}
