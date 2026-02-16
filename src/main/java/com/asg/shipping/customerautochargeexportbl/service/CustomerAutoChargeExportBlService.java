package com.asg.shipping.customerautochargeexportbl.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface CustomerAutoChargeExportBlService {

    Map<String, Object> list(FilterRequestDto filters, Pageable pageable);



    CustomerAutoChargeExportBLDto getCustomerAutoChargeExportBL(Long id);


    CustomerAutoChargeExportBLDto createCustomerAutoChargeExportBL(CustomerAutoChargeExportBLCreateDTO createDTO);

    CustomerAutoChargeExportBLDto updateCustomerAutoChargeExportBL(Long id, CustomerAutoChargeExportBLUpdateDTO updateDTO);


    void deleteCustomerAutoChargeExportBL(Long id, DeleteReasonDto deleteReasonDto);
}
