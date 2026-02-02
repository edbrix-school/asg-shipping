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
@Table(name = "AR_SH_RECEIPT_PYMT_DETAILS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptPymtDetails {

	@EmbeddedId
	private TransactionDtlId id;

	@Column(name = "PYMT_TYPE", nullable = false)
	private String pymtType;

	@Column(name = "CHQ_CARDNO")
	private String chqCardno;

	@Column(name = "CHQ_DATE")
	private LocalDate chqDate;

	@Column(name = "BANK_POID")
	private Long bankPoid;

	@Column(name = "ACCOUNT_POID")
	private Long accountPoid;

	@Column(name = "ACCOUNT_NAME")
	private String accountName;

	@Column(name = "ACCOUNT_NO")
	private String accountNo;

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

	@Column(name = "VERIFY_CHEQUE")
	private String verifyCheque;

	@Column(name = "TT_BANK_POID")
	private Long ttBankPoid;

	@ManyToOne
	@JoinColumn(name = "TRANSACTION_POID", referencedColumnName = "TRANSACTION_POID", 
		foreignKey = @ForeignKey(name = "AR_SH_RECEIPT_PYMT_DETAIL_FK1"), insertable = false, updatable = false)
	private ArShReceiptHdr receiptHdr;
}
