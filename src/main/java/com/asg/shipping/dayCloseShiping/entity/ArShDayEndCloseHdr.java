package com.asg.shipping.dayCloseShiping.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AR_SH_DAY_END_CLOSE_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShDayEndCloseHdr extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "TRANSACTION_DATE")
	private LocalDate transactionDate;

	@Column(name = "GROUP_POID")
	private Long groupPoid;

	@Column(name = "COMPANY_POID")
	private Long companyPoid;

	@Column(name = "DOC_REF")
	private String docRef;

	@Column(name = "LOCATION_CODE")
	private String locationCode;

	@Column(name = "CASH_AMOUNT")
	private BigDecimal cashAmount;

	@Column(name = "CHEQUE_AMOUNT")
	private BigDecimal chequeAmount;

	@Column(name = "OUTSTANDING_AMOUNT")
	private BigDecimal outstandingAmount;

	@Column(name = "TOTAL_AMOUNT")
	private BigDecimal totalAmount;

	@Column(name = "NOOF_CHQS")
	private Long noOfCheques;

	@Column(name = "LOC_REMARKS")
	private String locRemarks;

	@Column(name = "VERIFIED_RCVD")
	private String verifiedRcvd;

	@Column(name = "MAIN_OFC_REMARKS")
	private String mainOfcRemarks;

	@Column(name = "DELETED")
	private String deleted;
}
