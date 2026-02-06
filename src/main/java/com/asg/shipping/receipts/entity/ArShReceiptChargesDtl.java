package com.asg.shipping.receipts.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AR_SH_RECEIPT_CHARGES_DTL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptChargesDtl {

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

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;

	@Column(name = "LASTMODIFIED_BY")
	private String lastModifiedBy;

	@Column(name = "LASTMODIFIED_DATE")
	private LocalDateTime lastModifiedDate;

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

	@ManyToOne
	@JoinColumn(name = "TRANSACTION_POID", referencedColumnName = "TRANSACTION_POID", 
		foreignKey = @ForeignKey(name = "AR_SH_RECEIPT_CHARGES_DTL_FK1"), insertable = false, updatable = false)
	private ArShReceiptHdr receiptHdr;
}
