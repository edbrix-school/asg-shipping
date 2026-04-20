package com.asg.shipping.customerinvoicechargemapmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface CustomerInvoiceChargeMapMasterService {

    CustomerInvoiceChargeMapMasterResponse getByCustomer(Long customerPoid, Long groupPoid);

    CustomerInvoiceChargeMapMasterResponse saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid
    );

    void deleteDetail(
            Long customerPoid,
            DeleteReasonDto deleteReasonDto
    );

    Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable);
}
