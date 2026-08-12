package com.asg.shipping.importmanifestupdate.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.lov.MasterLovLookup;
import com.asg.common.lib.security.model.CustomAuthDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.asg.shipping.common.service.LovService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import com.asg.shipping.importmanifestupdate.constants.BlManifestValidationMessages;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.entity.*;
import com.asg.shipping.importmanifestupdate.respository.*;
import com.asg.shipping.importmanifestupdate.util.ImportManifestBlMapper;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportManifestBlServiceImpl implements ImportManifestBlService {

    private final ShipBlManifestHdrRepository repository;
    private final ShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ShipBlManifestPartBLRepository containerPrtRepository;
    private final ShipBlManifestEmailFaxDtlRepository emailFaxDtlRepository;
    private final ShipBlManifestMafiDtlRepository mafiDtlRepository;
    private final DocumentSearchService documentService;
    private final ImportManifestBlMapper mapper;
    private final EntityManager entityManager;

    private final ImportManifestBlProcRepository procRepository;
    private final AddressDetailsRepository addressDetailsRepository;
    private final LoggingService loggingService;
    private final LovService lovService;
    private final MasterLovLookup masterLovLookup;
    @Qualifier("lovLookupExecutor")
    private final Executor lovLookupExecutor;
    private final DocumentDeleteService documentDeleteService;
    private final BlManifestValidationService blManifestValidationService;

    private static final String ACTION_NOCHANGES = "NOCHANGES";
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";

    @Override
    @Transactional
    public ImportManifestBlRequestDto updateImportManifestBl(
            Long id,
            ImportManifestBlUpdateDTO dto,
            Long companyPoid,
            Long groupPoid) {

        log.info("Updating Import Manifest BL with id: {}", id);

        ShipBlManifestHdr existingEntity = repository.findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import Manifest BL", "transactionPoid", id.toString()));

        ShipBlManifestHdr oldEntity = new ShipBlManifestHdr();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        validateMandatoryFieldsForUpdate(dto);

        blManifestValidationService.validateHoldReasons(
                dto.getHoldReason(), dto.getHoldCanDo(), dto.getHoldRemarks());

        boolean addressFound = checkNotifyAddressesExist(dto);
        blManifestValidationService.validateAddressesForCan(
                dto.getHoldReason(), dto.getConsigneePoid(), dto.getNotifyPoid1(),
                dto.getManuallyCanSend(), addressFound);

        blManifestValidationService.validateContainerFields(
                filterActiveContainers(dto.getContainers()));

        blManifestValidationService.validateFinancialGain(
                filterActiveCharges(dto.getChargeDetails()));

        blManifestValidationService.validateFreightType(
                dto.getFreightStatus(), dto.getHoldReason(),
                filterActiveCharges(dto.getChargeDetails()));

        blManifestValidationService.validateDemurrageChargeCode(
                filterActiveCharges(dto.getChargeDetails()));

        validateQuotationMapping(dto, id, companyPoid, groupPoid);

        validateUpdateDTO(dto, id, companyPoid, groupPoid);

        String oldFreightStatus = existingEntity.getFreightStatus();
        String oldDoNo = existingEntity.getDoNo();

        // Map DTO to entity and save
        ShipBlManifestHdr savedEntity = mapper.mapUpdateDTOToEntity(dto, existingEntity);
        if (hasAnyEdiChange(dto)) {
            formatEdiFields(savedEntity);
        }
        ShipBlManifestHdr saved = repository.saveAndFlush(savedEntity);
        updateDetailTables(dto, saved.getTransactionPoid());
        loggingService.logChanges(oldEntity, savedEntity, ShipBlManifestHdr.class, UserContext.getDocumentId(),
                id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Register AFTER COMMIT actions (legacy DocumentAfterSave)
        registerAfterCommitActions(saved, groupPoid, companyPoid, oldDoNo, oldFreightStatus, dto.getDoNo(),
                dto.getFreightStatus());

        log.info("Successfully updated Import Manifest BL with id: {}", id);
        return getImportManifestBl(id);
    }

    @Override
    // Intentionally not @Transactional: this is a pure read, and leaving the thread free of a
    // transaction is what lets loadDetailTables fetch the detail tables concurrently. Every
    // repository call here is self-transactional on its own.
    public ImportManifestBlRequestDto getImportManifestBl(Long id) {
        log.info("Getting Import Manifest BL with id: {}", id);

        ShipBlManifestHdr entity = repository.findByTransactionPoid(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Import Manifest BL", "transactionPoid", id.toString()));

        ImportManifestBlRequestDto dto = mapper.mapToDto(entity);

        loadDetailTables(dto, id);
        enrichLovData(dto);
        dto.setSimpleCargoDescription(joinCargoDescriptionsByType(dto.getCargoDescriptions(), "DESC", "DESCRIPTION"));
        dto.setSimpleCargoMarks(joinCargoDescriptionsByType(dto.getCargoDescriptions(), "MARK", "MARKS"));

        log.info("Successfully retrieved Import Manifest BL with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public ImportManifestUpdateOpsDto updateImportManifestUpdateOps(
            Long id,
            ImportManifestUpdateOpsDto screenDto,
            Long companyPoid,
            Long groupPoid) {
        log.info("Updating Import Manifest Update Ops with id: {}", id);

        ImportManifestBlUpdateDTO dto = mapper.mapScreenDtoToUpdateDto(screenDto);
        ImportManifestBlRequestDto resultDto = updateImportManifestBl(id, dto, companyPoid, groupPoid);
        
        return mapper.mapToScreenDto(resultDto);
    }

    @Override
    // Read-only wrapper around getImportManifestBl — see the note there on why it stays untransacted.
    public ImportManifestUpdateOpsDto getImportManifestUpdateOps(Long id) {
        log.info("Getting Import Manifest Update Ops with id: {}", id);
        
        ImportManifestBlRequestDto resultDto = getImportManifestBl(id);
        
        return mapper.mapToScreenDto(resultDto);
    }

    @Override
    @Transactional
    public void deleteImportManifestBl(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting Import Manifest BL with id: {}", id);

        ShipBlManifestHdr entity = repository.findByTransactionPoid(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Import Manifest BL", "transactionPoid", id.toString()));

        LocalDate transactionDate = entity.getTransactionDate() == null
                ? null
                : LocalDate.from(entity.getTransactionDate());

        documentDeleteService.deleteDocument(
                id,
                "SHIP_BL_MANIFEST_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                transactionDate);

        log.info("Successfully deleted Import Manifest BL with id: {}", id);
    }

    @Override
    public Map<String, Object> list(FilterRequestDto request, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        try {
            String operator = documentService.resolveOperator(request);
            String isDeleted = documentService.resolveIsDeleted(request);
            List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", fromDate, toDate);

            RawSearchResult raw = documentService.search(
                    UserContext.getDocumentId(),
                    filters,
                    operator,
                    pageable,
                    isDeleted,
                    "BL_NUMBER",
                    "TRANSACTION_POID");

            Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
            return PaginationUtil.wrapPage(page, raw.displayFields());
        } catch (Exception e) {
            log.error("Error listing Import Manifest BLs", e);
            throw e;
        }
    }


    @Override
    @org.springframework.transaction.annotation.Transactional
    public ResendCanResponseDto resendCan(Long transactionPoId, String updateDemurrage) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            return procRepository.resendCan(entity.getVoyageTransactionPoid(), transactionPoId, updateDemurrage);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to resend CAN: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error resending CAN for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public LoadEmailFaxResponseDto loadEmailFax(Long addressMasterPoid, String addressType) {
        try {
            var addressDetails = addressDetailsRepository.findByAddressMasterPoidAndAddressType(
                    addressMasterPoid, "CAN");
            var emailFaxDetails = addressDetails.stream()
                    .map(ad -> EmailFaxDetailDto.builder()
                            .actionType("isCreated")
                            .addressPoid(ad.getAddressPoid() != null ? new java.math.BigDecimal(ad.getAddressPoid()) : null)
                            .addressType(addressType)
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .sendYesNo("N")
                            .sendEmailFax("EMAIL")
                            .build())
                    .toList();
            log.info("Loaded email/fax data for addressMasterPoid: {}, count: {}", addressMasterPoid,
                    emailFaxDetails.size());
            return LoadEmailFaxResponseDto.builder().emailFaxDetails(emailFaxDetails).build();
        } catch (Exception e) {
            log.error("Error loading email/fax data for addressMasterPoid: {}", addressMasterPoid, e);
            throw e;
        }
    }

    @Override
    public BlStatusResponseDto getBlStatus(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.getBlStatus(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to get BL status: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting BL status for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    /**
     * Checks if at least one notify party has SendYesNo=Y with an email/fax set.
     * Legacy: AddressManuallyCheck() lines 870-952
     */
    private boolean checkNotifyAddressesExist(ImportManifestBlUpdateDTO dto) {
        if (dto.getNotifyParties() == null || dto.getNotifyParties().isEmpty()) {
            return false;
        }
        return dto.getNotifyParties().stream()
                .anyMatch(np -> "Y".equalsIgnoreCase(np.getSendYesNo())
                        && (np.getFax() != null || np.getEmail1() != null || np.getEmail2() != null));
    }

    /**
     * Filters containers to only include active (non-deleted, non-nochanges) items
     * for validation by the shared service.
     */
    private List<ContainerRequestDto> filterActiveContainers(List<ContainerRequestDto> containers) {
        if (containers == null)
            return List.of();
        return containers.stream()
                .filter(c -> {
                    String action = resolveAction(c.getActionType());
                    return !ACTION_ISDELETED.equals(action) && !ACTION_NOCHANGES.equals(action);
                })
                .toList();
    }

    /**
     * Filters charges to only include active (non-deleted) items
     * for validation by the shared service.
     */
    private List<ChargeRequestDto> filterActiveCharges(List<ChargeRequestDto> charges) {
        if (charges == null)
            return List.of();
        return charges.stream()
                .filter(c -> !ACTION_ISDELETED.equals(resolveAction(c.getActionType())))
                .toList();
    }

    /**
     * PROC_SHIP_VALD_BEFORE_SAVE - called as GetBlValidate() in legacy.
     * Returns true if BL validation passes. Used as a condition together with
     * quotation/bookedByPP checks.
     * Legacy: GetBlValidate() lines 523-568 + DocumentBeforeSave lines 846-855
     */
    private void validateQuotationMapping(ImportManifestBlUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        boolean blValidateResult = callBlValidateProc(dto, id, companyPoid, groupPoid);

        // Legacy: if (GetBlValidate() && quotationPoid == null && freightStatus == "2")
        // if (bookedByPP != null && bookedByPP == "N") -> error
        if (blValidateResult
                && dto.getQuotationTransactionPoid() == null
                && "2".equalsIgnoreCase(dto.getFreightStatus())) {
            if (dto.getBookedByPp() != null && "N".equalsIgnoreCase(dto.getBookedByPp())) {
                throw new ValidationException(BlManifestValidationMessages.QUOTATION_MAPPING_REQUIRED);
            }
        }
    }

    /**
     * Calls PROC_SHIP_VALD_BEFORE_SAVE and returns boolean result.
     * Legacy: GetBlValidate() lines 523-568
     */
    private boolean callBlValidateProc(ImportManifestBlUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        try {
            Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE");
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_VOYAGE_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_VALIDATION_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);
            query.setParameter("P_TRANSACTION_POID", id);
            query.setParameter("P_VOYAGE_TRANSACTION_POID", dto.getVoyageTransactionPoid());
            query.setParameter("P_VALIDATION_TYPE", "VLD_QUOTATION");

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");
            return result != null && result.equalsIgnoreCase("TRUE");
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_VALD_BEFORE_SAVE for transaction: {}", id, e);
            return false; // Legacy returns false on exception
        }
    }

    private void validateUpdateDTO(ImportManifestBlUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        // Validate BL number uniqueness per voyage (excluding current record)
        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().isEmpty()) {
            Long voyagePoid = dto.getVoyageTransactionPoid();
            if (voyagePoid != null) {
                if (repository.existsByVoyageTransactionPoidAndBlNumberExcludingPoid(voyagePoid,
                        dto.getBlNumber().trim(), id)) {
                    throw new ValidationException(String.format(
                            BlManifestValidationMessages.BL_NUMBER_EXISTS_FOR_VOYAGE, dto.getBlNumber().trim()));
                }
            }
            // Validate global BL number uniqueness (excluding current record)
            if (repository.existsByBlNumberExcludingPoid(dto.getBlNumber().trim(), id)) {
                throw new ValidationException(
                        String.format(BlManifestValidationMessages.BL_NUMBER_EXISTS, dto.getBlNumber().trim()));
            }
        }

        // Validate port of loading for non-EXPORT BL_TYPE
        if (dto.getBlType() != null && !"EXPORT".equals(dto.getBlType()) && dto.getPortOfLoadingPoid() == null) {
            // Check if existing record has port of loading
            ShipBlManifestHdr existing = repository.findById(id).orElse(null);
            if (existing == null || existing.getPortOfLoadingPoid() == null) {
                throw new ValidationException(BlManifestValidationMessages.PORT_OF_LOADING_REQUIRED);
            }
        }
    }

    public void formatEdiFields(ShipBlManifestHdr entity) {
        String blType = entity.getBlType();
        boolean isExport = "EXPORT".equals(blType);

        if (!isExport) {
            // For non-EXPORT: trim and remove newlines
            if (entity.getShipperEdiName() != null) {
                entity.setShipperEdiName(entity.getShipperEdiName()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getShipperEdiAddress() != null) {
                entity.setShipperEdiAddress(entity.getShipperEdiAddress()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getConsigneeEdiName() != null) {
                entity.setConsigneeEdiName(entity.getConsigneeEdiName()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getConsigneeEdiAddress() != null) {
                entity.setConsigneeEdiAddress(entity.getConsigneeEdiAddress()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify1EdiName() != null) {
                entity.setNotify1EdiName(entity.getNotify1EdiName()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify1EdiAddress() != null) {
                entity.setNotify1EdiAddress(entity.getNotify1EdiAddress()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify2EdiName() != null) {
                entity.setNotify2EdiName(entity.getNotify2EdiName()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify2EdiAddress() != null) {
                entity.setNotify2EdiAddress(entity.getNotify2EdiAddress()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify3EdiName() != null) {
                entity.setNotify3EdiName(entity.getNotify3EdiName()
                        .replace("\n", "").replace("\r", "").trim());
            }
            if (entity.getNotify3EdiAddress() != null) {
                entity.setNotify3EdiAddress(entity.getNotify3EdiAddress()
                        .replace("\n", "").replace("\r", "").trim());
            }
        } else {
            // For EXPORT: convert to uppercase
            if (entity.getShipperEdiName() != null) {
                entity.setShipperEdiName(entity.getShipperEdiName().toUpperCase());
            }
            if (entity.getShipperEdiAddress() != null) {
                entity.setShipperEdiAddress(entity.getShipperEdiAddress().toUpperCase());
            }
            if (entity.getConsigneeEdiName() != null) {
                entity.setConsigneeEdiName(entity.getConsigneeEdiName().toUpperCase());
            }
            if (entity.getConsigneeEdiAddress() != null) {
                entity.setConsigneeEdiAddress(entity.getConsigneeEdiAddress().toUpperCase());
            }
            if (entity.getNotify1EdiName() != null) {
                entity.setNotify1EdiName(entity.getNotify1EdiName().toUpperCase());
            }
            if (entity.getNotify1EdiAddress() != null) {
                entity.setNotify1EdiAddress(entity.getNotify1EdiAddress().toUpperCase());
            }
            if (entity.getNotify2EdiName() != null) {
                entity.setNotify2EdiName(entity.getNotify2EdiName().toUpperCase());
            }
            if (entity.getNotify2EdiAddress() != null) {
                entity.setNotify2EdiAddress(entity.getNotify2EdiAddress().toUpperCase());
            }
            if (entity.getNotify3EdiName() != null) {
                entity.setNotify3EdiName(entity.getNotify3EdiName().toUpperCase());
            }
            if (entity.getNotify3EdiAddress() != null) {
                entity.setNotify3EdiAddress(entity.getNotify3EdiAddress().toUpperCase());
            }
        }

        // Truncate CONSIGNEE_EDI_NAME to 190 characters
        if (entity.getConsigneeEdiName() != null && entity.getConsigneeEdiName().length() > 190) {
            entity.setConsigneeEdiName(entity.getConsigneeEdiName().substring(0, 190));
        }
    }

    private void updateDetailTables(ImportManifestBlUpdateDTO dto, Long transactionPoid) {

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        // General Cargo Details
        if (dto.getGeneralCargoDetails() != null) {
            List<ShipBlManifestGeneralDtl> toSave = new ArrayList<>();
            List<ShipBlManifestGeneralDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestGeneralDtl>> logRequests = new ArrayList<>();
            Long maxDetRowId = generalDtlRepository.getMaxDetRowId(transactionPoid);

            for (GeneralCargoRequestDto detailDto : dto.getGeneralCargoDetails()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestGeneralDtl entity = mapper.mapGeneralDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, ++maxDetRowId));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestGeneralDtl existing = generalDtlRepository
                                .findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("General Cargo Detail", "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestGeneralDtl oldEntity = new ShipBlManifestGeneralDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateGeneralFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid,
                                detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestGeneralDtl.class, docId,
                                docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestGeneralDtl> saved = generalDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on General Cargo Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                generalDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestGeneralDtl> entitiesToDelete = generalDtlRepository.findAllById(toDelete);
                generalDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }

        }

        // Cargo Descriptions
        if (hasCargoInputForType(dto, "DESC", "DESCRIPTION")) {
            replaceSimpleCargoRows(transactionPoid, resolveSimpleCargoDescription(dto),
                    "DESC", docId, docKeyPoid);
        }
        if (hasCargoInputForType(dto, "MARK", "MARKS")) {
            replaceSimpleCargoRows(transactionPoid, resolveSimpleCargoMarks(dto),
                    "MARK", docId, docKeyPoid);
        }

        // Containers
        if (dto.getContainers() != null) {
            List<ShipBlManifestContainerDtl> toSave = new ArrayList<>();
            List<ShipBlManifestContainerDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestContainerDtl>> logRequests = new ArrayList<>();

            Long maxDetRowId = containerDtlRepository.getMaxDetRowId(transactionPoid);

            for (ContainerRequestDto detailDto : dto.getContainers()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {

                        ShipBlManifestContainerDtl entity = mapper.mapContainerDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, ++maxDetRowId));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {

                        ShipBlManifestContainerDtl existing = containerDtlRepository.findById(
                                new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Container Detail",
                                        "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestContainerDtl oldEntity = new ShipBlManifestContainerDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateContainerFromDto(detailDto, existing);

                        if (existing.getContainerNo() != null && !existing.getContainerNo().trim().isEmpty()) {

                            containerDtlRepository
                                    .findByIdTransactionPoidAndContainerNo(
                                            transactionPoid,
                                            existing.getContainerNo().trim())
                                    .ifPresent(conflict -> {
                                        if (!conflict.getId().getDetRowId()
                                                .equals(existing.getId().getDetRowId())) {

                                            throw new ValidationException(
                                                    String.format(BlManifestValidationMessages.CONTAINER_DUPLICATE,
                                                            existing.getContainerNo()));
                                        }
                                    });
                        }

                        toUpdate.add(existing);

                        String logDetail = String.format(
                                "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s",
                                transactionPoid,
                                detailDto.getDetRowId());

                        logRequests.add(
                                new LogRequestDto<>(
                                        oldEntity,
                                        existing,
                                        ShipBlManifestContainerDtl.class,
                                        docId,
                                        docKeyPoid,
                                        logDetail));
                    }

                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestContainerDtl> saved = containerDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Container Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                containerDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestContainerDtl> entitiesToDelete = containerDtlRepository.findAllById(toDelete);
                containerDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
        }

        // Charges
        if (dto.getChargeDetails() != null) {
            List<ShipBlManifestChargesDtl> toSave = new ArrayList<>();
            List<ShipBlManifestChargesDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestChargesDtl>> logRequests = new ArrayList<>();

            Long maxDetRowId = chargesDtlRepository.getMaxDetRowId(transactionPoid);

            for (ChargeRequestDto detailDto : dto.getChargeDetails()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestChargesDtl entity = mapper.mapChargesDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, ++maxDetRowId));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestChargesDtl existing = chargesDtlRepository
                                .findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestChargesDtl oldEntity = new ShipBlManifestChargesDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateChargesFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid,
                                detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestChargesDtl.class, docId,
                                docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestChargesDtl> saved = chargesDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Charge Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                chargesDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestChargesDtl> entitiesToDelete = chargesDtlRepository.findAllById(toDelete);
                chargesDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
        }

        // Part BLs
        if (dto.getPartBls() != null) {
            List<ShipBlManifestPartBL> toSave = new ArrayList<>();
            List<ShipBlManifestPartBL> toUpdate = new ArrayList<>();
            List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestPartBL>> logRequests = new ArrayList<>();

            Long maxDetRowId = containerPrtRepository.getMaxDetRowId(transactionPoid);

            for (PartBlRequestDto detailDto : dto.getPartBls()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestPartBL entity = mapper.mapContainerPrtFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, ++maxDetRowId));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestPartBL existing = containerPrtRepository
                                .findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Part BL Detail", "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestPartBL oldEntity = new ShipBlManifestPartBL();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updatePartBlFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid,
                                detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestPartBL.class, docId,
                                docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestPartBL> saved = containerPrtRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Part BL Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                containerPrtRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestPartBL> entitiesToDelete = containerPrtRepository.findAllById(toDelete);
                containerPrtRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
        }

        // Notify Parties
        if (dto.getNotifyParties() != null) {
            List<ShipBlManifestEmailFaxDtl> toSave = new ArrayList<>();
            List<ShipBlManifestEmailFaxDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestEmailFaxId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestEmailFaxDtl>> logRequests = new ArrayList<>();

            Long maxDetRowId = emailFaxDtlRepository.getMaxDetRowId(transactionPoid);

            for (NotifyPartyRequestDto detailDto : dto.getNotifyParties()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestEmailFaxDtl entity = mapper.mapEmailFaxDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestEmailFaxId(transactionPoid, ++maxDetRowId,
                                detailDto.getAddressType()));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestEmailFaxDtl existing = emailFaxDtlRepository
                                .findById(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(),
                                        detailDto.getAddressType()))
                                .orElseThrow(() -> new ResourceNotFoundException("Notify Party Detail", "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestEmailFaxDtl oldEntity = new ShipBlManifestEmailFaxDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateEmailFaxFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid,
                                detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestEmailFaxDtl.class, docId,
                                docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(),
                                    detailDto.getAddressType()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestEmailFaxDtl> saved = emailFaxDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Notify Party Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                emailFaxDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestEmailFaxDtl> entitiesToDelete = emailFaxDtlRepository.findAllById(toDelete);
                emailFaxDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
        }

        // MAFI Details
        if (dto.getMafiDetails() != null) {
            List<ShipBlManifestMafiDtl> toSave = new ArrayList<>();
            List<ShipBlManifestMafiDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestMafiDtl>> logRequests = new ArrayList<>();

            Long maxDetRowId = mafiDtlRepository.getMaxDetRowId(transactionPoid);

            for (MafiRequestDto detailDto : dto.getMafiDetails()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestMafiDtl entity = mapper.mapMafiDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, ++maxDetRowId));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestMafiDtl existing = mafiDtlRepository
                                .findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("MAFI Detail", "detRowId",
                                        detailDto.getDetRowId()));

                        ShipBlManifestMafiDtl oldEntity = new ShipBlManifestMafiDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateMafiFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid,
                                detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestMafiDtl.class, docId,
                                docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestMafiDtl> saved = mafiDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on MAFI Detail with detRowId: %s",
                            e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                mafiDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }
            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestMafiDtl> entitiesToDelete = mafiDtlRepository.findAllById(toDelete);
                mafiDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
        }

    }

    /**
     * PROC_SHIP_DO_BL_STATUS - Update DO/BL status
     * Parameters: P_LOGIN_GROUP_POID, P_LOGIN_COMPANY_POID, P_LOGIN_USER_POID,
     * P_TRANSACTION_POID, P_RESULT (OUT VARCHAR)
     */
    private void updateDoBlStatus(Long transactionPoid, Long groupPoid, Long companyPoid) {
        try {
            Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_DO_BL_STATUS");
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");
            log.debug("Successfully called PROC_SHIP_DO_BL_STATUS, result: {}", result);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_DO_BL_STATUS for transaction: {}", transactionPoid, e);
        }
    }

    /**
     * PROC_SHIP_BL_PAGE_SAVE_AFTER - Post-save processing
     * Parameters: P_LOGIN_GROUP_POID, P_LOGIN_COMPANY_POID, P_TRANSACTION_POID,
     * P_PARAM1, P_PARAM2, P_LOGIN_USER_POID
     */
    private void callAfterSaveProcedure(ShipBlManifestHdr saved, Long groupPoid, Long companyPoid, String param2) {
        try {
            Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();
            Long transactionPoid = saved.getTransactionPoid();
            String processType = param2 != null ? param2 : "AUTOSUMWEIGHTPACKATE";

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER");
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);

            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_KEY_POID", transactionPoid);
            query.setParameter("P_DET_ROW_ID", null);
            query.setParameter("P_UPDATE_TYPE", processType);
            query.setParameter("P_LOGIN_USER", String.valueOf(userPoid));

            query.execute();
            log.debug("Successfully called PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", saved.getTransactionPoid(), e);
            // Don't throw exception - post-save processing should not fail the save
            // operation
        }
    }

    /**
     * Load all detail tables for a transaction
     */
    /**
     * The seven detail tables are independent, so on a plain read they are fetched concurrently.
     *
     * That is only safe with no transaction bound to the calling thread: each repository call then
     * opens and closes its own EntityManager. Inside a transaction — {@code updateImportManifestBl}
     * re-reads the document through here after saving — the EntityManager is shared and must not be
     * touched from another thread, so that path stays sequential.
     */
    public ImportManifestBlRequestDto loadDetailTables(ImportManifestBlRequestDto dto, Long transactionPoid) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            loadDetailTablesSequentially(dto, transactionPoid);
        } else {
            loadDetailTablesInParallel(dto, transactionPoid);
        }
        dto.setSimpleCargoDescription(joinCargoDescriptionsByType(dto.getCargoDescriptions(), "DESC", "DESCRIPTION"));
        dto.setSimpleCargoMarks(joinCargoDescriptionsByType(dto.getCargoDescriptions(), "MARK", "MARKS"));
        return dto;
    }

    private void loadDetailTablesSequentially(ImportManifestBlRequestDto dto, Long transactionPoid) {
        dto.setGeneralCargoDetails(mapper.mapGeneralDtlListToDto(
                generalDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setCargoDescriptions(mapper.mapCargoDtlListToDto(
                cargoDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setContainers(mapper.mapContainerDtlListToDto(
                containerDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setChargeDetails(mapper.mapChargesDtlListToDto(
                chargesDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setPartBls(mapper.mapContainerPrtListToDto(
                containerPrtRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setNotifyParties(mapper.mapEmailFaxDtlListToDto(
                emailFaxDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        dto.setMafiDetails(mapper.mapMafiDtlListToDto(
                mafiDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
    }

    private void loadDetailTablesInParallel(ImportManifestBlRequestDto dto, Long transactionPoid) {
        CustomAuthDetails caller = UserContext.getCurrentUser();

        CompletableFuture<List<GeneralCargoRequestDto>> generalCargoFuture = supplyWithUserContext(caller,
                () -> mapper.mapGeneralDtlListToDto(
                        generalDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<CargoDescriptionRequestDto>> cargoFuture = supplyWithUserContext(caller,
                () -> mapper.mapCargoDtlListToDto(
                        cargoDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<ContainerRequestDto>> containerFuture = supplyWithUserContext(caller,
                () -> mapper.mapContainerDtlListToDto(
                        containerDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<ChargeRequestDto>> chargesFuture = supplyWithUserContext(caller,
                () -> mapper.mapChargesDtlListToDto(
                        chargesDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<PartBlRequestDto>> partBlFuture = supplyWithUserContext(caller,
                () -> mapper.mapContainerPrtListToDto(
                        containerPrtRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<NotifyPartyRequestDto>> notifyPartyFuture = supplyWithUserContext(caller,
                () -> mapper.mapEmailFaxDtlListToDto(
                        emailFaxDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));
        CompletableFuture<List<MafiRequestDto>> mafiFuture = supplyWithUserContext(caller,
                () -> mapper.mapMafiDtlListToDto(
                        mafiDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid)));

        try {
            CompletableFuture.allOf(generalCargoFuture, cargoFuture, containerFuture, chargesFuture,
                    partBlFuture, notifyPartyFuture, mafiFuture).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }

        dto.setGeneralCargoDetails(generalCargoFuture.join());
        dto.setCargoDescriptions(cargoFuture.join());
        dto.setContainers(containerFuture.join());
        dto.setChargeDetails(chargesFuture.join());
        dto.setPartBls(partBlFuture.join());
        dto.setNotifyParties(notifyPartyFuture.join());
        dto.setMafiDetails(mafiFuture.join());
    }

    /**
     * UserContext is a plain ThreadLocal a pooled thread cannot see, so the caller's auth details
     * are carried onto the worker and cleared again once the task is done.
     */
    private <T> CompletableFuture<T> supplyWithUserContext(CustomAuthDetails caller, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            UserContext.setCurrentUser(caller);
            try {
                return supplier.get();
            } finally {
                UserContext.clear();
            }
        }, lovLookupExecutor);
    }

    /**
     * Every header LOV that resolves through plain SQL, joined onto the header row in one shot.
     *
     * These were eleven separate single-row round-trips. They all hang off primary keys of the one
     * header row, so a LEFT JOIN returns exactly one row with no multiplication — unlike the detail
     * tables, where joining the master data would both defeat the LOV cache and repeat every master
     * name once per detail row.
     *
     * The column list, the join keys and the quotation description expression are lifted verbatim
     * from the per-field queries this replaces, so a row resolves here exactly as it did before.
     * Ports deliberately join on PORT_POID alone, matching the query that was here previously.
     */
    private static final String HEADER_LOV_PROJECTION_SQL = """
            SELECT QTN.TRANSACTION_POID, QTN.DOC_REF,
                   'Vld_DT-' || TO_CHAR(QTN.VALIDITY_FROM_DATE, 'DD-MON-RRRR') || ' BTW ' || TO_CHAR(QTN.VALIDITY_TO_DATE, 'DD-MON-RRRR')
                     || ', Cust- ' || QTN.CUSTOMER_POID || ' ' || GET_ADDRESS_NAME(SUBSTR(QTN.CUSTOMER_POID, 1, INSTR(QTN.CUSTOMER_POID, '.') - 1))
                     || ', Line- ' || QTN.LINE_POID || ' ' || GET_LINE_CODE(QTN.LINE_POID)
                     || ', SalesMan-' || QTN.SALESMAN_POID || ' ' || GET_SALESMAN_NAME(QTN.SALESMAN_POID)
                     || ', Load-' || GET_PORT_NAME(QTN.LOADING_PORT_POID)
                     || ', Discharge-' || GET_PORT_NAME(QTN.DISCHARGE_PORT_POID)
                     || ', Company-' || GET_COMPANY_CODE(QTN.QTN_COMPANY)
                     || ',Comodity ' || QTN.COMMODITY_TYPE,
                   SLM.SALESMAN_POID, SLM.SALESMAN_CODE, SLM.SALESMAN_NAME,
                   CMD.COMODITY_POID, CMD.COMODITY_CODE, CMD.COMODITY_NAME,
                   CNS.ADDRESS_MASTER_POID, CNS.ADDRESS_MASTER_POID, CNS.ADDRESS_NAME,
                   NF1.ADDRESS_MASTER_POID, NF1.ADDRESS_MASTER_POID, NF1.ADDRESS_NAME,
                   CUS.CUSTOMER_POID, CUS.CUSTOMER_CODE, CUS.CUSTOMER_NAME,
                   RCP.PORT_POID, RCP.PORT_CODE, RCP.PORT_NAME,
                   DLV.PORT_POID, DLV.PORT_CODE, DLV.PORT_NAME,
                   LOD.PORT_POID, LOD.PORT_CODE, LOD.PORT_NAME,
                   DIS.PORT_POID, DIS.PORT_CODE, DIS.PORT_NAME
            FROM SHIP_BL_MANIFEST_HDR HDR
            LEFT JOIN SALES_QUOTATION_SHIP_HDR QTN ON QTN.TRANSACTION_POID  = HDR.QUOTATION_TRANSACTION_POID
            LEFT JOIN SALES_SALESMAN_MASTER    SLM ON SLM.SALESMAN_POID     = HDR.SALESMAN_POID
            LEFT JOIN SHIP_COMODITY_MASTER     CMD ON CMD.COMODITY_POID     = HDR.COMODITY_POID
            LEFT JOIN GLOBAL_ADDRESS_MASTER    CNS ON CNS.ADDRESS_MASTER_POID = HDR.CONSIGNEE_POID
            LEFT JOIN GLOBAL_ADDRESS_MASTER    NF1 ON NF1.ADDRESS_MASTER_POID = HDR.NOTIFY_POID_1
            LEFT JOIN SALES_CUSTOMER_MASTER    CUS ON CUS.CUSTOMER_POID     = HDR.BOOKING_PARTY_POID
            LEFT JOIN SHIP_PORT_MASTER         RCP ON RCP.PORT_POID         = HDR.PLACE_OF_RECIEPT_POID
            LEFT JOIN SHIP_PORT_MASTER         DLV ON DLV.PORT_POID         = HDR.PLACE_OF_DELIEVERY_POID
            LEFT JOIN SHIP_PORT_MASTER         LOD ON LOD.PORT_POID         = HDR.PORT_OF_LOADING_POID
            LEFT JOIN SHIP_PORT_MASTER         DIS ON DIS.PORT_POID         = HDR.PORT_OF_DISCHARGE_POID
            WHERE HDR.TRANSACTION_POID = :poid
            """;

    private static final Map<String, LovItem> CHARGE_TYPE_LOV = Map.of(
            "MANIFEST", new LovItem(1L, "MANIFEST", "MANIFESTED PRINCIPAL PAYABLE", "MANIFESTED PRINCIPAL PAYABLE", 1L, 0),
            "LOCAL", new LovItem(2L, "LOCAL", "LOCAL CHARGE", "LOCAL CHARGE", 2L, 0),
            "BOTH", new LovItem(3L, "BOTH", "BOTH", "BOTH", 3L, 0)
    );

    private static final Map<String, LovItem> SHIP_FREIGHT_TYPE_LOV = Map.of(
            "P", new LovItem(1L, "P", "PREPAID", "PREPAID", 1L, 0),
            "C", new LovItem(2L, "C", "COLLECT", "COLLECT", 2L, 0),
            "E", new LovItem(3L, "E", "ELSEWHERE", "ELSEWHERE", 3L, 0)
    );

    private static final Map<String, LovItem> CARGO_TYPE_LOV = Map.ofEntries(
            Map.entry("FCL-FCL", new LovItem(1L, "FCL-FCL", "FCL-FCL", "FCL-FCL", 1L, 0)),
            Map.entry("LCL-LCL", new LovItem(2L, "LCL-LCL", "LCL-LCL", "LCL-LCL", 2L, 0)),
            Map.entry("FCL_LCL", new LovItem(3L, "FCL_LCL", "FCL LCL", "FCL LCL", 3L, 0)),
            Map.entry("GENERAL", new LovItem(4L, "GENERAL", "GENERAL", "GENERAL", 4L, 0)),
            Map.entry("LCL-FCL", new LovItem(5L, "LCL-FCL", "LCL-FCL", "LCL-FCL", 5L, 0)),
            Map.entry("EMPTY", new LovItem(6L, "EMPTY", "EMPTY", "EMPTY", 6L, 0)),
            Map.entry("RORO", new LovItem(7L, "RORO", "RORO", "RORO", 7L, 0))
    );

    private static final Map<String, LovItem> BL_TYPE_IMPORT_LOV = Map.of(
            "IMPORT", new LovItem(1L, "IMPORT", "IMPORT", "IMPORT", 1L, 0),
            "SWITCH", new LovItem(3L, "SWITCH", "SWITCH", "SWITCH", 3L, 0),
            "CROSSTRADE", new LovItem(4L, "CROSSTRADE", "CROSSTRADE", "CROSSTRADE", 4L, 0)
    );

    // Same static set backs both BL_RELEASE_TYPE and BL_ISSUE_TYPE in the source LOV package.
    // Keyed by both the LOV's own CODE (OBL/EXPRESS/SEAWAY/OTHER) and its POID-as-string ("1".."4"):
    // BL_ISSUE_TYPE/BL_RELEASE_TYPE on the header entity is stored as the numeric id as text, not
    // the code text, so a code-only lookup never actually matched (the old lovService call had the
    // same mismatch — it silently returned an empty stub instead of a real match).
    private static final Map<String, LovItem> BL_ISSUE_TYPE_LOV = Map.ofEntries(
            Map.entry("OBL", new LovItem(1L, "OBL", "ORIGINAL BL REQUIRED", "ORIGINAL BL REQUIRED", 1L, 0)),
            Map.entry("1", new LovItem(1L, "OBL", "ORIGINAL BL REQUIRED", "ORIGINAL BL REQUIRED", 1L, 0)),
            Map.entry("EXPRESS", new LovItem(2L, "EXPRESS", "EXPRESS RELEASED", "EXPRESS RELEASED", 2L, 0)),
            Map.entry("2", new LovItem(2L, "EXPRESS", "EXPRESS RELEASED", "EXPRESS RELEASED", 2L, 0)),
            Map.entry("SEAWAY", new LovItem(3L, "SEAWAY", "SEAWAY BILL", "SEAWAY BILL", 3L, 0)),
            Map.entry("3", new LovItem(3L, "SEAWAY", "SEAWAY BILL", "SEAWAY BILL", 3L, 0)),
            Map.entry("OTHER", new LovItem(4L, "OTHER", "OTHER", "OTHER", 4L, 0)),
            Map.entry("4", new LovItem(4L, "OTHER", "OTHER", "OTHER", 4L, 0))
    );

    private static final Map<String, LovItem> SHIP_DO_ANOTICE_HOLD_LOV = Map.of(
            "1", new LovItem(1L, "1", "HOLD DO", "HOLD DO", 1L, 0),
            "4", new LovItem(4L, "4", "VERIFIED", "VERIFIED", 4L, 0),
            "5", new LovItem(5L, "5", "NOTVERIFIED", "NOTVERIFIED", 5L, 0),
            "6", new LovItem(6L, "6", "VERIFIED-ELSEWHERE", "VERIFIED-ELSEWHERE", 6L, 0)
    );

    private void enrichLovData(ImportManifestBlRequestDto dto) {
        if (dto == null) {
            return;
        }

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        enrichHeaderLovData(dto, groupPoid, companyPoid, userPoid);
        enrichGeneralCargoLovData(dto.getGeneralCargoDetails(), groupPoid, companyPoid, userPoid);
        enrichContainerLovData(dto.getContainers(), groupPoid, companyPoid, userPoid);
        enrichChargeLovData(dto.getChargeDetails(), groupPoid, companyPoid, userPoid);
        enrichPartBlLovData(dto.getPartBls(), groupPoid, companyPoid, userPoid);
    }

    private void enrichHeaderLovData(ImportManifestBlRequestDto dto, Long groupPoid, Long companyPoid,
            Long userPoid) {
        try {
            // Voyage stays on the LOV framework: VESSAL_VOYAGE applies group/company/user scoping
            // that a direct join would silently drop.
            if (dto.getVoyageTransactionPoid() != null) {
                dto.setVoyageTransactionPoidDet(
                        lovService.getLovItemByPoid(dto.getVoyageTransactionPoid(), "VESSAL_VOYAGE", groupPoid,
                                companyPoid, userPoid));
            }

            applyHeaderLovProjection(dto);

            if (dto.getCargoType() != null) {
                dto.setCargoTypeDet(CARGO_TYPE_LOV.get(dto.getCargoType().trim().toUpperCase()));
            }
            if (dto.getBlType() != null) {
                dto.setBlTypeDet(BL_TYPE_IMPORT_LOV.get(dto.getBlType().trim().toUpperCase()));
            }
            if (dto.getBlIssueType() != null) {
                dto.setBlIssueTypeDet(BL_ISSUE_TYPE_LOV.get(dto.getBlIssueType().trim().toUpperCase()));
            }
            if (dto.getHoldReason() != null) {
                dto.setHoldReasonDet(SHIP_DO_ANOTICE_HOLD_LOV.get(dto.getHoldReason().trim().toUpperCase()));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for header detail with transactionPoid: {}", dto.getTransactionPoid(),
                    e);
        }
    }

    private void applyHeaderLovProjection(ImportManifestBlRequestDto dto) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(HEADER_LOV_PROJECTION_SQL)
                .setParameter("poid", dto.getTransactionPoid()).getResultList();
        if (rows.isEmpty()) {
            return;
        }
        Object[] row = rows.get(0);
        dto.setQuotationTransactionDet(lovAt(row, 0));
        dto.setSalesmanDet(lovAt(row, 3));
        dto.setComodityDet(lovAt(row, 6));
        dto.setConsigneeDet(lovAt(row, 9));
        dto.setNotifyPoid1Det(lovAt(row, 12));
        dto.setBookingPartyDet(lovAt(row, 15));
        dto.setPlaceOfRecieptDet(lovAt(row, 18));
        dto.setPlaceOfDelieveryDet(lovAt(row, 21));
        dto.setPortOfLoadingDet(lovAt(row, 24));
        dto.setPortOfDischargeDet(lovAt(row, 27));
    }

    /**
     * Reads one (POID, CODE, DESCRIPTION) triple out of the projected row. A null poid means the
     * header column was null or the joined row is missing — both produced a null det before.
     */
    private static LovItem lovAt(Object[] row, int offset) {
        Object poid = row[offset];
        if (poid == null) {
            return null;
        }
        Long id = ((Number) poid).longValue();
        String code = row[offset + 1] != null ? row[offset + 1].toString() : null;
        String description = row[offset + 2] != null ? row[offset + 2].toString() : null;
        return new LovItem(id, code, description, description, id, 0);
    }

    private void enrichGeneralCargoLovData(List<GeneralCargoRequestDto> dtos,
            Long groupPoid, Long companyPoid, Long userPoid) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        try {
            Map<Long, LovItem> commodities = masterLovLookup.commoditiesByPoid(
                    poids(dtos, GeneralCargoRequestDto::getComodityPoid));
            Map<Long, LovItem> ports = masterLovLookup.portsByPoid(
                    poids(dtos, GeneralCargoRequestDto::getDestinationPortPoid));
            for (GeneralCargoRequestDto dto : dtos) {
                dto.setComodityDet(commodities.get(dto.getComodityPoid()));
                dto.setDestinationPortDet(ports.get(dto.getDestinationPortPoid()));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for general cargo details", e);
        }
    }

    private void enrichContainerLovData(List<ContainerRequestDto> dtos,
            Long groupPoid, Long companyPoid, Long userPoid) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        try {
            Map<Long, LovItem> commodities = masterLovLookup.commoditiesByPoid(
                    poids(dtos, ContainerRequestDto::getComodityPoid));
            Map<Long, LovItem> ports = masterLovLookup.portsByPoid(
                    poids(dtos, ContainerRequestDto::getDestinationPortPoid));
            Map<String, LovItem> isoTypes = masterLovLookup.containerTypesByCode(
                    codes(dtos, ContainerRequestDto::getEquipmentIsoType));
            Map<String, LovItem> imcoClasses = masterLovLookup.imcoClassesByCode(
                    codes(dtos, ContainerRequestDto::getImcoClassType));
            Map<String, LovItem> oogTypes = masterLovLookup.oogTypesByCode(
                    codes(dtos, ContainerRequestDto::getOogType));
            for (ContainerRequestDto dto : dtos) {
                dto.setComodityDet(commodities.get(dto.getComodityPoid()));
                dto.setDestinationPortDet(ports.get(dto.getDestinationPortPoid()));
                dto.setEquipmentIsoTypeDet(lookupByCode(isoTypes, dto.getEquipmentIsoType()));
                dto.setImcoClassTypeDet(lookupByCode(imcoClasses, dto.getImcoClassType()));
                dto.setOogTypeDet(lookupByCode(oogTypes, dto.getOogType()));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for container details", e);
        }
    }

    private void enrichChargeLovData(List<ChargeRequestDto> dtos,
            Long groupPoid, Long companyPoid, Long userPoid) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        try {
            Map<Long, LovItem> chargeMasters = masterLovLookup.chargeMastersByPoid(
                    poids(dtos, ChargeRequestDto::getChargePoid));
            Map<String, LovItem> currencies = masterLovLookup.currenciesByCode(
                    codes(dtos, ChargeRequestDto::getCurrencyCode));
            Map<String, LovItem> bases = masterLovLookup.basisByCode(
                    codes(dtos, ChargeRequestDto::getChargeBasisOn));
            Map<Long, LovItem> ports = masterLovLookup.portsByPoid(
                    poids(dtos, ChargeRequestDto::getPaidAtPortPoid));
            Map<Long, LovItem> taxes = masterLovLookup.taxesByPoid(
                    poids(dtos, ChargeRequestDto::getTaxPoid));
            Map<Long, LovItem> receiptInvoices = lovService.getLovItemsByPoids(
                    poids(dtos, ChargeRequestDto::getReceiptInvoicePoid), "MANIFEST_RECEIPT_INVOICE",
                    groupPoid, companyPoid, userPoid);

            for (ChargeRequestDto dto : dtos) {
                dto.setChargeDet(chargeMasters.get(dto.getChargePoid()));
                dto.setCurrencyCodeDet(lookupByCode(currencies, dto.getCurrencyCode()));
                dto.setBasisDet(lookupByCode(bases, dto.getChargeBasisOn()));
                dto.setPaidAtPortDet(ports.get(dto.getPaidAtPortPoid()));
                dto.setTaxDet(taxes.get(dto.getTaxPoid()));
                if (dto.getChargeType() != null) {
                    dto.setChargeTypeDet(CHARGE_TYPE_LOV.get(dto.getChargeType().toUpperCase()));
                }
                if (dto.getFreightType() != null) {
                    dto.setFreightTypeDet(SHIP_FREIGHT_TYPE_LOV.get(dto.getFreightType().toUpperCase()));
                }
                if (dto.getReceiptInvoicePoid() != null) {
                    // getLovItemByPoid used to return a poid-only stub when the LOV had no matching
                    // row; the batch form simply omits the key, so keep the stub for an equal shape.
                    LovItem receiptInvoiceDet = receiptInvoices.get(dto.getReceiptInvoicePoid());
                    dto.setReceiptInvoiceDet(receiptInvoiceDet != null ? receiptInvoiceDet
                            : new LovItem(dto.getReceiptInvoicePoid(), null, null, null, null, null));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for charge details", e);
        }
    }

    private void enrichPartBlLovData(List<PartBlRequestDto> dtos,
            Long groupPoid, Long companyPoid, Long userPoid) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        try {
            Map<Long, LovItem> commodities = masterLovLookup.commoditiesByPoid(
                    poids(dtos, PartBlRequestDto::getComodityPoid));
            Map<String, LovItem> containerParts = containerPartsByCode(
                    codes(dtos, PartBlRequestDto::getContainerNo));
            for (PartBlRequestDto dto : dtos) {
                dto.setComodityDet(commodities.get(dto.getComodityPoid()));
                dto.setContainerNoDet(lookupByCode(containerParts, dto.getContainerNo()));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for part BL details", e);
        }
    }

    /** Not master data — these are container rows of live manifests, so they are never cached. */
    private Map<String, LovItem> containerPartsByCode(List<String> codes) {
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LovItem> map = new HashMap<>();
        containerDtlRepository.findContainerPartLovByCodes(codes).forEach(row -> {
            Long poid = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String description = (String) row[2];
            map.putIfAbsent(code.toUpperCase(), new LovItem(poid, code, description, description, poid, 0));
        });
        return map;
    }

    /** Distinct, non-null poids for one column across a detail list — the key set for a batch lookup. */
    private static <T> List<Long> poids(List<T> rows, java.util.function.Function<T, Long> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /** Distinct, non-blank codes for one column across a detail list. */
    private static <T> List<String> codes(List<T> rows, java.util.function.Function<T, String> getter) {
        return rows.stream().map(getter).filter(c -> c != null && !c.isBlank())
                .map(String::trim).distinct().collect(Collectors.toList());
    }

    /** Batched code lookups are keyed by uppercase code; rows keep whatever casing they were saved with. */
    private static LovItem lookupByCode(Map<String, LovItem> map, String code) {
        return code == null || code.isBlank() ? null : map.get(code.trim().toUpperCase());
    }

    private void validateMandatoryFieldsForUpdate(ImportManifestBlUpdateDTO dto) {
        if (dto.getVoyageTransactionPoid() == null) {
            throw new ValidationException(BlManifestValidationMessages.VOYAGE_REQUIRED);
        }
        if (dto.getCargoType() == null) {
            throw new ValidationException(BlManifestValidationMessages.CARGO_TYPE_REQUIRED);
        }
        if (dto.getBlNumber() == null) {
            throw new ValidationException(BlManifestValidationMessages.BL_NUMBER_REQUIRED);
        }
        if (dto.getBlType() == null) {
            throw new ValidationException(BlManifestValidationMessages.BL_TYPE_REQUIRED);
        }
    }

    private String joinCargoDescriptionsByType(List<CargoDescriptionRequestDto> rows, String... types) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String result = rows.stream()
                .filter(row -> row.getDescriptionType() != null
                        && java.util.Arrays.stream(types).anyMatch(t -> t.equalsIgnoreCase(row.getDescriptionType())))
                .map(CargoDescriptionRequestDto::getCargoDescription)
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.joining(" "));
        return result.isEmpty() ? null : result;
    }

    private String resolveSimpleCargoDescription(ImportManifestBlUpdateDTO dto) {
        if (dto.getSimpleCargoDescription() != null && !dto.getSimpleCargoDescription().trim().isEmpty()) {
            return dto.getSimpleCargoDescription().trim();
        }
        return joinCargoDescriptionsByType(dto.getCargoDescriptions(), "DESC", "DESCRIPTION");
    }

    private String resolveSimpleCargoMarks(ImportManifestBlUpdateDTO dto) {
        if (dto.getSimpleCargoMarks() != null && !dto.getSimpleCargoMarks().trim().isEmpty()) {
            return dto.getSimpleCargoMarks().trim();
        }
        return joinCargoDescriptionsByType(dto.getCargoDescriptions(), "MARK", "MARKS");
    }

    private boolean hasCargoInputForType(ImportManifestBlUpdateDTO dto, String... types) {
        if (dto.getCargoDescriptions() != null) {
            boolean hasLegacyRows = dto.getCargoDescriptions().stream()
                    .anyMatch(row -> row.getDescriptionType() != null
                            && java.util.Arrays.stream(types)
                            .anyMatch(type -> type.equalsIgnoreCase(row.getDescriptionType())));
            if (hasLegacyRows) {
                return true;
            }
        }

        if (types.length == 0) {
            return false;
        }

        return switch (types[0].toUpperCase()) {
            case "DESC", "DESCRIPTION" -> dto.getSimpleCargoDescription() != null;
            case "MARK", "MARKS" -> dto.getSimpleCargoMarks() != null;
            default -> false;
        };
    }

    private void replaceSimpleCargoRows(Long transactionPoid, String text, String descriptionType,
            String docId, String docKeyPoid) {
        cargoDtlRepository.deleteByIdTransactionPoidAndIdDescriptionType(transactionPoid, descriptionType);

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Long nextDetRowId = cargoDtlRepository.getMaxDetRowId(transactionPoid) + 1;
        ShipBlManifestCargoDtl entity = new ShipBlManifestCargoDtl();
        entity.setId(new ShipBlManifestCargoDtlId(transactionPoid, nextDetRowId, descriptionType));
        entity.setCargoDescription(text.trim());
        cargoDtlRepository.save(entity);
        loggingService.createLogSummaryEntry(docId, docKeyPoid,
                String.format("Row Created on Cargo Description Detail with detRowId: %s", nextDetRowId));
    }

    private boolean hasAnyEdiChange(ImportManifestBlUpdateDTO dto) {
        return dto.getShipperEdiName() != null
                || dto.getConsigneeEdiName() != null
                || dto.getNotify1EdiName() != null
                || dto.getNotify2EdiName() != null
                || dto.getNotify3EdiName() != null;
    }

    private ShipBlManifestHdr findEntityById(Long transactionPoId) {
        return repository.findById(transactionPoId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Ship BL Manifest", "transactionPoId", transactionPoId));
    }

    private String resolveAction(String rawAction) {
        String action = (rawAction == null || rawAction.trim().isEmpty()) ? ACTION_NOCHANGES
                : rawAction.trim().toUpperCase();
        return switch (action) {
            case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }

    public void registerAfterCommitActions(ShipBlManifestHdr saved, Long groupPoid, Long companyPoid,
            String oldDoNo, String oldFreightStatus, String newDoNo, String newFreightStatus) {
        boolean callDoStatus = (newDoNo != null && !newDoNo.equals(oldDoNo))
                || (newFreightStatus != null
                        && !newFreightStatus.equals(oldFreightStatus));

        boolean hasManualTotals = saved.getTotalNetVolume() != null
                || saved.getTotalWeight() != null
                || saved.getTotalNetWeight() != null
                || saved.getTotalNoOfPacks() != null;

        Long transactionPoid = saved.getTransactionPoid();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        if (callDoStatus) {
                            updateDoBlStatus(transactionPoid, groupPoid, companyPoid);
                        }

                        if (!hasManualTotals) {
                            callAfterSaveProcedure(
                                    saved,
                                    groupPoid,
                                    companyPoid,
                                    "AUTOSUMWEIGHTPACKATE");
                        }
                    }
                });
    }
}
