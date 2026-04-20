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
@Table(name = "AR_SH_RECEIPT_CONTAINER_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptContainerDtl extends BaseEntity {

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

}
