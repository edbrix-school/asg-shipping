package com.asg.shipping.receipts.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDtlId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "DET_ROW_ID")
	private Long detRowId;
}
