package com.asg.shipping.customerinvoicechargemapmaster.service.impl;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerInvoiceChargeMapMasterServiceImpl
        implements CustomerInvoiceChargeMapMasterService {

    private final CustomerInvoicePrtMasterRepository masterRepo;
    private final CustomerInvoicePrtDtlRepository detailRepo;
    private final LoggingService loggingService;

    // ========================= GET =========================

    @Override
    @Transactional(readOnly = true)
    public CustomerInvoiceChargeMapMasterResponse getByCustomer(
            Long customerPoid,
            Long groupPoid) {

        log.info("Getting customer invoice charge mapping with customerPoid: {}", customerPoid);

        masterRepo.findById(customerPoid)
                .filter(m -> !"Y".equals(m.getDeleted()))
                .orElseThrow(() ->
                        new com.asg.common.lib.exception.ResourceNotFoundException(
                                "Customer invoice charge mapping", "customerPoid", customerPoid.toString()
                        ));

        List<CustomerInvoicePrtDtlEntity> details =
                detailRepo.findByIdCustomerPoid(customerPoid);

        if (details.isEmpty()) {
            throw new com.asg.common.lib.exception.ResourceNotFoundException(
                    "Customer invoice charge mapping", "customerPoid", customerPoid.toString()
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
        log.info("Successfully retrieved customer invoice charge mapping with customerPoid: {}", customerPoid);
        return response;
    }

    // ========================= SAVE / UPDATE =========================

    @Override
    public void saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid) {

        log.info("Saving/updating customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one charge mapping detail is required");
        }

        boolean isNewRecord = !masterRepo.existsById(request.getCustomerPoid());

        CustomerInvoicePrtMasterEntity master =
                masterRepo
                        .findById(request.getCustomerPoid())
                        .orElseGet(() -> createMaster(request.getCustomerPoid(), groupPoid));
        log.debug("Master record ensured for customerPoid: {}", master.getCustomerPoid());


        if (isNewRecord) {
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), request.getCustomerPoid().toString());
        } else {
            loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), request.getCustomerPoid().toString());
        }

        for (CustomerInvoiceChargeMapDetailDto dto : request.getDetails()) {
            saveOrUpdateDetail(request.getCustomerPoid(), dto);
        }

        log.info("Successfully saved/updated customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());
    }

    // ========================= DELETE DETAIL =========================

    @Override
    public void deleteDetail(
            Long customerPoid,
            Long detRowId,
            Long groupPoid) {

        log.info("Deleting customer invoice charge detail with customerPoid: {}, detRowId: {}", customerPoid, detRowId);

        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);

        CustomerInvoicePrtDtlEntity entity =
                detailRepo.findById(id)
                        .orElseThrow(() ->
                                new com.asg.common.lib.exception.ResourceNotFoundException(
                                        "Charge detail", "detRowId", detRowId.toString()));

        // HARD DELETE (as per SRS)
        detailRepo.delete(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), customerPoid.toString());
        String logDetail = String.format("KeyId = CUSTOMER_POID:%s, DET_ROW_ID:%s", customerPoid, detRowId);
        String tableName = CustomerInvoicePrtDtlEntity.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), customerPoid.toString(), "Detail Deleted", "EXISTS", "DELETED", logDetail, tableName);

        log.info("Successfully deleted customer invoice charge detail with customerPoid: {}, detRowId: {}", customerPoid, detRowId);
    }

    // ========================= PRIVATE METHODS =========================

    private CustomerInvoicePrtMasterEntity createMaster(
            Long customerPoid,
            Long groupPoid) {

        CustomerInvoicePrtMasterEntity master =
                new CustomerInvoicePrtMasterEntity();

        master.setCustomerPoid(customerPoid);
        master.setGroupPoid(groupPoid);
        master.setActive("Y");
        master.setDeleted("N");

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
