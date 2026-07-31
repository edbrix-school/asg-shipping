package com.asg.shipping.daycloseshiping.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DayCloseSummaryProjection {
	LocalDate getTransactionDate();

	BigDecimal getCashAmount();

	BigDecimal getChequeAmount();

	BigDecimal getTotalAmount();

	Long getNoOfCheques();

	String getVerifiedRcvd();

	String getMainOfcRemarks();
}
