package com.asg.shipping.dayCloseShiping.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class ArShDayEndCloseDtl {

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

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
