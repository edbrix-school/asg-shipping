package com.asg.shipping.receipts.entity;

import java.math.BigDecimal;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "AR_SH_RECEIPT_CHARGES_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptChargesDtl extends BaseEntity {

	@EmbeddedId
	private TransactionDtlId id;

	@Column(name = "BL_POID")
	private Long blPoid;

	@Column(name = "CHARGES_DET_ROW_ID")
	private Long chargesDetRowId;

	@Column(name = "CHARGE_POID")
	private Long chargePoid;

	@Column(name = "AMOUNT")
	private BigDecimal amount;

	@Column(name = "AMOUNT_SELECT")
	private String amountSelect;

	@Column(name = "CHARGE_NEW_RECORD")
	private String chargeNewRecord;

	@Column(name = "INVOICE_TYPE")
	private String invoiceType;

	@Column(name = "TAX_POID")
	private Long taxPoid;

	@Column(name = "TAX_PERCENTAGE")
	private BigDecimal taxPercentage;

	@Column(name = "TAX_AMOUNT")
	private BigDecimal taxAmount;


}
