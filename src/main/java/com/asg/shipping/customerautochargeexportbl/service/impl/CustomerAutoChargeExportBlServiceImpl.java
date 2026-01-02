package com.asg.shipping.customerautochargeexportbl.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
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

    private final DocumentSearchService documentSearchService;
    private final ShipCustomerChargesHdrRepository headerRepository;
    private final ShipCustomerChargesDtlRepository detailRepository;
    private final CustomerAutoChargeExportBLMapper mapper;
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

        return getCustomerAutoChargeExportBL(entity.getTransactionPoid());
    }

    @Override
    @Transactional
    public CustomerAutoChargeExportBLDto updateCustomerAutoChargeExportBL(Long id, CustomerAutoChargeExportBLUpdateDTO updateDTO) {
        log.info("Updating customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity entity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", id.toString()));


        var periodFrom = updateDTO.getPeriodFrom() != null
                ? updateDTO.getPeriodFrom()
                : mapper.toLocalDate(entity.getPeriodFrom());

        var periodTo = updateDTO.getPeriodTo() != null
                ? updateDTO.getPeriodTo()
                : mapper.toLocalDate(entity.getPeriodTo());
        validatePeriodDates(periodFrom, periodTo);

        mapper.mapUpdateDTOToEntity(updateDTO, entity);
        entity = headerRepository.save(entity);
        log.info("Customer auto charge export BL updated with id: {}", id);

        detailRepository.deleteByTransactionPoid(id);
        if (updateDTO.getChargeDetails() != null && !updateDTO.getChargeDetails().isEmpty()) {
            saveDetailRecords(entity.getTransactionPoid(), updateDTO.getChargeDetails());
        }


        return getCustomerAutoChargeExportBL(entity.getTransactionPoid());
    }

    @Override
    @Transactional
    public void deleteCustomerAutoChargeExportBL(Long id) {
        log.info("Deleting customer auto charge export BL with id: {}", id);

        ShipCustomerChargesHdrEntity entity = headerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Auto Charge Export BL", "transactionPoid", id.toString()));

        entity.setDeleted("Y");
        headerRepository.save(entity);
        log.info("Customer auto charge export BL deleted with id: {}", id);
    }



    private void validatePeriodDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Period From and Period To must not be null");
        }

        if (from.isAfter(to)) {
            throw new ValidationException("Period From cannot be after Period To");
        }
    }


    private void saveDetailRecords(Long transactionPoid, List<CustomerAutoChargeDetailDto> detailDtos) {
        if (detailDtos != null && !detailDtos.isEmpty()) {
            Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
            for (CustomerAutoChargeDetailDto dto : detailDtos) {
                ShipCustomerChargesDtlEntity entity = mapper.mapDtlFromDto(dto, transactionPoid, null);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                detailRepository.save(entity);
            }
        }
    }
}
