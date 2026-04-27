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
import java.util.List;
import java.util.Map;
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
        loggingService.logChanges(oldEntity, savedEntity, ShipBlManifestHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Register AFTER COMMIT actions (legacy DocumentAfterSave)
        registerAfterCommitActions(saved, groupPoid, companyPoid, oldDoNo, oldFreightStatus, dto.getDoNo(), dto.getFreightStatus());

        ImportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, id);

        log.info("Successfully updated Import Manifest BL with id: {}", id);
        return getImportManifestBl(id);
    }


    @Override
    @Transactional
    public ImportManifestBlRequestDto getImportManifestBl(Long id) {
        log.info("Getting Import Manifest BL with id: {}", id);

        ShipBlManifestHdr entity = repository.findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import Manifest BL", "transactionPoid", id.toString()));

        ImportManifestBlRequestDto dto = mapper.mapToDto(entity);

        loadDetailTables(dto, id);

        log.info("Successfully retrieved Import Manifest BL with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public void deleteImportManifestBl(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting Import Manifest BL with id: {}", id);

        ShipBlManifestHdr entity = repository.findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import Manifest BL", "transactionPoid", id.toString()));

        LocalDate transactionDate = entity.getTransactionDate() == null
                ? null
                : LocalDate.from(entity.getTransactionDate());

        documentDeleteService.deleteDocument(
                id,
                "SHIP_BL_MANIFEST_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                transactionDate
        );

        log.info("Successfully deleted Import Manifest BL with id: {}", id);
    }

    @Override
    public Map<String, Object> listOfImportManifest(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "BL_NUMBER",   // label
                "TRANSACTION_POID");  // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request) {
        try {
            findEntityById(transactionPoId);
            return procRepository.updateEmailVerification(transactionPoId, request);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to update: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error updating email verification for transactionPoId: {}", transactionPoId, e);
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
    public SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.getEdiEmails(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to send EDI emails: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error sending EDI emails for transactionPoId: {}", transactionPoId, e);
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
                            .addressPoid(ad.getAddressPoid())
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .fax(ad.getFax())
                            .addressType(addressType)
                            .build())
                    .toList();
            log.info("Loaded email/fax data for addressMasterPoid: {}, count: {}", addressMasterPoid, emailFaxDetails.size());
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
        if (containers == null) return List.of();
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
        if (charges == null) return List.of();
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
        //            if (bookedByPP != null && bookedByPP == "N") -> error
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
                if (repository.existsByVoyageTransactionPoidAndBlNumberExcludingPoid(voyagePoid, dto.getBlNumber().trim(), id)) {
                    throw new ValidationException(String.format(BlManifestValidationMessages.BL_NUMBER_EXISTS_FOR_VOYAGE, dto.getBlNumber().trim()));
                }
            }
            // Validate global BL number uniqueness (excluding current record)
            if (repository.existsByBlNumberExcludingPoid(dto.getBlNumber().trim(), id)) {
                throw new ValidationException(String.format(BlManifestValidationMessages.BL_NUMBER_EXISTS, dto.getBlNumber().trim()));
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
                        entity.setId(new ShipBlManifestDtlId(transactionPoid, maxDetRowId++));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestGeneralDtl existing = generalDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("General Cargo Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestGeneralDtl oldEntity = new ShipBlManifestGeneralDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateGeneralFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestGeneralDtl.class, docId, docKeyPoid, logDetail));
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
                    String logDetail = String.format("Row Created on General Cargo Detail with detRowId: %s", e.getId().getDetRowId());
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
        if (dto.getCargoDescriptions() != null) {
            List<ShipBlManifestCargoDtl> toSave = new ArrayList<>();
            List<ShipBlManifestCargoDtl> toUpdate = new ArrayList<>();
            List<ShipBlManifestCargoDtlId> toDelete = new ArrayList<>();
            List<LogRequestDto<ShipBlManifestCargoDtl>> logRequests = new ArrayList<>();

            Long maxDetRowId = cargoDtlRepository.getMaxDetRowId(transactionPoid);

            for (CargoDescriptionRequestDto detailDto : dto.getCargoDescriptions()) {
                String action = resolveAction(detailDto.getActionType());
                switch (action) {
                    case ACTION_NOCHANGES -> {
                    }
                    case ACTION_ISCREATED -> {
                        ShipBlManifestCargoDtl entity = mapper.mapCargoDtlFromDto(detailDto, transactionPoid);
                        entity.setId(new ShipBlManifestCargoDtlId(transactionPoid, ++maxDetRowId, detailDto.getDescriptionType()));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestCargoDtl existing = cargoDtlRepository.findById(new ShipBlManifestCargoDtlId(transactionPoid, detailDto.getDetRowId(), detailDto.getDescriptionType()))
                                .orElseThrow(() -> new ResourceNotFoundException("Cargo Description Detail", "detRowId", detailDto.getDetRowId()));
                        ShipBlManifestCargoDtl oldEntity = new ShipBlManifestCargoDtl();
                        BeanUtils.copyProperties(existing, oldEntity);
                        if (detailDto.getCargoDescription() != null)
                            existing.setCargoDescription(detailDto.getCargoDescription());
                        if (detailDto.getRecordOrder() != null) existing.setRecordOrder(detailDto.getRecordOrder());
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestCargoDtl.class, docId, docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestCargoDtlId(transactionPoid, detailDto.getDetRowId(), detailDto.getDescriptionType()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestCargoDtl> saved = cargoDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Cargo Description Detail with detRowId: %s", e.getId().getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }

            if (!toUpdate.isEmpty()) {
                cargoDtlRepository.saveAll(toUpdate);
                if (!logRequests.isEmpty()) {
                    loggingService.createLogBatch(logRequests);
                }

            }

            if (!toDelete.isEmpty()) {
                List<ShipBlManifestCargoDtl> entitiesToDelete = cargoDtlRepository.findAllById(toDelete);
                cargoDtlRepository.deleteAllInBatch(entitiesToDelete);
                entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
            }
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

                        ShipBlManifestContainerDtl existing =
                                containerDtlRepository.findById(
                                        new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId())
                                ).orElseThrow(() -> new ResourceNotFoundException(
                                        "Container Detail",
                                        "detRowId",
                                        detailDto.getDetRowId()
                                ));

                        ShipBlManifestContainerDtl oldEntity = new ShipBlManifestContainerDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateContainerFromDto(detailDto, existing);

                        if (existing.getContainerNo() != null && !existing.getContainerNo().trim().isEmpty()) {

                            containerDtlRepository
                                    .findByIdTransactionPoidAndContainerNo(
                                            transactionPoid,
                                            existing.getContainerNo().trim()
                                    )
                                    .ifPresent(conflict -> {
                                        if (!conflict.getId().getDetRowId()
                                                .equals(existing.getId().getDetRowId())) {

                                            throw new ValidationException(
                                                    String.format(BlManifestValidationMessages.CONTAINER_DUPLICATE, existing.getContainerNo())
                                            );
                                        }
                                    });
                        }

                        toUpdate.add(existing);

                        String logDetail = String.format(
                                "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s",
                                transactionPoid,
                                detailDto.getDetRowId()
                        );

                        logRequests.add(
                                new LogRequestDto<>(
                                        oldEntity,
                                        existing,
                                        ShipBlManifestContainerDtl.class,
                                        docId,
                                        docKeyPoid,
                                        logDetail
                                )
                        );
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
                    String logDetail = String.format("Row Created on Container Detail with detRowId: %s", e.getId().getDetRowId());
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
                        ShipBlManifestChargesDtl existing = chargesDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestChargesDtl oldEntity = new ShipBlManifestChargesDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateChargesFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestChargesDtl.class, docId, docKeyPoid, logDetail));
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
                    String logDetail = String.format("Row Created on Charge Detail with detRowId: %s", e.getId().getDetRowId());
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
                        ShipBlManifestPartBL existing = containerPrtRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Part BL Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestPartBL oldEntity = new ShipBlManifestPartBL();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updatePartBlFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestPartBL.class, docId, docKeyPoid, logDetail));
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
                    String logDetail = String.format("Row Created on Part BL Detail with detRowId: %s", e.getId().getDetRowId());
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
                        entity.setId(new ShipBlManifestEmailFaxId(transactionPoid, ++maxDetRowId, detailDto.getAddressType()));
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestEmailFaxDtl existing = emailFaxDtlRepository.findById(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(), detailDto.getAddressType()))
                                .orElseThrow(() -> new ResourceNotFoundException("Notify Party Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestEmailFaxDtl oldEntity = new ShipBlManifestEmailFaxDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateEmailFaxFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestEmailFaxDtl.class, docId, docKeyPoid, logDetail));
                    }
                    case ACTION_ISDELETED -> {
                        if (detailDto.getDetRowId() != null) {
                            toDelete.add(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(), detailDto.getAddressType()));
                        }
                    }
                }
            }

            if (!toSave.isEmpty()) {
                List<ShipBlManifestEmailFaxDtl> saved = emailFaxDtlRepository.saveAll(toSave);
                saved.forEach(e -> {
                    String logDetail = String.format("Row Created on Notify Party Detail with detRowId: %s", e.getId().getDetRowId());
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
                        ShipBlManifestMafiDtl existing = mafiDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("MAFI Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestMafiDtl oldEntity = new ShipBlManifestMafiDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateMafiFromDto(detailDto, existing);
                        toUpdate.add(existing);

                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detailDto.getDetRowId());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestMafiDtl.class, docId, docKeyPoid, logDetail));
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
                    String logDetail = String.format("Row Created on MAFI Detail with detRowId: %s", e.getId().getDetRowId());
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
     * Parameters: P_LOGIN_GROUP_POID, P_LOGIN_COMPANY_POID, P_LOGIN_USER_POID, P_TRANSACTION_POID, P_RESULT (OUT VARCHAR)
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
     * Parameters: P_LOGIN_GROUP_POID, P_LOGIN_COMPANY_POID, P_TRANSACTION_POID, P_PARAM1, P_PARAM2, P_LOGIN_USER_POID
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
            // Don't throw exception - post-save processing should not fail the save operation
        }
    }

    /**
     * Load all detail tables for a transaction
     */
    public ImportManifestBlRequestDto loadDetailTables(ImportManifestBlRequestDto dto, Long transactionPoid) {
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
        return dto;
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

    private boolean hasAnyEdiChange(ImportManifestBlUpdateDTO dto) {
        return dto.getShipperEdiName() != null
                || dto.getConsigneeEdiName() != null
                || dto.getNotify1EdiName() != null
                || dto.getNotify2EdiName() != null
                || dto.getNotify3EdiName() != null;
    }

    private ShipBlManifestHdr findEntityById(Long transactionPoId) {
        return repository.findById(transactionPoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship BL Manifest", "transactionPoId", transactionPoId));
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

    public void registerAfterCommitActions(ShipBlManifestHdr saved, Long groupPoid, Long companyPoid,
                                            String oldDoNo, String oldFreightStatus, String newDoNo, String newFreightStatus) {
        boolean callDoStatus =
                (newDoNo != null && !newDoNo.equals(oldDoNo))
                        || (newFreightStatus != null
                        && !newFreightStatus.equals(oldFreightStatus));

        Long transactionPoid = saved.getTransactionPoid();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        if (callDoStatus) {
                            updateDoBlStatus(transactionPoid, groupPoid, companyPoid);
                        }

                        callAfterSaveProcedure(
                                saved,
                                groupPoid,
                                companyPoid,
                                "AUTOSUMWEIGHTPACKATE"
                        );
                    }
                }
        );
    }
}
