package com.asg.shipping.containertypeportchargestariff.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesDtl;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesHdr;
import com.asg.shipping.containertypeportchargestariff.repository.PortChargesTariffCustomRepository;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesDtlRepository;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesHdrRepository;
import com.asg.shipping.exceptions.ResourceAlreadyExistsException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.portMaster.repository.PortMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortChargesTariffServiceImpl implements PortChargesTariffService {

    private final DocumentSearchService documentService;
    private final ShipPortChargesHdrRepository hdrRepository;
    private final ShipPortChargesDtlRepository dtlRepository;
    private final PortMasterRepository portMasterRepository;
    private final ShipLineMasterThirdPartyRepository lineMasterThirdPartyRepository;
    private final PortChargesTariffCustomRepository customRepository;

    @Override
    @Transactional
    public Map<String, Object> listPortChargesTariff(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "DESCRIPTION", "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public PortChargesTariffDto getPortChargesTariff(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();

        ShipPortChargesHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", transactionPoid.toString()));

        if (!groupPoid.equals(hdr.getGroupPoid())) {
            throw new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", transactionPoid.toString());
        }

        List<ShipPortChargesDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        return mapToDto(hdr, details);
    }

    @Override
    @Transactional
    public PortChargesTariffDto createPortChargesTariff(PortChargesTariffCreateDto dto, Long groupPoid, Long userPoid) {
        validateCreateRequest(dto);
        validateDateOverlap(dto.getPortPoid(), dto.getChargeLinePoid(), dto.getChargeDivision(), dto.getPeriodFrom(), dto.getPeriodTo(), null, groupPoid);

        ShipPortChargesHdr hdr = new ShipPortChargesHdr();
        hdr.setTransactionDate(LocalDate.now());
        hdr.setGroupPoid(groupPoid);
        hdr.setPortPoid(dto.getPortPoid());
        hdr.setDescription(dto.getDescription());
        hdr.setPeriodFrom(dto.getPeriodFrom());
        hdr.setPeriodTo(dto.getPeriodTo());
        hdr.setDocRef(dto.getDocRef());
        hdr.setSeqNo(dto.getSeqNo());
        hdr.setChargeLinePoid(dto.getChargeLinePoid());
        hdr.setChargeDivision(dto.getChargeDivision());
        hdr.setCreatedBy(getCurrentUser());
        hdr.setCreatedDate(LocalDateTime.now());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());
        hdr.setDeleted("N");

        ShipPortChargesHdr savedHdr = hdrRepository.save(hdr);
        hdrRepository.flush();

        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            processCreateDetails(savedHdr.getTransactionPoid(), dto.getDetails());
        }

        return getPortChargesTariff(savedHdr.getTransactionPoid());
    }

    @Override
    @Transactional
    public PortChargesTariffDto updatePortChargesTariff(Long id, PortChargesTariffUpdateDto dto, Long groupPoid, Long userPoid) {
        ShipPortChargesHdr hdr = hdrRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", id.toString()));

        if (!groupPoid.equals(hdr.getGroupPoid())) {
            throw new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", id.toString());
        }

        validateUpdateRequest(dto, id);

        // 🔑 capture old values BEFORE mutation
        LocalDate oldFrom = hdr.getPeriodFrom();
        LocalDate oldTo = hdr.getPeriodTo();

        hdr.setPortPoid(dto.getPortPoid());
        hdr.setDescription(dto.getDescription());
        hdr.setPeriodFrom(dto.getPeriodFrom());
        hdr.setPeriodTo(dto.getPeriodTo());
        hdr.setDocRef(dto.getDocRef());
        hdr.setSeqNo(dto.getSeqNo());
        hdr.setChargeLinePoid(dto.getChargeLinePoid());
        hdr.setChargeDivision(dto.getChargeDivision());
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());

        // 🔑 flush so DB reflects new state
        hdrRepository.saveAndFlush(hdr);

        boolean periodChanged = !oldFrom.equals(dto.getPeriodFrom()) || !oldTo.equals(dto.getPeriodTo());

        if (periodChanged) {
            validateDateOverlap(dto.getPortPoid(), dto.getChargeLinePoid(), dto.getChargeDivision(), dto.getPeriodFrom(), dto.getPeriodTo(), id, groupPoid);
        }

        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            processDetails(id, dto.getDetails());
        }

        return getPortChargesTariff(hdr.getTransactionPoid());
    }


    @Override
    @Transactional
    public void deletePortChargesTariff(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();

        ShipPortChargesHdr hdr = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", transactionPoid.toString()));

        if (!groupPoid.equals(hdr.getGroupPoid())) {
            throw new ResourceNotFoundException("Port Charges Tariff", "transactionPoid", transactionPoid.toString());
        }

        if ("Y".equals(hdr.getDeleted())) {
            return;
        }

        hdr.setDeleted("Y");
        hdr.setLastModifiedBy(getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());
        hdrRepository.save(hdr);

        dtlRepository.deleteByTransactionPoid(transactionPoid);
    }

    @Override
    @Transactional
    public ValidateOverlapResponseDto validateOverlap(ValidateOverlapRequestDto request) {

        Long groupPoid = UserContext.getGroupPoid();

        // 1. Invalid date range → treat as overlap
        if (request.getPeriodFrom().isAfter(request.getPeriodTo())) {
            return ValidateOverlapResponseDto.builder()
                    .overlapping(true)
                    .conflicts(List.of())
                    .build();
        }

        // 2. Query DB directly for overlaps (exclude self if UPDATE)
        List<ShipPortChargesHdr> overlaps = hdrRepository.findOverlappingTariffs(groupPoid, request.getPortPoid(), request.getChargeLinePoid(), request.getChargeDivision(), request.getPeriodFrom(), request.getPeriodTo(), request.getTransactionPoid());

        // 3. Map conflicts for UI
        List<OverlapConflictDto> conflicts = overlaps.stream()
                .map(hdr -> OverlapConflictDto.builder()
                        .transactionPoid(hdr.getTransactionPoid())
                        .periodFrom(hdr.getPeriodFrom())
                        .periodTo(hdr.getPeriodTo())
                        .description(hdr.getDescription())
                        .build())
                .toList();

        // 4. Final response
        return ValidateOverlapResponseDto.builder()
                .overlapping(!overlaps.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    private void validateCreateRequest(PortChargesTariffCreateDto dto) {
        if (StringUtils.isNotBlank(dto.getDocRef()) && hdrRepository.existsByDocRefIgnoreCase(dto.getDocRef())) {
            throw new ResourceAlreadyExistsException("Doc Ref", dto.getDocRef());
        }
        if (dto.getPortPoid() != null) {
            if (!portMasterRepository.existsByPortPoidAndGroupPoid(dto.getPortPoid(), UserContext.getGroupPoid())) {
                throw new ResourceNotFoundException("Port Master", "portPoid", dto.getPortPoid().toString());
            }
        }
        if (dto.getChargeLinePoid() != null) {
            if (!lineMasterThirdPartyRepository.existsByLinePoid(dto.getChargeLinePoid())) {
                throw new ResourceNotFoundException("Line Master", "linePoid", dto.getChargeLinePoid().toString());
            }
        }
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null && dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
            throw new ValidationException("Period From must be before or equal to Period To");
        }
    }

    private void validateUpdateRequest(PortChargesTariffUpdateDto dto, Long transactionPoid) {
        if (StringUtils.isNotBlank(dto.getDocRef()) && hdrRepository.existsByDocRefIgnoreCaseAndTransactionPoidNot(dto.getDocRef(), transactionPoid)) {
            throw new ResourceAlreadyExistsException("Doc Ref", dto.getDocRef());
        }
        if (dto.getPortPoid() != null) {
            if (!portMasterRepository.existsByPortPoidAndGroupPoid(dto.getPortPoid(), UserContext.getGroupPoid())) {
                throw new ResourceNotFoundException("Port Master", "portPoid", dto.getPortPoid().toString());
            }
        }
        if (dto.getChargeLinePoid() != null) {
            if (!lineMasterThirdPartyRepository.existsByLinePoid(dto.getChargeLinePoid())) {
                throw new ResourceNotFoundException("Line Master", "linePoid", dto.getChargeLinePoid().toString());
            }
        }
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null && dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
            throw new ValidationException("Period From must be before or equal to Period To");
        }
    }

    private void validateDateOverlap(Long portPoid, Long chargeLinePoid, String chargeDivision, LocalDate periodFrom, LocalDate periodTo, Long excludeId, Long groupPoid) {
        List<ShipPortChargesHdr> overlaps = hdrRepository.findOverlappingTariffs(groupPoid, portPoid, chargeLinePoid, chargeDivision, periodFrom, periodTo, excludeId);

        if (!overlaps.isEmpty()) {
            ShipPortChargesHdr conflict = overlaps.get(0);
            throw new ValidationException("Period overlaps with existing tariff (Transaction ID: " + conflict.getTransactionPoid() + ")");
        }
    }

    private void processDetails(Long transactionPoid, List<PortChargesDetailUpdateDto> details) {
        List<ShipPortChargesDtl> entitiesToDelete = new ArrayList<>();
        List<ShipPortChargesDtl> entitiesToSave = new ArrayList<>();

        for (PortChargesDetailUpdateDto dto : details) {
            String action = dto.getActionType() != null ? dto.getActionType().toLowerCase() : "";

            switch (action) {
                case "isdeleted" -> handleDeleteAction(transactionPoid, dto, entitiesToDelete);
                case "iscreated", "isupdated" -> handleCreateOrUpdateAction(transactionPoid, dto, entitiesToSave);
            }
        }

        if (!entitiesToDelete.isEmpty()) dtlRepository.deleteAll(entitiesToDelete);
        if (!entitiesToSave.isEmpty()) dtlRepository.saveAll(entitiesToSave);
    }

    private void handleDeleteAction(Long transactionPoid, PortChargesDetailUpdateDto dto, List<ShipPortChargesDtl> entitiesToDelete) {
        if (dto.getDetRowId() != null) {
            dtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                    .ifPresent(entitiesToDelete::add);
        }
    }

    private void handleCreateOrUpdateAction(Long transactionPoid, PortChargesDetailUpdateDto dto, List<ShipPortChargesDtl> entitiesToSave) {
        if (dto.getDetRowId() != null) {
            dtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                    .ifPresentOrElse(
                            existingEntity -> updateExistingEntity(existingEntity, dto, entitiesToSave),
                            () -> createNewEntity(transactionPoid, dto, entitiesToSave)
                    );
        } else {
            createNewEntity(transactionPoid, dto, entitiesToSave);
        }
    }

    private void updateExistingEntity(ShipPortChargesDtl entity, PortChargesDetailUpdateDto dto, List<ShipPortChargesDtl> entitiesToSave) {
        entity.setChargeCodePoid(dto.getChargeCodePoid());
        entity.setChargeTypeApplicable(dto.getChargeTypeApplicable());
        entity.setChargeApplicable(dto.getChargeApplicable());
        entity.setImcoClassType(dto.getImcoClassType());
        entity.setOogType(dto.getOogType());
        entity.setOthersType(dto.getOthersType());
        entity.setAmount20(dto.getAmount20());
        entity.setAmount40(dto.getAmount40());
        entity.setAmount53(dto.getAmount53());
        entity.setAmountOther(dto.getAmountOther());
        entity.setAmount20Cost(dto.getAmount20Cost());
        entity.setAmount40Cost(dto.getAmount40Cost());
        entity.setAmount53Cost(dto.getAmount53Cost());
        entity.setAmountOtherCost(dto.getAmountOtherCost());
        entity.setShipChargeType(dto.getShipChargeType());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        entitiesToSave.add(entity);
    }

    private void createNewEntity(Long transactionPoid, PortChargesDetailUpdateDto dto, List<ShipPortChargesDtl> entitiesToSave) {
        ShipPortChargesDtl newEntity = new ShipPortChargesDtl();
        newEntity.setTransactionPoid(transactionPoid);
        newEntity.setDetRowId(dto.getDetRowId() != null ? dto.getDetRowId() : getNextDetRowId(transactionPoid));
        newEntity.setChargeCodePoid(dto.getChargeCodePoid());
        newEntity.setChargeTypeApplicable(dto.getChargeTypeApplicable());
        newEntity.setChargeApplicable(dto.getChargeApplicable());
        newEntity.setImcoClassType(dto.getImcoClassType());
        newEntity.setOogType(dto.getOogType());
        newEntity.setOthersType(dto.getOthersType());
        newEntity.setAmount20(dto.getAmount20());
        newEntity.setAmount40(dto.getAmount40());
        newEntity.setAmount53(dto.getAmount53());
        newEntity.setAmountOther(dto.getAmountOther());
        newEntity.setAmount20Cost(dto.getAmount20Cost());
        newEntity.setAmount40Cost(dto.getAmount40Cost());
        newEntity.setAmount53Cost(dto.getAmount53Cost());
        newEntity.setAmountOtherCost(dto.getAmountOtherCost());
        newEntity.setShipChargeType(dto.getShipChargeType());
        newEntity.setCreatedBy(getCurrentUser());
        newEntity.setCreatedDate(LocalDateTime.now());
        newEntity.setLastModifiedBy(getCurrentUser());
        newEntity.setLastModifiedDate(LocalDateTime.now());
        entitiesToSave.add(newEntity);
    }

    private void processCreateDetails(Long transactionPoid, List<PortChargesDetailCreateDto> details) {
        List<ShipPortChargesDtl> entitiesToSave = new ArrayList<>();

        for (int i = 0; i < details.size(); i++) {
            PortChargesDetailCreateDto dto = details.get(i);
            ShipPortChargesDtl entity = new ShipPortChargesDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(dto.getDetRowId() != null ? dto.getDetRowId() : (long) (i + 1));
            entity.setChargeCodePoid(dto.getChargeCodePoid());
            entity.setChargeTypeApplicable(dto.getChargeTypeApplicable());
            entity.setChargeApplicable(dto.getChargeApplicable());
            entity.setImcoClassType(dto.getImcoClassType());
            entity.setOogType(dto.getOogType());
            entity.setOthersType(dto.getOthersType());
            entity.setAmount20(dto.getAmount20());
            entity.setAmount40(dto.getAmount40());
            entity.setAmount53(dto.getAmount53());
            entity.setAmountOther(dto.getAmountOther());
            entity.setAmount20Cost(dto.getAmount20Cost());
            entity.setAmount40Cost(dto.getAmount40Cost());
            entity.setAmount53Cost(dto.getAmount53Cost());
            entity.setAmountOtherCost(dto.getAmountOtherCost());
            entity.setShipChargeType(dto.getShipChargeType());
            entity.setCreatedBy(getCurrentUser());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setLastModifiedBy(getCurrentUser());
            entity.setLastModifiedDate(LocalDateTime.now());
            entitiesToSave.add(entity);
        }

        dtlRepository.saveAll(entitiesToSave);
    }

    private Long getNextDetRowId(Long transactionPoid) {
        List<ShipPortChargesDtl> existing = dtlRepository.findByTransactionPoid(transactionPoid);
        return existing.stream()
                .mapToLong(ShipPortChargesDtl::getDetRowId)
                .max()
                .orElse(0L) + 1;
    }

    private PortChargesTariffDto mapToDto(ShipPortChargesHdr hdr, List<ShipPortChargesDtl> details) {
        List<PortChargesDetailDto> detailDtos = null;
        if (details != null) {
            detailDtos = details.stream()
                    .map(this::mapDetailToDto)
                    .collect(Collectors.toList());
        }

        return PortChargesTariffDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .transactionDate(hdr.getTransactionDate())
                .groupPoid(hdr.getGroupPoid())
                .portPoid(hdr.getPortPoid())
                .description(hdr.getDescription())
                .periodFrom(hdr.getPeriodFrom())
                .periodTo(hdr.getPeriodTo())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .deleted(hdr.getDeleted())
                .docRef(hdr.getDocRef())
                .seqNo(hdr.getSeqNo())
                .chargeLinePoid(hdr.getChargeLinePoid())
                .chargeDivision(hdr.getChargeDivision())
                .details(detailDtos)
                .build();
    }

    private PortChargesDetailDto mapDetailToDto(ShipPortChargesDtl dtl) {
        return PortChargesDetailDto.builder()
                .transactionPoid(dtl.getTransactionPoid())
                .detRowId(dtl.getDetRowId())
                .createdBy(dtl.getCreatedBy())
                .createdDate(dtl.getCreatedDate())
                .lastModifiedBy(dtl.getLastModifiedBy())
                .lastModifiedDate(dtl.getLastModifiedDate())
                .chargeCodePoid(dtl.getChargeCodePoid())
                .chargeTypeApplicable(dtl.getChargeTypeApplicable())
                .chargeApplicable(dtl.getChargeApplicable())
                .imcoClassType(dtl.getImcoClassType())
                .oogType(dtl.getOogType())
                .othersType(dtl.getOthersType())
                .amount20(dtl.getAmount20())
                .amount40(dtl.getAmount40())
                .amount53(dtl.getAmount53())
                .amountOther(dtl.getAmountOther())
                .amount20Cost(dtl.getAmount20Cost())
                .amount40Cost(dtl.getAmount40Cost())
                .amount53Cost(dtl.getAmount53Cost())
                .amountOtherCost(dtl.getAmountOtherCost())
                .shipChargeType(dtl.getShipChargeType())
                .build();
    }
}