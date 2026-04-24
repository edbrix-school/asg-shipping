package com.asg.shipping.receipts.repository;


import com.asg.shipping.receipts.dto.ChargeDto;
import com.asg.shipping.receipts.dto.ReceiptAutoPopulateDto;
import com.asg.shipping.receipts.dto.ReceiptBlAutoPopulateDto;
import com.asg.shipping.receipts.dto.TaxConfig;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ShipReceiptProcRepository {

	void afterSave(Long groupPoid, Long companyPoid, Long docKeyPoid, Long detRowId, String updateType, String loginUser);

	String validateBlacklistedCustomer(String accountNo, Long bankPoid);

	BigDecimal validateDemurrageAmount(Long blPoid, String containerNo, BigDecimal demAmount);

	String validateFinancialYear(Long companyPoid, LocalDate transactionDate);

	String validateTransactionPeriod(Long companyPoid, LocalDate transactionDate);

	String getReceiptType(Long blPoid);

	ReceiptBlAutoPopulateDto autoPopulateFields(Long blPoid);

	void doCntPrintAfter(Long groupPoid, Long companyPoid, Long docKeyPoid, Long detRowId, String updateType, String reprintUser);

	String receiptValidate(Long transactionPoid, Long companyPoid, Long userId);

	String glLedgerPostShRcpInv(Long groupPoid, Long companyPoid, Long userPoid, String docId, Long transactionPoid, String docRef);


	 BigDecimal calculateDemurrageAmount(Long currentTransactionPoid, Long blPoid, String containerNo, String containerType,
	 	 	 	 	 	 	 	 	 	 Long linePoid, LocalDate fromDate, LocalDate toDate, Long extraFreeDays);

	TaxConfig getDemurrageTaxInfo(Long companyPoid);

	String getContainerSize(String containerIsoType);

	java.util.List<ChargeDto> getCombinedCharges(Long blPoid, Long companyPoid);

    String validateDuplicateBlReceipt(Long blPoid, String remarks);

    String validateChequeDate(LocalDate chequeDate, String type);

    String validatePdcDateAgainstInvoice(Long blPoid, LocalDate chequeDate);

    Long getBankCompany(Long bankPoid);

    String getGlobalParameter(String paramName, String paramKeyIdType, Long companyPoid, String defaultValue);
}
