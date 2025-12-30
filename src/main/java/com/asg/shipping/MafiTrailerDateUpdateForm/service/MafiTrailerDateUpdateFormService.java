package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormResponse;

public interface MafiTrailerDateUpdateFormService {
	Map<String, Object> getAll(String docId, FilterRequestDto request, Pageable pageable);

	MafiTrailerDateUpdateFormResponse getById(Long transactionPoid, Long groupPoid, Long companyPoid);

	void update(Long transactionPoid, MafiTrailerDateUpdateFormRequest request,
			Long groupPoid, Long companyPoid, String userId);

}
