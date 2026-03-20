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
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.entity.*;
import com.asg.shipping.importmanifestupdate.event.BlManifestSaveEvent;
import com.asg.shipping.importmanifestupdate.respository.*;
import com.asg.shipping.importmanifestupdate.util.ImportManifestBlMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

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
    private final BlManifestValidationRepository validationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImportManifestBlProcRepository procRepository;
    private final AddressDetailsRepository addressDetailsRepository;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    private static final String ACTION_NOCHANGES = "NOCHANGES";
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";


    @Override
    @Transactional
    public ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto dto) {
        log.info("Creating new Import Manifest BL");

        validateMandatoryFields(dto);
        validateCreateDTO(dto);
        validateHoldReasons(dto);
        validateAddresses(dto);
        validateContainers(dto);
        validateFinancial(dto);
        validateFreightType(dto);
        validateDemurrage(dto);


        ShipBlManifestHdr entity = mapper.mapToEntity(dto);
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setGroupPoid(UserContext.getGroupPoid());
        autoPopulateDefaults(entity);
        formatEdiFields(entity);

        ShipBlManifestHdr saved = repository.saveAndFlush(entity);
        Long transactionPoid = saved.getTransactionPoid();
        procRepository.validateBeforeSave(dto, transactionPoid);
        log.info("BL Manifest header saved with transactionPoid: {}", transactionPoid);
        ImportManifestBlUpdateDTO createDto = new ImportManifestBlUpdateDTO();
        createDto.setGeneralCargoDetails(dto.getGeneralCargoDetails());
        createDto.setCargoDescriptions(dto.getCargoDescriptions());
        createDto.setContainers(dto.getContainers());
        createDto.setChargeDetails(dto.getChargeDetails());
        createDto.setPartBls(dto.getPartBls());
        createDto.setNotifyParties(dto.getNotifyParties());
        createDto.setMafiDetails(dto.getMafiDetails());
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), transactionPoid.toString());

        saveDetailTables(createDto, transactionPoid);
        log.info("Detail tables saved for transactionPoid: {}", transactionPoid);

        ImportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, transactionPoid);
        eventPublisher.publishEvent(new BlManifestSaveEvent(saved, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), "AUTOSUMWEIGHTPACKATE"));

        log.info("Successfully created Import Manifest BL with id: {}", transactionPoid);
        return result;
    }

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
        validateBeforeSave(dto, id, companyPoid, groupPoid);
        validateUpdateDTO(dto, id, companyPoid, groupPoid);
        String oldFreightStatus = existingEntity.getFreightStatus();
        String oldDoNo = existingEntity.getDoNo();
        ShipBlManifestHdr savedEntity =  mapper.mapUpdateDTOToEntity(dto, existingEntity);
        if (hasAnyEdiChange(dto)) {
            formatEdiFields(savedEntity);
        }
        ShipBlManifestHdr saved = repository.saveAndFlush(savedEntity);
        updateDetailTables(dto, saved.getTransactionPoid());
        loggingService.logChanges(oldEntity, savedEntity, ShipBlManifestHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        boolean callDoStatus =
                (dto.getDoNo() != null && !dto.getDoNo().equals(oldDoNo))
                        || (dto.getFreightStatus() != null
                        && !dto.getFreightStatus().equals(oldFreightStatus));

        Long transactionPoid = saved.getTransactionPoid();

        //Register AFTER COMMIT actions
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

        ImportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, transactionPoid);


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
    public ResendCanResponseDto resendCan(Long transactionPoId) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            return procRepository.resendCan(entity.getVoyageTransactionPoid(), transactionPoId);
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
    public LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request) {
        try {
            findEntityById(transactionPoId);
            var addressDetails = addressDetailsRepository.findByAddressMasterPoidAndAddressType(
                    request.getAddressMasterPoid(), "CAN");
            var emailFaxDetails = addressDetails.stream()
                    .map(ad -> EmailFaxDetailDto.builder()
                            .addressPoid(Long.valueOf(ad.getAddressPoid()))
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .fax(ad.getFax())
                            .addressType(request.getAddressType())
                            .build())
                    .toList();
            log.info("Loaded email/fax data for transactionPoId: {}, count: {}", transactionPoId, emailFaxDetails.size());
            return LoadEmailFaxResponseDto.builder().emailFaxDetails(emailFaxDetails).build();
        } catch (ResourceNotFoundException e) {
            log.error("Failed to load email/fax: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error loading email/fax data for transactionPoId: {}", transactionPoId, e);
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
     * PROC_SHIP_VALD_BEFORE_SAVE - Validation before save (update)
     */
    private void validateBeforeSave(ImportManifestBlUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        try {
            Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();
            Long transactionPoid = id;
            Long voyageTransactionPoid = dto.getVoyageTransactionPoid();
            String validationType = "VLD_QUOTATION";

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
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            query.setParameter("P_VOYAGE_TRANSACTION_POID", voyageTransactionPoid);
            query.setParameter("P_VALIDATION_TYPE", validationType);

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");
            if (result != null && !result.equalsIgnoreCase("TRUE")) {
                throw new ValidationException("Validation failed: " + result);
            }
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_VALD_BEFORE_SAVE", e);
            throw new ValidationException("Validation error: " + e.getMessage());
        }
    }

    private void validateUpdateDTO(ImportManifestBlUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        // Validate BL number uniqueness per voyage (excluding current record)
        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().isEmpty()) {
            Long voyagePoid = dto.getVoyageTransactionPoid();
            if (voyagePoid != null) {
                if (repository.existsByVoyageTransactionPoidAndBlNumberExcludingPoid(voyagePoid, dto.getBlNumber().trim(), id)) {
                    throw new ValidationException("BL number already exists for this voyage");
                }
            }
            // Validate global BL number uniqueness (excluding current record)
            if (repository.existsByBlNumberExcludingPoid(dto.getBlNumber().trim(), id)) {
                throw new ValidationException("BL number already exists");
            }
        }

        // Validate port of loading for non-EXPORT BL_TYPE
        if (dto.getBlType() != null && !"EXPORT".equals(dto.getBlType()) && dto.getPortOfLoadingPoid() == null) {
            // Check if existing record has port of loading
            ShipBlManifestHdr existing = repository.findById(id).orElse(null);
            if (existing == null || existing.getPortOfLoadingPoid() == null) {
                throw new ValidationException("Port of loading is required for non-EXPORT BL type");
            }
        }
    }

    private void formatEdiFields(ShipBlManifestHdr entity) {
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
        String currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestGeneralDtl existing = generalDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("General Cargo Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestGeneralDtl oldEntity = new ShipBlManifestGeneralDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateGeneralFromDto(detailDto, existing);
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                generalDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on General Cargo Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
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
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                cargoDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on Cargo Description Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
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
                                                    "Container number already exists: " + existing.getContainerNo()
                                            );
                                        }
                                    });
                        }

                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                containerDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on Container Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestChargesDtl existing = chargesDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestChargesDtl oldEntity = new ShipBlManifestChargesDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateChargesFromDto(detailDto, existing);
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                chargesDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on Charge Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestPartBL existing = containerPrtRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Part BL Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestPartBL oldEntity = new ShipBlManifestPartBL();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updatePartBlFromDto(detailDto, existing);
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                containerPrtRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on Part BL Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestEmailFaxDtl existing = emailFaxDtlRepository.findById(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(), detailDto.getAddressType()))
                                .orElseThrow(() -> new ResourceNotFoundException("Notify Party Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestEmailFaxDtl oldEntity = new ShipBlManifestEmailFaxDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateEmailFaxFromDto(detailDto, existing);
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                emailFaxDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on Notify Party Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
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
                        entity.setCreatedBy(currentUser);
                        entity.setCreatedDate(now);
                        toSave.add(entity);
                    }
                    case ACTION_ISUPDATED -> {
                        ShipBlManifestMafiDtl existing = mafiDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                                .orElseThrow(() -> new ResourceNotFoundException("MAFI Detail", "detRowId", detailDto.getDetRowId()));

                        ShipBlManifestMafiDtl oldEntity = new ShipBlManifestMafiDtl();
                        BeanUtils.copyProperties(existing, oldEntity);

                        mapper.updateMafiFromDto(detailDto, existing);
                        existing.setLastModifiedBy(currentUser);
                        existing.setLastModifiedDate(now);
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
                mafiDtlRepository.deleteAllById(toDelete);
                toDelete.forEach(id -> {
                    String logDetail = String.format("Row Deleted on MAFI Detail with detRowId: %s", id.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
                });
            }
        }


    }


    /**
     * Update all detail tables for a transaction (update)
     */
    private void saveDetailTables(ImportManifestBlUpdateDTO dto, Long transactionPoid) {
        List<String> logEntries = new ArrayList<>();

        if (dto.getGeneralCargoDetails() != null) {
            Long detRowId =
                    getNextDetRowId(generalDtlRepository.getMaxDetRowId(transactionPoid));

            for (GeneralCargoRequestDto detailDto : dto.getGeneralCargoDetails()) {

                ShipBlManifestGeneralDtl entity =
                        mapper.mapGeneralDtlFromDto(detailDto, transactionPoid);

                entity.getId().setDetRowId(detRowId);
                generalDtlRepository.save(entity);

                logEntries.add(
                        String.format(
                                "Row Created on General Cargo Detail with DetRowId: %s",
                                detRowId
                        )
                );

                detRowId++;
            }
        }
        if (dto.getCargoDescriptions() != null) {
            Long detRowId =
                    getNextDetRowId(cargoDtlRepository.getMaxDetRowId(transactionPoid));

            for (CargoDescriptionRequestDto detailDto : dto.getCargoDescriptions()) {

                ShipBlManifestCargoDtl entity =
                        mapper.mapCargoDtlFromDto(detailDto, transactionPoid);

                entity.getId().setDetRowId(detRowId);
                cargoDtlRepository.save(entity);

                logEntries.add(
                        String.format(
                                "Row Created on Cargo Description Detail with DetRowId: %s",
                                detRowId
                        )
                );

                detRowId++;
            }
        }

        if (dto.getContainers() != null) {
            Long detRowId =
                    getNextDetRowId(containerDtlRepository.getMaxDetRowId(transactionPoid));
            for (ContainerRequestDto detailDto : dto.getContainers()) {

                ShipBlManifestContainerDtl entity = mapper.mapContainerDtlFromDto(detailDto, transactionPoid);
                entity.getId().setDetRowId(detRowId);
                containerDtlRepository.save(entity);
                logEntries.add(String.format("Row Created on Container Detail with DetRowId: %s", detRowId));
                detRowId++;
            }
        }

        if (CollectionUtils.isNotEmpty(dto.getChargeDetails())) {

            Long nextDetRowId =
                    getNextDetRowId(chargesDtlRepository.getMaxDetRowId(transactionPoid));

            for (ChargeRequestDto detailDto : dto.getChargeDetails()) {

                Long detRowId =
                        detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

                ShipBlManifestChargesDtl entity =
                        mapper.mapChargesDtlFromDto(detailDto, transactionPoid);

                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }

                chargesDtlRepository.save(entity);
                logEntries.add(
                        String.format("Row Created on Charge Detail with DetRowId: %s", detRowId)
                );
            }
        }


        if (CollectionUtils.isNotEmpty(dto.getPartBls())) {

            Long nextDetRowId =
                    getNextDetRowId(containerPrtRepository.getMaxDetRowId(transactionPoid));

            for (PartBlRequestDto detailDto : dto.getPartBls()) {

                Long detRowId =
                        detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

                ShipBlManifestPartBL entity =
                        mapper.mapContainerPrtFromDto(detailDto, transactionPoid);

                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }

                containerPrtRepository.save(entity);
                logEntries.add(
                        String.format("Row Created on Part BL Detail with DetRowId: %s", detRowId)
                );
            }
        }


        if (CollectionUtils.isNotEmpty(dto.getNotifyParties())) {

            Long nextDetRowId =
                    getNextDetRowId(emailFaxDtlRepository.getMaxDetRowId(transactionPoid));

            for (NotifyPartyRequestDto detailDto : dto.getNotifyParties()) {

                Long detRowId =
                        detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

                ShipBlManifestEmailFaxDtl entity =
                        mapper.mapEmailFaxDtlFromDto(detailDto, transactionPoid);

                if (entity.getId() == null) {
                    entity.setId(
                            new ShipBlManifestEmailFaxId(
                                    transactionPoid,
                                    detRowId,
                                    detailDto.getAddressType()
                            )
                    );
                } else {
                    entity.getId().setDetRowId(detRowId);
                }

                emailFaxDtlRepository.save(entity);
                logEntries.add(
                        String.format("Row Created on Notify Party Detail with DetRowId: %s", detRowId)
                );
            }
        }


        if (CollectionUtils.isNotEmpty(dto.getMafiDetails())) {

            Long nextDetRowId =
                    getNextDetRowId(mafiDtlRepository.getMaxDetRowId(transactionPoid));

            for (MafiRequestDto detailDto : dto.getMafiDetails()) {

                Long detRowId =
                        detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

                ShipBlManifestMafiDtl entity =
                        mapper.mapMafiDtlFromDto(detailDto, transactionPoid);

                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }

                mafiDtlRepository.save(entity);
                logEntries.add(
                        String.format("Row Created on MAFI Detail with DetRowId: %s", detRowId)
                );
            }
        }
        // Batch log all entries at once to optimize database calls
        for (String logDetail : logEntries) {
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
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
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARAM1", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PARAM2", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_TRANSACTION_POID", transactionPoid);
            query.setParameter("P_PARAM1", null);
            query.setParameter("P_PARAM2", processType);
            query.setParameter("P_LOGIN_USER_POID", userPoid);

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

    private Long getNextDetRowId(Long maxDetRowId) {
        return (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
    }


    private void validateMandatoryFields(ImportManifestBlCreateDto dto) {
        if (dto.getVoyageTransactionPoid() == null) {
            throw new ValidationException("Voyage number is required");
        }
        if (dto.getCargoType() == null) {
            throw new ValidationException("Cargo Type is required");
        }
        if (dto.getBlNumber() == null) {
            throw new ValidationException("BL number is required");
        }
        if (dto.getBlType() == null) {
            throw new ValidationException("BL Type is required");
        }
    }

    private void validateMandatoryFieldsForUpdate(ImportManifestBlUpdateDTO dto) {
        if (dto.getVoyageTransactionPoid() == null) {
            throw new ValidationException("Voyage number is required");
        }
        if (dto.getCargoType() == null) {
            throw new ValidationException("Cargo Type is required");
        }
        if (dto.getBlNumber() == null) {
            throw new ValidationException("BL number is required");
        }
        if (dto.getBlType() == null) {
            throw new ValidationException("BL Type is required");
        }
    }

    private void validateCreateDTO(ImportManifestBlCreateDto dto) {

        validateVoyageCompany(dto.getVoyageTransactionPoid());

        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().isEmpty()) {
            if (repository.existsByVoyageTransactionPoidAndBlNumber(dto.getVoyageTransactionPoid(), dto.getBlNumber().trim())) {
                throw new ValidationException("BL number already exists for this voyage");
            }
        }

        if (dto.getPortOfLoadingPoid() == null) {
            throw new ValidationException("Port of loading is required for IMPORT BL");
        }

        validateFinancialYear(UserContext.getCompanyPoid(), dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
    }


    private void validateFinancialYear(Long companyPoid, LocalDateTime transactionDate) {
        if (!validationRepository.isValidFinancialYear(companyPoid, transactionDate)) {
            log.error("Validation failed: Invalid financial year for company {} on date {}", companyPoid, transactionDate);
        }
    }

    private void validateVoyageCompany(Long voyageTransactionPoid) {
        Long voyageCompanyPoid = validationRepository.getVoyageCompanyPoid(voyageTransactionPoid);
        if (voyageCompanyPoid == null || !voyageCompanyPoid.equals(UserContext.getCompanyPoid())) {
            log.error("Validation failed: Voyage company mismatch. Voyage company: {}, User company: {}", voyageCompanyPoid, UserContext.getCompanyPoid());
            throw new ValidationException("Voyage not found or company mismatch");
        }
    }


    private void autoPopulateDefaults(ShipBlManifestHdr entity) {
        if (entity.getBlType() == null) entity.setBlType("IMPORT");
        if (entity.getCargoType() == null) entity.setCargoType("FCL-FCL");
        if (entity.getBlIssueType() == null) entity.setBlIssueType("1");
        if (entity.getPortOfDischargePoid() == null) entity.setPortOfDischargePoid(800L);
        if (entity.getPlaceOfDeliveryPoid() == null) entity.setPlaceOfDeliveryPoid(800L);
        if (entity.getHoldReason() == null) entity.setHoldReason("5");
        if (entity.getFreightStatus() == null) entity.setFreightStatus("1");
        if (entity.getSalesmanPoid() == null || entity.getSalesmanPoid() == 1) entity.setSalesmanPoid(51L);
        if (entity.getComodityPoid() == null) entity.setComodityPoid(10L);
        if (entity.getBookedByPp() == null) entity.setBookedByPp("Y");
    }


    private void validateHoldReasons(ImportManifestBlCreateDto dto) {

        if (dto.getHoldReason() != null &&
                Set.of("1", "2", "3").contains(dto.getHoldReason()) &&
                (dto.getHoldRemarks() == null || dto.getHoldRemarks().trim().isEmpty())) {

            log.error("Validation failed: Hold reason {} requires remarks", dto.getHoldReason());
            throw new ValidationException("Please check Hold Remarks field");
        }

        if ("Y".equalsIgnoreCase(dto.getHoldCanDo()) &&
                (dto.getHoldRemarks() == null || dto.getHoldRemarks().trim().isEmpty())) {

            log.error("Validation failed: Hold CAN/DO requires remarks");
            throw new ValidationException("Please check Hold Remarks field");
        }
    }

    private void validateAddresses(ImportManifestBlCreateDto dto) {

        String holdReason = dto.getHoldReason() != null ? dto.getHoldReason() : "5";

        if (!"5".equalsIgnoreCase(holdReason)) {

            if (dto.getConsigneePoid() == null || dto.getConsigneePoid().equals(1L)) {
                log.error("Validation failed: Invalid Consignee POID");
                throw new ValidationException("Invalid Consignee POID");
            }

            if (dto.getNotifyPoid1() == null || dto.getNotifyPoid1().equals(1L)) {
                log.error("Validation failed: Invalid Notify POID");
                throw new ValidationException("Invalid Notify POID");
            }

            boolean addressFound = checkAddressesExist(dto);

            String manuallyCanSend = dto.getManuallyCanSend();

            if (!addressFound &&
                    (manuallyCanSend == null || "N".equalsIgnoreCase(manuallyCanSend))) {

                log.error("Validation failed: No address selected for CAN");
                throw new ValidationException("No address selected for CAN.");
            }
        }
    }



    private boolean checkAddressesExist(ImportManifestBlCreateDto dto) {
        if (dto.getNotifyParties() != null && !dto.getNotifyParties().isEmpty()) {
            for (NotifyPartyRequestDto party : dto.getNotifyParties()) {
                if (party.getSendYesNo() != null && "Y".equals(party.getSendYesNo())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateContainers(ImportManifestBlCreateDto dto) {
        if (dto.getContainers() != null && !dto.getContainers().isEmpty()) {
            for (ContainerRequestDto container : dto.getContainers()) {
                if (container.getContainerNo() == null || container.getContainerNo().trim().isEmpty() ||
                    container.getEquipmentIsoType() == null || container.getEquipmentIsoType().trim().isEmpty()) {
                    log.error("Validation failed: Container {} missing required fields", container.getContainerNo());
                    throw new ValidationException("Containerno / EquipmentIsotype must enter....");
                }
            }
        }
    }

    private void validateFinancial(ImportManifestBlCreateDto dto) {
        java.math.BigDecimal totalGain = calculateTotalGain(dto.getChargeDetails());
        if (totalGain.compareTo(java.math.BigDecimal.ZERO) < 0) {
            log.error("Validation failed: Total gain is negative: {}", totalGain);
            throw new ValidationException("Total gain is in negetive.." + totalGain);
        }

        if (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty()) {
            for (ChargeRequestDto charge : dto.getChargeDetails()) {
                if (charge.getChargePoid() == null || charge.getChargePoid() <= 0) {
                    log.error("Validation failed: Invalid charge POID");
                    throw new ValidationException("Charge POID is required and must be valid");
                }
            }
        }
    }

    private java.math.BigDecimal calculateTotalGain(List<ChargeRequestDto> chargeDetails) {
        if (chargeDetails == null || chargeDetails.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal totalGain = java.math.BigDecimal.ZERO;
        for (ChargeRequestDto charge : chargeDetails) {
            java.math.BigDecimal saleAmount = charge.getPerQuantityAmount() != null ?
                    java.math.BigDecimal.valueOf(charge.getPerQuantityAmount().doubleValue()) : java.math.BigDecimal.ZERO;
            java.math.BigDecimal buyAmount = charge.getBuyPercharge() != null ?
                    java.math.BigDecimal.valueOf(charge.getBuyPercharge().doubleValue()) : java.math.BigDecimal.ZERO;
            totalGain = totalGain.add(saleAmount.subtract(buyAmount));
        }
        return totalGain;
    }

    private void validateFreightType(ImportManifestBlCreateDto dto) {
        String holdReason = dto.getHoldReason() != null ? dto.getHoldReason() : "999";
        String globalFreightType = determineGlobalFreightType(dto.getChargeDetails());

        if ("XX".equals(globalFreightType) && !"5".equals(holdReason)) {
            log.error("Validation failed: Freight type not entered");
            throw new ValidationException("Freight not enter..");
        }

        if ("1".equals(dto.getFreightStatus()) &&
                !"P".equals(globalFreightType) &&
                !"XX".equals(globalFreightType) &&
                !"5".equals(holdReason)) {
            log.error("Validation failed: Freight status 1 mismatch with type {}", globalFreightType);
            throw new ValidationException("Freight status mismatch..");
        }

        if ("2".equals(dto.getFreightStatus()) &&
                !"C".equals(globalFreightType) &&
                !"XX".equals(globalFreightType) &&
                !"5".equals(holdReason)) {
            log.error("Validation failed: Freight status 2 mismatch with type {}", globalFreightType);
            throw new ValidationException("Freight status mismatch...");
        }

        if ("3".equals(dto.getFreightStatus()) &&
                !"E".equals(globalFreightType) &&
                !"XX".equals(globalFreightType) &&
                !"5".equals(holdReason)) {
            log.error("Validation failed: Freight status 3 mismatch with type {}", globalFreightType);
            throw new ValidationException("Freight status mismatch....");
        }
    }

    private String determineGlobalFreightType(List<ChargeRequestDto> chargeDetails) {
        if (chargeDetails == null || chargeDetails.isEmpty()) {
            return "XX";
        }

        String freightType = null;
        for (ChargeRequestDto charge : chargeDetails) {
            if (charge.getFreightType() != null && !charge.getFreightType().trim().isEmpty()) {
                if (freightType == null) {
                    freightType = charge.getFreightType();
                } else if (!freightType.equals(charge.getFreightType())) {
                    return "XX";
                }
            }
        }
        return freightType != null ? freightType : "XX";
    }

    private void validateDemurrage(ImportManifestBlCreateDto dto) {
        if (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty()) {
            for (ChargeRequestDto charge : dto.getChargeDetails()) {
                if (charge.getChargePoid() != null && charge.getChargePoid().equals(94L)) {
                    log.error("Validation failed: Demurrage charge code 94 not allowed");
                    throw new ValidationException("Use Manifested Demmurage code (DEMMF)...");
                }
            }
        }
    }

    private void validateFinancialYear(LocalDate transactionDate) {
        if (transactionDate != null && transactionDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date cannot be in future");
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
}
