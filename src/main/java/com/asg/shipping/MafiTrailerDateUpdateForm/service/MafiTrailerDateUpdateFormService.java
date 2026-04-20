package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormResponse;

public interface MafiTrailerDateUpdateFormService {
	Map<String, Object> getAll(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate);

	MafiTrailerDateUpdateFormResponse getById(Long transactionPoid);

    MafiTrailerDateUpdateFormResponse update(Long transactionPoid, MafiTrailerDateUpdateFormRequest request);

}
