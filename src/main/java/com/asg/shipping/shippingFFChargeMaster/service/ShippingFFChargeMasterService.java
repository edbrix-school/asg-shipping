package com.asg.shipping.shippingFFChargeMaster.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ShippingFFChargeMasterService {

    ChargeDto createCharge(ChargeCreateDTO dto, Long groupPoid, Long userPoid);

    ChargeDto updateCharge(Long id, ChargeUpdateDTO dto, Long groupPoid, Long userPoid);

    ChargeDto getCharge(Long id);

    void deleteCharge(Long id);

    Map<String, Object> searchCharges(String docId, FilterRequestDto request, Pageable pageable);
}
