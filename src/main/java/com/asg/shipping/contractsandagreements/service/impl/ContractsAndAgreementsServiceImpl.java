package com.asg.shipping.contractsandagreements.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementPicDtlDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementPicDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementRenewalDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementsHdrRepository;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsService;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsValidationService;
import com.asg.shipping.contractsandagreements.util.mapper.ContractsAndAgreementsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractsAndAgreementsServiceImpl implements ContractsAndAgreementsService {

    private final DocumentSearchService documentSearchService;
    private final AdminContractsAgreementsHdrRepository headerRepo;
    private final AdminContractsAgreementRenewalDtlRepository renewalDtlRepository;
    private final AdminContractsAgreementPicDtlRepository picDtlRepository;
    private final DocumentDeleteService documentDeleteService;
    private final ContractsAndAgreementsValidationService validationService;
    private final LoggingService loggingService;

    private static final String ACTION_NOCHANGES = "NOCHANGES";
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";


    @Override
    @Transactional
    public AdminContractsAgreementHdrDto createContractsAndAgreements(AdminContractsAgreementHdrDto dto) {

        if (validationService.checkForDuplicateAgreementName(dto.getAgreementName())) {
            throw new ValidationException(
                    "Agreement Name with " + dto.getAgreementName() + " already exists"
            );
        }

        validationService.partyValidation(dto.getPartyType(), dto.getPartyPoid());

        validationService.expiryDateValidation(
                dto.getExpiryDate(),
                dto.getEffectiveDate(),
                dto.getTerminationDate()
        );
        AdminContractsAgreementHdr entity = new AdminContractsAgreementHdr();
                ContractsAndAgreementsMapper.updateHdrEntity(dto,entity);

        entity.setCreatedBy(UserContext.getUserName());
        entity.setCreatedDate(LocalDateTime.now());

        AdminContractsAgreementHdr saved = headerRepo.saveAndFlush(entity);
        log.info("Created new Contracts and Agreements with ID: {}", saved.getTransactionPoid());

        saveAgreementContentDetails(dto.getAgreementContentDetails(), saved.getTransactionPoid());
        saveAgreementRenewalDetails(dto.getRenewalDetails(), saved.getTransactionPoid());

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.CREATED,
                UserContext.getDocumentId(),
                saved.getTransactionPoid().toString()
        );

        return getContractsAndAgreementsById(saved.getTransactionPoid());
    }


    @Override
    public AdminContractsAgreementHdrDto getContractsAndAgreementsById(Long transactionPoid) {

        AdminContractsAgreementHdr hdr = findByHeaderId(transactionPoid);

        List<AdminContractsAgreementPicDtl> picDtls =
                picDtlRepository.findByIdTransactionPoid(transactionPoid);

        List<AdminContractsAgreementRenewalEntity> renewalDtls =
                renewalDtlRepository.findByIdTransactionPoid(transactionPoid);

        return ContractsAndAgreementsMapper.mapToExportDto(
                hdr,
                picDtls,
                renewalDtls
        );
    }


    @Override
    @Transactional
    public AdminContractsAgreementHdrDto updateContractsAndAgreements(
            Long transactionPoid,
            AdminContractsAgreementHdrDto dto
    ) {

        AdminContractsAgreementHdr entity = findByHeaderId(transactionPoid);

        AdminContractsAgreementHdr oldEntity = new AdminContractsAgreementHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        if (!entity.getAgreementName().equals(dto.getAgreementName())) {
            if (validationService.checkForDuplicateAgreementName(
                    dto.getAgreementName(),
                    transactionPoid
            )) {
                throw new ValidationException(
                        "Agreement Name with " + dto.getAgreementName() + " already exists"
                );
            }
        }


        validationService.partyValidation(dto.getPartyType(), dto.getPartyPoid());

        validationService.expiryDateValidation(
                dto.getExpiryDate(),
                dto.getEffectiveDate(),
                dto.getTerminationDate()
        );

        ContractsAndAgreementsMapper.updateHdrEntity(dto, entity);

        entity.setLastModifiedBy(UserContext.getUserName());
        entity.setLastModifiedDate(LocalDateTime.now());

        headerRepo.save(entity);

        loggingService.logChanges(oldEntity,entity, AdminContractsAgreementHdr.class,UserContext.getDocumentId(),transactionPoid.toString(), LogDetailsEnum.MODIFIED,"TRANSACTION_POID");


        updateAgreementContentDetails(dto.getAgreementContentDetails(), transactionPoid);
        updateAgreementRenewalDetails(dto.getRenewalDetails(), transactionPoid);

        return getContractsAndAgreementsById(transactionPoid);
    }


    @Override
    @Transactional
    public void deleteContractsAndAgreements(Long id, DeleteReasonDto deleteReasonDto) {

        AdminContractsAgreementHdr entity = findByHeaderId(id);


        LocalDate createdDate = Optional.ofNullable(entity.getCreatedDate())
                .map(LocalDateTime::toLocalDate)
                .orElse(null);


        documentDeleteService.deleteDocument(
                id,
                "ADMIN_CONTRACTS_AGREEMENT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                createdDate
        );
    }


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

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }


    private void saveAgreementRenewalDetails(
            List<AdminContractsAgreementRenewalDto> dtos,
            Long transactionPoid
    ) {

        if (dtos == null || dtos.isEmpty()) return;

        long nextDetRowId =
                renewalDtlRepository.findMaxDetRowId(transactionPoid) + 1;

        for (AdminContractsAgreementRenewalDto dto : dtos) {

            AdminContractsAgreementRenewalEntity entity =
                    ContractsAndAgreementsMapper.mapRenewalDtlDto(dto);

            entity.setId(new AdminContractsAgreementDtlId(
                    transactionPoid,
                    nextDetRowId
            ));

            renewalDtlRepository.save(entity);

            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    transactionPoid.toString(),
                    "Row Created on Agreement Renewal Detail with DetRowId: " + nextDetRowId
            );

            nextDetRowId++;
        }
    }

    private void saveAgreementContentDetails(
            List<AdminContractsAgreementPicDtlDto> dtos,
            Long transactionPoid
    ) {

        if (dtos == null || dtos.isEmpty()) return;

        long nextDetRowId =
                picDtlRepository.findMaxDetRowId(transactionPoid) + 1;

        for (AdminContractsAgreementPicDtlDto dto : dtos) {

            AdminContractsAgreementPicDtl entity =
                    ContractsAndAgreementsMapper.mapPicDtlDtoToEntity(dto);
            entity.setCreatedBy(UserContext.getUserName());
            entity.setCreatedDate(LocalDateTime.now());

            entity.setId(new AdminContractsAgreementDtlId(
                    transactionPoid,
                    nextDetRowId
            ));
            picDtlRepository.save(entity);

            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    transactionPoid.toString(),
                    "Row Created on Agreement Content Detail with DetRowId: " + nextDetRowId
            );

            nextDetRowId++;
        }
    }


    private void updateAgreementRenewalDetails(
            List<AdminContractsAgreementRenewalDto> dtos,
            Long transactionPoid
    ) {

        if (dtos == null || dtos.isEmpty()) return;

        List<AdminContractsAgreementRenewalEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<AdminContractsAgreementRenewalEntity>> logs = new ArrayList<>();

        for (AdminContractsAgreementRenewalDto dto : dtos) {

            switch (resolveAction(dto.getActionType())) {

                case ACTION_ISDELETED -> {
                    renewalDtlRepository.deleteByIdTransactionPoidAndIdDetRowId(
                            transactionPoid,
                            dto.getDetRowId()
                    );
                    loggingService.createLogSummaryEntry(
                            UserContext.getDocumentId(),
                            transactionPoid.toString(),
                            "Row Deleted on Agreement Renewal Detail with DetRowId: " + dto.getDetRowId()
                    );
                }

                case ACTION_ISCREATED ->
                        saveAgreementRenewalDetails(List.of(dto), transactionPoid);

                case ACTION_ISUPDATED -> {
                    AdminContractsAgreementRenewalEntity entity =
                            renewalDtlRepository
                                    .findByIdTransactionPoidAndIdDetRowId(
                                            transactionPoid,
                                            dto.getDetRowId()
                                    )
                                    .orElseThrow(() ->
                                            new ResourceNotFoundException(
                                                    "Agreement Renewal Detail",
                                                    "DetRowId",
                                                    dto.getDetRowId()
                                            )
                                    );

                    AdminContractsAgreementRenewalEntity old = new AdminContractsAgreementRenewalEntity();
                    BeanUtils.copyProperties(entity, old);

                    ContractsAndAgreementsMapper.updateRenewalDtlEntity(dto, entity);
                    entity.setLastUpdatedBy(UserContext.getUserName());
                    entity.setLastUpdatedDate(LocalDateTime.now());

                    toUpdate.add(entity);

                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
                    logs.add(new LogRequestDto<>(old, entity, AdminContractsAgreementRenewalEntity.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            renewalDtlRepository.saveAll(toUpdate);
            loggingService.createLogBatch(logs);
        }
    }

    private void updateAgreementContentDetails(
            List<AdminContractsAgreementPicDtlDto> dtos,
            Long transactionPoid
    ) {

        if (dtos == null || dtos.isEmpty()) return;

        List<AdminContractsAgreementPicDtl> toUpdate = new ArrayList<>();
        List<LogRequestDto<AdminContractsAgreementPicDtl>> logs = new ArrayList<>();

        for (AdminContractsAgreementPicDtlDto dto : dtos) {

            switch (resolveAction(dto.getActionType())) {

                case ACTION_ISDELETED -> {
                    picDtlRepository.deleteByIdTransactionPoidAndIdDetRowId(
                            transactionPoid,
                            dto.getDetRowId()
                    );
                    loggingService.createLogSummaryEntry(
                            UserContext.getDocumentId(),
                            transactionPoid.toString(),
                            "Row Deleted on Agreement Content Detail with DetRowId: " + dto.getDetRowId()
                    );
                }

                case ACTION_ISCREATED ->
                        saveAgreementContentDetails(List.of(dto), transactionPoid);

                case ACTION_ISUPDATED -> {
                    AdminContractsAgreementPicDtl entity =
                            picDtlRepository
                                    .findByIdTransactionPoidAndIdDetRowId(
                                            transactionPoid,
                                            dto.getDetRowId()
                                    )

                                    .orElseThrow(() ->
                                            new ResourceNotFoundException(
                                                    "Agreement Content Detail",
                                                    "DetRowId",
                                                    dto.getDetRowId()
                                            )
                                    );

                    AdminContractsAgreementPicDtl old = new AdminContractsAgreementPicDtl();
                    BeanUtils.copyProperties(entity, old);

                    ContractsAndAgreementsMapper.updatePicDtlEntity(dto, entity);
                    entity.setLastModifiedBy(UserContext.getUserName());
                    entity.setLastModifiedDate(LocalDateTime.now());
                    toUpdate.add(entity);

                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
                    logs.add(new LogRequestDto<>(old, entity, AdminContractsAgreementPicDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            picDtlRepository.saveAll(toUpdate);
            loggingService.createLogBatch(logs);
        }
    }

    private AdminContractsAgreementHdr findByHeaderId(Long transactionPoid) {
        return headerRepo.findById(transactionPoid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Contracts and Agreements",
                                "transactionPoid",
                                transactionPoid
                        )
                );
    }

    private String resolveAction(String rawAction) {
        String action = rawAction == null ? ACTION_NOCHANGES : rawAction.trim().toUpperCase();
        return switch (action) {
            case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }
}
