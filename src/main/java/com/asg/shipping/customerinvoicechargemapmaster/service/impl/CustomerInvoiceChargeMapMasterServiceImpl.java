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

import java.time.LocalDateTime;
import java.util.List;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

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
        log.info("Successfully retrieved customer invoice charge mapping with customerPoid: {}", customerPoid);
        return response;
    }

    // ========================= SAVE / UPDATE =========================

    @Override
    public void saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid,
            String userId) {

        log.info("Saving/updating customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one charge mapping detail is required");
        }

        boolean isNewRecord = !masterRepo.existsById(request.getCustomerPoid());
        
        CustomerInvoicePrtMasterEntity master =
                masterRepo.findById(request.getCustomerPoid())
                        .orElseGet(() -> createMaster(
                                request.getCustomerPoid(),
                                groupPoid,
                                userId
                        ));

        // Update audit fields on every save
        master.setLastModifiedBy(getCurrentUser());
        master.setLastModifiedDate(LocalDateTime.now());
        masterRepo.save(master);

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
            Long groupPoid,
            String userId) {

        log.info("Deleting customer invoice charge detail with customerPoid: {}, detRowId: {}", customerPoid, detRowId);

        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);

        CustomerInvoicePrtDtlEntity entity =
                detailRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Charge detail not found"));

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
            Long groupPoid,
            String userId) {

        CustomerInvoicePrtMasterEntity master =
                new CustomerInvoicePrtMasterEntity();

        master.setCustomerPoid(customerPoid);
        master.setGroupPoid(groupPoid);
        master.setActive("Y");
        master.setDeleted("N");
        master.setCreatedBy(getCurrentUser());
        master.setCreatedDate(LocalDateTime.now());
        master.setLastModifiedBy(getCurrentUser());
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
        entity.setLastModifiedBy(getCurrentUser());
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
