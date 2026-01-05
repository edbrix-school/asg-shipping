package com.asg.shipping.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GLOBAL_CURRENCY_DINOMINATION")
@Getter
@Setter
public class GlobalCurrencyDenomination {

	@Id
	@Column(name = "DET_ROW_ID")
	private Long detRowId;

	@Column(name = "CURRENCY_CODE")
	private String currencyCode;

	@Column(name = "CURRENCY_AMOUNT")
	private String currencyAmount;

	@Column(name = "CURRENCY_TYPE")
	private String currencyType;

	@Column(name = "SEQNO")
	private Integer seqNo;
}
