package com.asg.shipping.customerinvoicechargemapmaster.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerInvoiceChargeMapMasterServiceImpl
        implements CustomerInvoiceChargeMapMasterService {

    private final CustomerInvoicePrtMasterRepository masterRepo;
    private final CustomerInvoicePrtDtlRepository detailRepo;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;

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
    public CustomerInvoiceChargeMapMasterResponse saveOrUpdate(
            CustomerInvoiceChargeMapMasterRequest request,
            Long groupPoid) {

        log.info("Saving/updating customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one charge mapping detail is required");
        }

        // If customer was changed, delete old master + details first
        if (request.getOldCustomerPoid() != null
                && !request.getOldCustomerPoid().equals(request.getCustomerPoid())) {
            detailRepo.deleteAll(detailRepo.findByIdCustomerPoid(request.getOldCustomerPoid()));
            masterRepo.findById(request.getOldCustomerPoid()).ifPresent(masterRepo::delete);
        }

        boolean isNewRecord = !masterRepo.existsById(request.getCustomerPoid());

        CustomerInvoicePrtMasterEntity master =
                masterRepo
                        .findById(request.getCustomerPoid())
                        .orElseGet(() -> createMaster(request.getCustomerPoid(), groupPoid));
        log.debug("Master record ensured for customerPoid: {}", master.getCustomerPoid());

        String docId = UserContext.getDocumentId();
        String key = request.getCustomerPoid().toString();

        if (isNewRecord) {
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        } else {
            CustomerInvoicePrtMasterEntity oldMaster = new CustomerInvoicePrtMasterEntity();
            BeanUtils.copyProperties(master, oldMaster);
            loggingService.logChanges(oldMaster, master, CustomerInvoicePrtMasterEntity.class, docId, key,
                    LogDetailsEnum.MODIFIED, "CUSTOMER_POID");
        }

        for (CustomerInvoiceChargeMapDetailDto dto : request.getDetails()) {
            saveOrUpdateDetail(request.getCustomerPoid(), dto, docId, key);
        }
        log.info("Successfully saved/updated customer invoice charge mapping for customerPoid: {}", request.getCustomerPoid());
        return getByCustomer(request.getCustomerPoid(), groupPoid);

}

    // ========================= DELETE DETAIL =========================

    @Override
    public void deleteDetail(
            Long customerPoid,
            DeleteReasonDto deleteReasonDto) {

        log.info("Deleting customer invoice charge mapping for customerPoid: {}", customerPoid);

        masterRepo.findById(customerPoid)
                .orElseThrow(() -> new com.asg.common.lib.exception.ResourceNotFoundException(
                        "Customer invoice charge mapping", "customerPoid", customerPoid.toString()));

        documentDeleteService.deleteDocument(
                customerPoid,
                "CUSTOMER_INVOICE_PRT_MASTER",
                "CUSTOMER_POID",
                deleteReasonDto,
                null
        );

        log.info("Successfully deleted customer invoice charge mapping for customerPoid: {}", customerPoid);
    }

    // ========================= LIST =========================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "CUSTOMER_POID",
                "CUSTOMER_POID");

        org.springframework.data.domain.Page<Map<String, Object>> page =
                new org.springframework.data.domain.PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
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
            CustomerInvoiceChargeMapDetailDto dto,
            String docId,
            String key) {

        Long detRowId = dto.getDetRowId();
        if (detRowId == null) {
            Long maxId = detailRepo.findMaxDetRowId(customerPoid);
            detRowId = (maxId == null ? 1 : maxId + 1);
        }

        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);

        String actionType = dto.getActionType() != null ? dto.getActionType().trim().toUpperCase() : "";

        if (actionType.contains("DELETE")) {
            detailRepo.findById(id).ifPresent(existing -> {
                detailRepo.delete(existing);
                loggingService.logChanges(existing, null, CustomerInvoicePrtDtlEntity.class, docId, key,
                        LogDetailsEnum.DELETED, "CUSTOMER_POID");
            });
            return;
        }

        if (actionType.contains("NOCHANGE") || actionType.contains("NO_CHANGE")) {
            return;
        }

        CustomerInvoicePrtDtlEntity existing = detailRepo.findById(id).orElse(null);
        boolean isNewDetail = existing == null;

        CustomerInvoicePrtDtlEntity entity = isNewDetail ? new CustomerInvoicePrtDtlEntity() : existing;

        CustomerInvoicePrtDtlEntity oldDetail = new CustomerInvoicePrtDtlEntity();
        if (!isNewDetail) {
            BeanUtils.copyProperties(existing, oldDetail);
        }

        entity.setId(id);
        entity.setChargePoid(dto.getChargePoid());
        entity.setLineChargeDescription(dto.getLineChargeDescription());
        entity.setValidUntil(dto.getValidUntil());

        detailRepo.save(entity);

        if (isNewDetail) {
            loggingService.createLogSummaryEntry(docId, key,
                    String.format("Row Created on Charge Detail with detRowId: %s", id.getDetRowId()));
        } else {
            loggingService.logChanges(oldDetail, entity, CustomerInvoicePrtDtlEntity.class, docId, key,
                    LogDetailsEnum.MODIFIED, "CUSTOMER_POID");
        }
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
