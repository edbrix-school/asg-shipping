package com.asg.shipping.receipts.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "AR_SH_RECEIPT_PYMT_DETAILS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptPymtDetails extends BaseEntity {

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

	@Column(name = "VERIFY_CHEQUE")
	private String verifyCheque;

	@Column(name = "TT_BANK_POID")
	private Long ttBankPoid;
;
}
