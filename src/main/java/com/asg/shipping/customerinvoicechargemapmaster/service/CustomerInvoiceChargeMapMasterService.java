package com.asg.shipping.customerinvoicechargemapmaster.service;

import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;

public interface CustomerInvoiceChargeMapMasterService {

    CustomerInvoiceChargeMapMasterResponse getByCustomer(Long customerPoid, Long groupPoid);

    void saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid,
            String userId
    );

    void deleteDetail(
            Long customerPoid,
            Long detRowId,
            Long groupPoid,
            String userId
    );
}
