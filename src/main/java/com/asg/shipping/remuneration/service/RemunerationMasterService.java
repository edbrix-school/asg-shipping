package com.asg.shipping.remuneration.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface RemunerationMasterService {
    Map<String, Object> listRemunerations(String docId, FilterRequestDto request, Pageable pageable);

    ShipRemunerationMasterResponseDto createRemuneration(ShipRemunerationMasterRequestDto requestDto);

    ShipRemunerationMasterResponseDto updateRemuneration(Long id, ShipRemunerationMasterRequestDto requestDto);

    ShipRemunerationMasterResponseDto getRemunerationById(Long id);

    void deleteRemuneration(Long id, DeleteReasonDto deleteReasonDto);
}
