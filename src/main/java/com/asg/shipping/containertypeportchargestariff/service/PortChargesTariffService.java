package com.asg.shipping.containertypeportchargestariff.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface PortChargesTariffService {

    Map<String, Object> listPortChargesTariff(String docId, FilterRequestDto request, Pageable pageable);

    PortChargesTariffDto getPortChargesTariff(Long id);

    PortChargesTariffDto createPortChargesTariff(PortChargesTariffCreateDto dto, Long groupPoid, Long userPoid);

    PortChargesTariffDto updatePortChargesTariff(Long id, PortChargesTariffUpdateDto dto, Long groupPoid, Long userPoid);

    void deletePortChargesTariff(Long id);

    ValidateOverlapResponseDto validateOverlap(ValidateOverlapRequestDto request);
}