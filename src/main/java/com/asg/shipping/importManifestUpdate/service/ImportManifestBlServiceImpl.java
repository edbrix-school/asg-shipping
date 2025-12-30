package com.asg.shipping.importManifestUpdate.service;


import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.entity.*;
import com.asg.shipping.importManifestUpdate.respository.*;
import com.asg.shipping.importManifestUpdate.util.ImportManifestBlMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportManifestBlServiceImpl implements ImportManifestBlService{

    private final ShipBlManifestHdrRepository repository;
    private final ShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ShipBlManifestPartBLRepository containerPrtRepository;
    private final ShipBlManifestEmailFaxDtlRepository emailFaxDtlRepository;
    private final ShipBlManifestMafiDtlRepository mafiDtlRepository;
    private final DocumentSearchService documentService;
    //private final LovService lovService;
    private final ImportManifestBlMapper mapper;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid) {
        log.info("Updating Import Manifest BL with id: {}", id);

        ShipBlManifestHdr entity = repository.findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import Manifest BL", "transactionPoid", id.toString()));

        // Call PROC_SHIP_VALD_BEFORE_SAVE for validation
        validateBeforeSave(dto, id, companyPoid, groupPoid);

        // Validate
        validateUpdateDTO(dto, id, companyPoid, groupPoid);

        // Store old values for comparison
        String oldBlNumber = entity.getBlNumber();
        String oldCargoType = entity.getCargoType();
        LocalDate oldTransactionDate = entity.getTransactionDate();
        String oldFreightStatus = entity.getFreightStatus();
        String oldDoNo = entity.getDoNo();

        // Update entity
        mapper.mapUpdateDTOToEntity(dto, entity);

        // Apply EDI formatting if EDI fields changed
        if (dto.getShipperEdiName() != null || dto.getConsigneeEdiName() != null ||
                dto.getNotify1EdiName() != null || dto.getNotify2EdiName() != null ||
                dto.getNotify3EdiName() != null) {
            formatEdiFields(entity);
        }

        // Validate transaction date change
       /* if (dto.getTransactionDate() != null && !dto.getTransactionDate().equals(oldTransactionDate)) {
            validateTransactionDateChange(companyPoid, dto.getTransactionDate());
        }*/

        ShipBlManifestHdr saved = repository.save(entity);

        // Update all detail tables
        updateDetailTables(dto, saved.getTransactionPoid());

        // Handle cargo type change (affects container inventory)
      /*  if (dto.getCargoType() != null && !dto.getCargoType().equals(oldCargoType)) {
            handleCargoTypeChange(saved.getTransactionPoid(), dto.getCargoType());
        }*/

        // Call PROC_SHIP_DO_BL_STATUS if DO/BL status changed
        if (dto.getDoNo() != null && !dto.getDoNo().equals(oldDoNo) ||
                (dto.getFreightStatus() != null && !dto.getFreightStatus().equals(oldFreightStatus))) {
            updateDoBlStatus(saved.getTransactionPoid(), groupPoid, companyPoid);
        }

        // Call PROC_SHIP_BL_PAGE_SAVE_AFTER for post-save processing
        processAfterSave(saved, groupPoid, companyPoid, "AUTOSUMWEIGHTPACKATE");

        ImportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
        //enrichLovData(result);

        log.info("Successfully updated Import Manifest BL with id: {}", id);
        return result;
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

    /**
     * Update all detail tables for a transaction (update)
     */
    private void updateDetailTables(ImportManifestBlUpdateDTO dto, Long transactionPoid) {
        // Delete existing details and recreate (simplified approach - can be optimized)
        if (dto.getGeneralCargoDetails() != null) {
            generalDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (GeneralCargoRequestDto detailDto : dto.getGeneralCargoDetails()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(generalDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestGeneralDtl entity = mapper.mapGeneralDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                generalDtlRepository.save(entity);
            }
        }

        if (dto.getCargoDescriptions() != null) {
            cargoDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (CargoDescriptionRequestDto detailDto : dto.getCargoDescriptions()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(cargoDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestCargoDtl entity = mapper.mapCargoDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestCargoDtlId(transactionPoid, detRowId,detailDto.getDescriptionType()));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                cargoDtlRepository.save(entity);
            }
        }

        if (dto.getContainers() != null) {
            // For container details, we need to be careful about container inventory integration
            // Delete existing and recreate
            containerDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (ContainerRequestDto detailDto : dto.getContainers()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(containerDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestContainerDtl entity = mapper.mapContainerDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                // Validate container number uniqueness
                if (entity.getContainerNo() != null && !entity.getContainerNo().trim().isEmpty()) {
                    containerDtlRepository.findByTransactionPoidAndContainerNo(transactionPoid, entity.getContainerNo())
                            .ifPresent(existing -> {
                                if (!existing.getId().getDetRowId().equals(detRowId)) {
                                    throw new ValidationException("Container number already exists: " + entity.getContainerNo());
                                }
                            });
                }
                containerDtlRepository.save(entity);
            }
        }

        if (dto.getChargeDetails() != null) {
            chargesDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (ChargeRequestDto detailDto : dto.getChargeDetails()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(chargesDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestChargesDtl entity = mapper.mapChargesDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                chargesDtlRepository.save(entity);
            }
        }

        if (dto.getPartBls() != null) {
            containerPrtRepository.deleteByTransactionPoid(transactionPoid);
            for (PartBlRequestDto detailDto : dto.getPartBls()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(containerPrtRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestPartBL entity = mapper.mapContainerPrtFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                containerPrtRepository.save(entity);
            }
        }

        if (dto.getNotifyParties() != null) {
            emailFaxDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (NotifyPartyRequestDto detailDto : dto.getNotifyParties()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(emailFaxDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestEmailFaxDtl entity = mapper.mapEmailFaxDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestEmailFaxId(transactionPoid, detRowId,detailDto.getAddressType()));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                emailFaxDtlRepository.save(entity);
            }
        }

        if (dto.getMafiDetails() != null) {
            mafiDtlRepository.deleteByTransactionPoid(transactionPoid);
            for (MafiRequestDto detailDto : dto.getMafiDetails()) {
                Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() :
                        getNextDetRowId(mafiDtlRepository.getMaxDetRowId(transactionPoid));
                ShipBlManifestMafiDtl entity = mapper.mapMafiDtlFromDto(detailDto, transactionPoid);
                if (entity.getId() == null) {
                    entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
                } else {
                    entity.getId().setDetRowId(detRowId);
                }
                mafiDtlRepository.save(entity);
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
    private void processAfterSave(ShipBlManifestHdr saved, Long groupPoid, Long companyPoid, String param2) {
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
    private void loadDetailTables(ImportManifestBlRequestDto dto, Long transactionPoid) {
        dto.setGeneralCargoDetails(mapper.mapGeneralDtlListToDto(
                generalDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setCargoDescriptions(mapper.mapCargoDtlListToDto(
                cargoDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setContainers(mapper.mapContainerDtlListToDto(
                containerDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setChargeDetails(mapper.mapChargesDtlListToDto(
                chargesDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setPartBls(mapper.mapContainerPrtListToDto(
                containerPrtRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setNotifyParties(mapper.mapEmailFaxDtlListToDto(
                emailFaxDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
        dto.setMafiDetails(mapper.mapMafiDtlListToDto(
                mafiDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)));
    }

    private Long getNextDetRowId(Long maxDetRowId) {
        return (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
    }
}
