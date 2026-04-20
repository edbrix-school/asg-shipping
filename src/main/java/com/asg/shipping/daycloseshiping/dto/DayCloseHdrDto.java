package com.asg.shipping.daycloseshiping.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DayCloseHdrDto {

	private Long transactionPoid;
	private Long groupPoid;
	private Long companyPoid;
	private LocalDate transactionDate;
	private String docRef;

	private BigDecimal cashAmount;
	private BigDecimal chequeAmount;
	private BigDecimal totalAmount;
	private Long noOfCheques;

	private String locRemarks;
	private String verifiedRcvd;
	private String mainOfcRemarks;

	private String locationCode;
	private BigDecimal outstandingAmount;

	private String createdBy;
	private LocalDateTime createdDate;
}
