package com.asg.shipping.receipts.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.enums.ButtonType;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ReceiptsService {

	ReceiptsBlDetailsDto getReceipt(Long transactionPoid);

	ReceiptSaveResponseDto createReceipt(ReceiptsCreateDto createDto);

	ReceiptSaveResponseDto updateReceipt(Long transactionPoid, ReceiptsUpdateDto updateDto);

	void deleteReceipt(Long transactionPoid, DeleteReasonDto deleteReasonDto);

	Map<String, Object> list(FilterRequestDto filters, Pageable pageable, LocalDate startDate, LocalDate endDate);

	ReceiptAutoPopulateDto autoPopulateFields(Long blPoid,Long transactionPoid);

	ReceiptCalculateDemurrageResponseDto calculateDemurrage(ReceiptCalculateDemurrageRequestDto requestDto);

	byte[] receiptAndInvoicePrint(Long transactionPoid, Long blPoid, ButtonType buttonType) throws Exception;
}
