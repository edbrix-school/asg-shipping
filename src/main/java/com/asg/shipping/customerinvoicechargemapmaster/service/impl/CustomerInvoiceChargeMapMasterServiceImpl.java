package com.asg.shipping.customerinvoicechargemapmaster.service.impl;

import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapDetailDto;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlEntity;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlId;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtMasterEntity;
import com.asg.shipping.customerinvoicechargemapmaster.repository.CustomerInvoicePrtDtlRepository;
import com.asg.shipping.customerinvoicechargemapmaster.repository.CustomerInvoicePrtMasterRepository;
import com.asg.shipping.customerinvoicechargemapmaster.service.CustomerInvoiceChargeMapMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerInvoiceChargeMapMasterServiceImpl
        implements CustomerInvoiceChargeMapMasterService {

    private final CustomerInvoicePrtMasterRepository masterRepo;
    private final CustomerInvoicePrtDtlRepository detailRepo;

    // ========================= GET =========================

    @Override
    @Transactional(readOnly = true)
    public CustomerInvoiceChargeMapMasterResponse getByCustomer(
            Long customerPoid,
            Long groupPoid) {

        CustomerInvoicePrtMasterEntity master =
                masterRepo.findById(customerPoid)
                        .filter(m -> !"Y".equals(m.getDeleted()))
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer invoice charge mapping not found"
                                ));

        List<CustomerInvoicePrtDtlEntity> details =
                detailRepo.findByIdCustomerPoid(customerPoid);

        if (details.isEmpty()) {
            throw new RuntimeException(
                    "Customer invoice charge mapping not found"
            );
        }

        CustomerInvoiceChargeMapMasterResponse response =
                new CustomerInvoiceChargeMapMasterResponse();

        response.setCustomerPoid(customerPoid);
        response.setDetails(
                details.stream()
                        .map(this::mapToDetailDto)
                        .toList()
        );

        return response;
    }

    // ========================= SAVE / UPDATE =========================

    @Override
    public void saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid,
            String userId) {

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one charge mapping detail is required");
        }

        CustomerInvoicePrtMasterEntity master =
                masterRepo.findById(request.getCustomerPoid())
                        .orElseGet(() -> createMaster(
                                request.getCustomerPoid(),
                                groupPoid,
                                userId
                        ));

        // Update audit fields on every save
        master.setLastModifiedBy(userId);
        master.setLastModifiedDate(LocalDateTime.now());
        masterRepo.save(master);

        for (CustomerInvoiceChargeMapDetailDto dto : request.getDetails()) {
            saveOrUpdateDetail(request.getCustomerPoid(), dto);
        }
    }

    // ========================= DELETE DETAIL =========================

    @Override
    public void deleteDetail(
            Long customerPoid,
            Long detRowId,
            Long groupPoid,
            String userId) {

        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);

        CustomerInvoicePrtDtlEntity entity =
                detailRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Charge detail not found"));

        // HARD DELETE (as per SRS)
        detailRepo.delete(entity);
    }

    // ========================= PRIVATE METHODS =========================

    private CustomerInvoicePrtMasterEntity createMaster(
            Long customerPoid,
            Long groupPoid,
            String userId) {

        CustomerInvoicePrtMasterEntity master =
                new CustomerInvoicePrtMasterEntity();

        master.setCustomerPoid(customerPoid);
        master.setGroupPoid(groupPoid);
        master.setActive("Y");
        master.setDeleted("N");
        master.setCreatedBy(userId);
        master.setCreatedDate(LocalDateTime.now());
        master.setLastModifiedBy(userId);
        master.setLastModifiedDate(LocalDateTime.now());

        return masterRepo.save(master);
    }

    private void saveOrUpdateDetail(
            Long customerPoid,
            CustomerInvoiceChargeMapDetailDto dto) {

        Long detRowId = dto.getDetRowId();
        if (detRowId == null) {
            Long maxId = detailRepo.findMaxDetRowId(customerPoid);
            detRowId = (maxId == null ? 1 : maxId + 1);
        }

        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);

        CustomerInvoicePrtDtlEntity entity =
                detailRepo.findById(id)
                        .orElse(new CustomerInvoicePrtDtlEntity());

        entity.setId(id);
        entity.setChargePoid(dto.getChargePoid());
        entity.setLineChargeDescription(dto.getLineChargeDescription());
        entity.setValidUntil(dto.getValidUntil());
        entity.setLastModifiedDate(LocalDateTime.now());

        detailRepo.save(entity);
    }

    private CustomerInvoiceChargeMapDetailDto mapToDetailDto(
            CustomerInvoicePrtDtlEntity entity) {

        CustomerInvoiceChargeMapDetailDto dto =
                new CustomerInvoiceChargeMapDetailDto();

        dto.setDetRowId(entity.getId().getDetRowId());
        dto.setChargePoid(entity.getChargePoid());
        dto.setLineChargeDescription(entity.getLineChargeDescription());
        dto.setValidUntil(entity.getValidUntil());

        return dto;
    }
}
