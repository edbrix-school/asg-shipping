package com.asg.shipping.customerautochargeexportbl.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.service.LovService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeDetailDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesDtlEntity;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesHdrEntity;
import com.asg.shipping.customerautochargeexportbl.repository.ShipCustomerChargesDtlRepository;
import com.asg.shipping.customerautochargeexportbl.repository.ShipCustomerChargesHdrRepository;
import com.asg.shipping.customerautochargeexportbl.service.CustomerAutoChargeExportBlService;
import com.asg.shipping.customerautochargeexportbl.util.CustomerAutoChargeExportBLMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerAutoChargeExportBlServiceImpl implements CustomerAutoChargeExportBlService {

    private static final String ACTION_NOCHANGES = "NOCHANGES";
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";

    private static final String SCREEN_NAME = "Customer Auto Charge Export BL";
    private static final String FIELD_TRANSACTION_POID = "transactionPoid";
    private static final String COL_TRANSACTION_POID = "TRANSACTION_POID";
    private static final String TABLE_HDR = "SHIP_CUSTOMER_CHARGES_HDR";
    private static final String LOG_ROW_CREATED = "Row Created on Customer Charge Detail with DetRowId: %s";
    private static final String LOG_ROW_DELETED = "Row Deleted on Customer Charge Detail with DetRowId: %s";
    private static final String LOG_KEY_ID = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";
    private static final String ERR_PERIOD_NULL = "Period From and Period To must not be null";
    private static final String ERR_PERIOD_ORDER = "To Date should be after From date";
    private static final String ERR_PERIOD_OVERLAP = "Period selected is overlapping with some existing period, please check...";
    private static final String ERR_DET_ROW_ID_NULL = "Customer Charge Detail DetRowId is null";

    private final DocumentSearchService documentSearchService;
    private final ShipCustomerChargesHdrRepository headerRepository;
    private final ShipCustomerChargesDtlRepository detailRepository;
    private final CustomerAutoChargeExportBLMapper mapper;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final LovService lovService;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public Map<String, Object> list(FilterRequestDto filters, Pageable pageable,LocalDate startDate, LocalDate endDate) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);
        RawSearchResult raw = documentSearchService.search(
                UserContext.getDocumentId(),
                filterList,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",
                COL_TRANSACTION_POID
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto getCustomerAutoChargeExportBL(Long id) {
        log.info("Getting customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity entity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SCREEN_NAME, FIELD_TRANSACTION_POID, id.toString()));


        List<ShipCustomerChargesDtlEntity> detailRecords = detailRepository.findByTransactionPoidOrderByDetRowId(id);

        CustomerAutoChargeExportBLDto dto = mapper.mapToDto(entity);
        dto.setChargeDetails(mapper.mapDtlListToDto(detailRecords));
        enrichLovData(dto);

        return dto;
    }

    private void enrichLovData(CustomerAutoChargeExportBLDto dto) {
        if (dto.getChargeDetails() == null || dto.getChargeDetails().isEmpty()) {
            return;
        }
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        for (CustomerAutoChargeDetailDto detail : dto.getChargeDetails()) {
            try {
                if (detail.getChargeCodePoid() != null) {
                    detail.setChargeCodeDet(lovService.getLovItemByPoid(detail.getChargeCodePoid(), "CHARGE_MASTER_ALL", groupPoid, companyPoid, userPoid));
                }
                if (detail.getType() != null) {
                    detail.setTypeDet(lovService.getLovItemByCode(detail.getType(), "CHARGE_REVENU_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (detail.getImcoClassType() != null) {
                    detail.setImcoClassTypeDet(lovService.getLovItemByCode(detail.getImcoClassType(), "IMCO_CLASS", groupPoid, companyPoid, userPoid));
                }
                if (detail.getOthersType() != null) {
                    detail.setOthersTypeDet(lovService.getLovItemByCode(detail.getOthersType(), "OTHERS_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (detail.getOogType() != null) {
                    detail.setOogTypeDet(lovService.getLovItemByCode(detail.getOogType(), "OOG_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (detail.getChargeApplicable() != null) {
                    detail.setChargeApplicableDet(lovService.getLovItemByCode(detail.getChargeApplicable(), "CHARGE_APPLICABLE", groupPoid, companyPoid, userPoid));
                }
                if (detail.getCurrencyCode() != null) {
                    detail.setCurrencyCodeDet(lovService.getLovItemByCode(detail.getCurrencyCode(), "CURRENCY", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for charge detail with detRowId: {}", detail.getDetRowId(), e);
            }
        }
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto createCustomerAutoChargeExportBL(CustomerAutoChargeExportBLCreateDTO createDTO) {
        log.info("Creating customer auto charge export BL");

        validatePeriodDates(createDTO.getPeriodFrom(), createDTO.getPeriodTo());
        validateDateOverlap(createDTO.getCustomerPoid(), createDTO.getPeriodFrom(), createDTO.getPeriodTo(), null);

        ShipCustomerChargesHdrEntity entity = ShipCustomerChargesHdrEntity.builder().build();
        mapper.mapCreateDTOToEntity(createDTO, entity, UserContext.getGroupPoid());

        entity = headerRepository.saveAndFlush(entity);
        entityManager.refresh(entity);
        log.info("Customer auto charge export BL created with id: {}", entity.getTransactionPoid());

        if (createDTO.getChargeDetails() != null && !createDTO.getChargeDetails().isEmpty()) {
            saveDetailRecords(entity.getTransactionPoid(), createDTO.getChargeDetails());
        }
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), entity.getTransactionPoid().toString(), String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), entity.getDocRef()));
        return getCustomerAutoChargeExportBL(entity.getTransactionPoid());
    }

    private void saveDetailRecords(Long transactionPoid, List<CustomerAutoChargeDetailDto> detailDtos) {
        if (detailDtos != null && !detailDtos.isEmpty()) {
            Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
            for (CustomerAutoChargeDetailDto dto : detailDtos) {
                ShipCustomerChargesDtlEntity entity = mapper.mapDtlFromDto(dto, transactionPoid, null);

                  entity.setDetRowId(maxDetRowId != null ? ++maxDetRowId : 1L);
                detailRepository.save(entity);
                String logDetail = String.format(LOG_ROW_CREATED, entity.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto updateCustomerAutoChargeExportBL(Long id, CustomerAutoChargeExportBLUpdateDTO updateDTO) {
        log.info("Updating customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity existingEntity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SCREEN_NAME, FIELD_TRANSACTION_POID, id.toString()));

        ShipCustomerChargesHdrEntity oldEntity = new ShipCustomerChargesHdrEntity();
        BeanUtils.copyProperties(existingEntity,oldEntity);

        var periodFrom = updateDTO.getPeriodFrom() != null
                ? updateDTO.getPeriodFrom()
                : existingEntity.getPeriodFrom();

        var periodTo = updateDTO.getPeriodTo() != null
                ? updateDTO.getPeriodTo()
                : existingEntity.getPeriodTo();
        validatePeriodDates(periodFrom, periodTo);
        Long customerPoid = updateDTO.getCustomerPoid() != null ? updateDTO.getCustomerPoid() : existingEntity.getCustomerPoid();
        validateDateOverlap(customerPoid, periodFrom, periodTo, id);

        mapper.mapUpdateDTOToEntity(updateDTO, existingEntity);
        existingEntity = headerRepository.save(existingEntity);

        log.info("Customer auto charge export BL updated with id: {}", id);

        loggingService.logChanges(oldEntity,existingEntity, ShipCustomerChargesHdrEntity.class,UserContext.getDocumentId(),id.toString(), LogDetailsEnum.MODIFIED,COL_TRANSACTION_POID);

        if (updateDTO.getChargeDetails() != null && !updateDTO.getChargeDetails().isEmpty()) {
            updateDetailRecords(existingEntity.getTransactionPoid(), updateDTO.getChargeDetails());
        }

        return getCustomerAutoChargeExportBL(existingEntity.getTransactionPoid());
    }

    @Override
    @Transactional
    public void deleteCustomerAutoChargeExportBL(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity entity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SCREEN_NAME, FIELD_TRANSACTION_POID, id.toString()));

        documentDeleteService.deleteDocument(id, TABLE_HDR, COL_TRANSACTION_POID, deleteReasonDto, entity.getTransactionDate());

    }



    private void validatePeriodDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException(ERR_PERIOD_NULL);
        }
        if (from.isAfter(to)) {
            throw new ValidationException(ERR_PERIOD_ORDER);
        }
    }

    private void validateDateOverlap(Long customerPoid, LocalDate periodFrom, LocalDate periodTo, Long excludeId) {
        if (headerRepository.countOverlappingPeriod(customerPoid, periodFrom, periodTo, excludeId) > 0) {
            throw new ValidationException(ERR_PERIOD_OVERLAP);
        }
    }


    private void updateDetailRecords(Long transactionPoid, List<CustomerAutoChargeDetailDto> detailDtos) {

        List<ShipCustomerChargesDtlEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipCustomerChargesDtlEntity>> logRequests = new ArrayList<>();
        for (CustomerAutoChargeDetailDto dto : detailDtos) {
            String action = resolveAction(dto.getActionType());
            switch (action) {
                case ACTION_NOCHANGES -> {
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        detailRepository.deleteByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId());
                        String logDetail = String.format(LOG_ROW_DELETED, dto.getDetRowId());
                        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                    }
                    else {
                        throw new ValidationException(ERR_DET_ROW_ID_NULL);
                    }
                }
                case ACTION_ISCREATED -> {
                    Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
                    long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : (maxDetRowId != null ? maxDetRowId + 1 : 1L);
                    ShipCustomerChargesDtlEntity entity = mapper.mapDtlFromDto(dto, transactionPoid, detRowId);
                    detailRepository.save(entity);
                    String logDetail = String.format(LOG_ROW_CREATED, entity.getDetRowId());
                    loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() != null) {
                        ShipCustomerChargesDtlEntity entity = detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .orElseThrow(() -> new ResourceNotFoundException("Customer Charge Detail", "DetRowId", dto.getDetRowId().toString()));
                        ShipCustomerChargesDtlEntity oldEntity = new ShipCustomerChargesDtlEntity();
                        BeanUtils.copyProperties(entity,oldEntity);
                        mapper.updateDtlEntity(dto, entity);
                        toUpdate.add(entity);
                        String logDetail = String.format(LOG_KEY_ID, transactionPoid, dto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, entity, ShipCustomerChargesDtlEntity.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
                    }
                    else {
                        throw new ValidationException(ERR_DET_ROW_ID_NULL);
                    }
                }
            }
        }
        if (!toUpdate.isEmpty()) {
            detailRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private String resolveAction(String rawAction) {
        String action = (rawAction == null || rawAction.trim().isEmpty()) ? ACTION_NOCHANGES : rawAction.trim().toUpperCase();
        return switch (action) {
            case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }
}
