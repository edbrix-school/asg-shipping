package com.asg.shipping.receipts.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.receipts.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ReceiptsService {

	ReceiptsBlDetailsDto getReceipt(Long transactionPoid);

	ReceiptsBlDetailsDto createReceipt(ReceiptsCreateDto createDto);

	ReceiptsBlDetailsDto updateReceipt(Long transactionPoid, ReceiptsUpdateDto updateDto);

	void deleteReceipt(Long transactionPoid);

	Map<String, Object> list(FilterRequestDto filters, Pageable pageable);

	ReceiptAutoPopulateDto autoPopulateFields(Long blPoid,Long transactionPoid);

	ReceiptCalculateDemurrageResponseDto calculateDemurrage(ReceiptCalculateDemurrageRequestDto requestDto);
}
