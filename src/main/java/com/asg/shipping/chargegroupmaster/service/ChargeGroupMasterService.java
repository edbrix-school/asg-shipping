package com.asg.shipping.chargegroupmaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ChargeGroupMasterService {

    ChargeGroupMasterResponseDto create(ChargeGroupMasterRequestDto request);

    ChargeGroupMasterResponseDto update(Long chargeGroupPoid,
                                        ChargeGroupMasterRequestDto request);

    ChargeGroupMasterResponseDto findById(Long chargeGroupPoid);

    void delete(Long chargeGroupPoid);

    Map<String, Object> listChargeGroupMaster(String docId, FilterRequestDto request, Pageable pageable);


}
