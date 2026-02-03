package com.asg.shipping.receipts.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "AR_SH_RECEIPT_CONTAINER_DTL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptContainerDtl {

	@EmbeddedId
	private TransactionDtlId id;

	@Column(name = "BL_POID")
	private Long blPoid;

	@Column(name = "CONTAINER_SOC_YN")
	private String containerSocYn;

	@Column(name = "CONTAINER_NO")
	private String containerNo;

	@Column(name = "DM_FRM_DATE")
	private LocalDate dmFrmDate;

	@Column(name = "DM_TO_DATE")
	private LocalDate dmToDate;

	@Column(name = "DM_DAYS")
	private Long dmDays;

	@Column(name = "DM_CHARGE_AMT")
	private BigDecimal dmChargeAmt;

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;

	@Column(name = "LASTMODIFIED_BY")
	private String lastModifiedBy;

	@Column(name = "LASTMODIFIED_DATE")
	private LocalDateTime lastModifiedDate;

	@Column(name = "FREE_DAYS")
	private Long freeDays;

	@Column(name = "EQUIPMENT_ISO_TYPE")
	private String equipmentIsoType;

	@Column(name = "DLV_FORM_PRINTED")
	private String dlvFormPrinted;

	@Column(name = "RTN_FORM_PRINTED")
	private String rtnFormPrinted;

	@Column(name = "EMPTY_IN")
	private LocalDate emptyIn;

	@Column(name = "CNT_TAX_POID")
	private Long cntTaxPoid;

	@Column(name = "CNT_TAX_PERCENTAGE")
	private BigDecimal cntTaxPercentage;

	@Column(name = "CNT_TAX_AMOUNT")
	private BigDecimal cntTaxAmount;

	@ManyToOne
	@JoinColumn(name = "TRANSACTION_POID", referencedColumnName = "TRANSACTION_POID", 
		foreignKey = @ForeignKey(name = "AR_SH_RECEIPT_CONTAINER_D_FK1"), insertable = false, updatable = false)
	private ArShReceiptHdr receiptHdr;
}
