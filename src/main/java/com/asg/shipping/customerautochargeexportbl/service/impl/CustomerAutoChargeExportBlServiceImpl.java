package com.asg.shipping.customerautochargeexportbl.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;


@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerAutoChargeExportBlServiceImpl implements CustomerAutoChargeExportBlService {

    private static final String ACTION_NOCHANGES = "NOCHANGES";
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";

    private final DocumentSearchService documentSearchService;
    private final ShipCustomerChargesHdrRepository headerRepository;
    private final ShipCustomerChargesDtlRepository detailRepository;
    private final CustomerAutoChargeExportBLMapper mapper;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    @Override
    public Map<String, Object> list(FilterRequestDto filters, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveFilters(filters);

        RawSearchResult raw = documentSearchService.search(
                UserContext.getDocumentId(),
                filterList,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto getCustomerAutoChargeExportBL(Long id) {
        log.info("Getting customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity entity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", id.toString()));


        List<ShipCustomerChargesDtlEntity> detailRecords = detailRepository.findByTransactionPoidOrderByDetRowId(id);

        CustomerAutoChargeExportBLDto dto = mapper.mapToDto(entity);
        dto.setChargeDetails(mapper.mapDtlListToDto(detailRecords));

        return dto;
    }

    @Override
    public CustomerAutoChargeExportBLDto createCustomerAutoChargeExportBL(CustomerAutoChargeExportBLCreateDTO createDTO) {
        log.info("Creating customer auto charge export BL");

        validatePeriodDates(createDTO.getPeriodFrom(), createDTO.getPeriodTo());

        ShipCustomerChargesHdrEntity entity = ShipCustomerChargesHdrEntity.builder().build();
        mapper.mapCreateDTOToEntity(createDTO, entity, UserContext.getGroupPoid());

        entity = headerRepository.save(entity);
        log.info("Customer auto charge export BL created with id: {}", entity.getTransactionPoid());

        if (createDTO.getChargeDetails() != null && !createDTO.getChargeDetails().isEmpty()) {
            saveDetailRecords(entity.getTransactionPoid(), createDTO.getChargeDetails());
        }
       loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED,UserContext.getDocumentId(),entity.getTransactionPoid().toString());
        return getCustomerAutoChargeExportBL(entity.getTransactionPoid());
    }

    private void saveDetailRecords(Long transactionPoid, List<CustomerAutoChargeDetailDto> detailDtos) {
        if (detailDtos != null && !detailDtos.isEmpty()) {
            Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
            for (CustomerAutoChargeDetailDto dto : detailDtos) {
                ShipCustomerChargesDtlEntity entity = mapper.mapDtlFromDto(dto, transactionPoid, null);

                  entity.setDetRowId(maxDetRowId != null ? ++maxDetRowId : 1L);
                detailRepository.save(entity);
                String logDetail = String.format("Row Created on Customer Charge Detail with DetRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto updateCustomerAutoChargeExportBL(Long id, CustomerAutoChargeExportBLUpdateDTO updateDTO) {
        log.info("Updating customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity existingEntity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", id.toString()));

        ShipCustomerChargesHdrEntity oldEntity = new ShipCustomerChargesHdrEntity();
        BeanUtils.copyProperties(existingEntity,oldEntity);

        var periodFrom = updateDTO.getPeriodFrom() != null
                ? updateDTO.getPeriodFrom()
                : mapper.toLocalDate(existingEntity.getPeriodFrom());

        var periodTo = updateDTO.getPeriodTo() != null
                ? updateDTO.getPeriodTo()
                : mapper.toLocalDate(existingEntity.getPeriodTo());
        validatePeriodDates(periodFrom, periodTo);

        mapper.mapUpdateDTOToEntity(updateDTO, existingEntity);
        existingEntity = headerRepository.save(existingEntity);

        log.info("Customer auto charge export BL updated with id: {}", id);

        loggingService.logChanges(oldEntity,existingEntity, ShipCustomerChargesHdrEntity.class,UserContext.getDocumentId(),id.toString(), LogDetailsEnum.MODIFIED,"TRANSACTION_POID");

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
                .orElseThrow(() -> new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", id.toString()));

        documentDeleteService.deleteDocument(id,"SHIP_CUSTOMER_CHARGES_HDR","TRANSACTION_POID"
        ,deleteReasonDto,entity.getTransactionDate());

    }



    private void validatePeriodDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Period From and Period To must not be null");
        }

        if (from.isAfter(to)) {
            throw new ValidationException("Period From cannot be after Period To");
        }
    }


    private void updateDetailRecords(Long transactionPoid, List<CustomerAutoChargeDetailDto> detailDtos) {
        for (CustomerAutoChargeDetailDto dto : detailDtos) {
            String action = resolveAction(dto.getActionType());
            switch (action) {
                case ACTION_NOCHANGES -> {
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        detailRepository.deleteByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId());
                        String logDetail = String.format("Row Deleted on Customer Charge Detail with DetRowId: %s", dto.getDetRowId());
                        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                    }
                    else {
                        throw new ValidationException("Customer Charge Detail DetRowId is null");
                    }
                }
                case ACTION_ISCREATED -> {
                    Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
                    long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : (maxDetRowId != null ? maxDetRowId + 1 : 1L);
                    ShipCustomerChargesDtlEntity entity = mapper.mapDtlFromDto(dto, transactionPoid, detRowId);
                    detailRepository.save(entity);
                    String logDetail = String.format("Row Created on Customer Charge Detail with DetRowId: %s", entity.getDetRowId());
                    loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() != null) {
                        ShipCustomerChargesDtlEntity entity = detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .orElseThrow(() -> new ResourceNotFoundException("Customer Charge Detail", "DetRowId", dto.getDetRowId().toString()));
                        mapper.updateDtlEntity(dto, entity);
                        detailRepository.save(entity);
                        String logDetail = String.format("Row Updated on Customer Charge Detail with DetRowId: %s", entity.getDetRowId());
                        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                    }
                    else {
                        throw new ValidationException("Customer Charge Detail  DetRowId is null");
                    }
                }
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
