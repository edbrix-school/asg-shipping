package com.asg.shipping.dayCloseShiping.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AR_SH_DAY_END_CLOSE_DTL")
@Getter
@Setter
@Builder
@IdClass(ArShDayEndCloseDtlId.class)
@NoArgsConstructor
@AllArgsConstructor
public class ArShDayEndCloseDtl extends BaseEntity {

	@Id
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID")
	private Long detRowId;

	@Column(name = "CURRENCY_AMOUNT")
	private BigDecimal currencyAmount;

	@Column(name = "CURRENCY_TYPE")
	private String currencyType;

	@Column(name = "NO_OF_TRAN")
	private Long noOfTran;

	@Column(name = "CASH_AMOUNT")
	private BigDecimal cashAmount;
}
