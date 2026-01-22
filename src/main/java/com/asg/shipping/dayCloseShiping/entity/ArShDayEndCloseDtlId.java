package com.asg.shipping.dayCloseShiping.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArShDayEndCloseDtlId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "DET_ROW_ID")
	private Long detRowId;
}
