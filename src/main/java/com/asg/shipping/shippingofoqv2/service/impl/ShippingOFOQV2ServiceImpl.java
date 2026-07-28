package com.asg.shipping.shippingofoqv2.service.impl;

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
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.shippingofoqv2.dto.*;
import com.asg.shipping.shippingofoqv2.entity.OFOQAmendBlDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.OFOQItemDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.OFOQManifestAmendmentResponseDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.OfoqApiDataHdrEntity;
import com.asg.shipping.shippingofoqv2.mapper.OFOQMapper;
import com.asg.shipping.shippingofoqv2.repository.*;
import com.asg.shipping.shippingofoqv2.service.OFOQApiService;
import com.asg.shipping.shippingofoqv2.service.ShippingOFOQV2Service;
import jakarta.persistence.EntityManager;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingOFOQV2ServiceImpl implements ShippingOFOQV2Service {

    /** Legacy posts the full manifest with manifest type "M" and BL number "0". */
    private static final String MANIFEST_TYPE_MANIFEST = "M";
    private static final String MANIFEST_TYPE_AMEND = "AM";
    private static final String NO_BL_NUMBER = "0";

    private final DocumentSearchService documentSearchService;
    private final OfoqApiDataHdrRepository ofoqApiDataHdrRepository;
    private final OFOQItemDtlRepository OFOQItemDtlRepository;
    private final OFOQManifestResponseDtlRepository OFOQManifestResponseDtlRepository;
    private final OFOQManifestAmendmentResponseDtlRepository OFOQManifestAmendmentResponseDtlRepository;
    private final OFOQAmendBlDtlRepository OFOQAmendBlDtlRepository;
    private final ShippingOFOQProcRepository shippingOFOQProcRepository;
    private final OFOQMapper ofoqMapper;
    private final OFOQApiService OFOQApiService;
    private final EntityManager entityManager;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    public Map<String, Object> listShippingOFOQ(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentSearchService.search(
                documentId,
                filterList,
                operator,
                pageable,
                isDeleted,
                "VESSEL_NAME",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public OFOQVoyageDataResponse getShippingOFOQById(Long transactionPoid) {
        OfoqApiDataHdrEntity headerEntity = findEntityById(transactionPoid);

        OFOQApiDataHdrDto header = ofoqMapper.toHeaderDto(headerEntity, null);

        List<OFOQItemDtlDto> lineDetail = OFOQItemDtlRepository
                .findByTransactionPoid(transactionPoid).stream()
                .map(ofoqMapper::toLineDetailDto)
                .toList();

        List<OFOQCheckStatusManifestResponse> manifestResponses = OFOQManifestResponseDtlRepository
                .findByTransactionPoid(transactionPoid).stream()
                .map(ofoqMapper::toManifestResponseDto)
                .toList();

        List<OFOQManifestAmendmentResponse> amendmentResponses = OFOQManifestAmendmentResponseDtlRepository
                .findByTransactionPoid(transactionPoid).stream()
                .map(ofoqMapper::toAmendmentResponseDto)
                .toList();

        List<OFOQAmendBLDto> amendBL = OFOQAmendBlDtlRepository
                .findByTransactionPoid(transactionPoid).stream()
                .map(ofoqMapper::toAmendBLDto)
                .toList();

        return ofoqMapper.toVoyageDataResponse(header, lineDetail, amendmentResponses, amendBL, manifestResponses);
    }

    @Override
    public OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request) {
        return shippingOFOQProcRepository.loadOFOQDetails(request);
    }

    @Override
    @Transactional
    public OFOQCheckStatusResponseDto createShippingOFOQ(ShippingOFOQV2RequestDto request) {
        log.info("Creating OFOQ manifest for docRef: {}", request.getDocRef());
        try {
            validateDocument(request.getArrivalDate(), request.getRotationNumber(), request.getVesselPoid(), request.getVoyageNo());

            OfoqApiDataHdrEntity savedHeader =
                    ofoqApiDataHdrRepository.saveAndFlush(
                            OfoqApiDataHdrEntity.builder()
                                    .docRef(request.getDocRef())
                                    .voyageNo(request.getVoyageNo())
                                    .vesselPoid(request.getVesselPoid())
                                    .arrivalDate(
                                            request.getArrivalDate() == null
                                                    ? null
                                                    : request.getArrivalDate().atStartOfDay()
                                    )
                                    .rotationNumber(request.getRotationNumber())
                                    .remarks(request.getRemarks())
                                    .transactionDate(LocalDateTime.now())
                                    .deleted("N")
                                    .manifestType(MANIFEST_TYPE_MANIFEST)
                                    .companyPoid(UserContext.getCompanyPoid())
                                    .build()
                    );

            Long transactionPoid = savedHeader.getTransactionPoid();
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), transactionPoid.toString());
            log.debug("OFOQ header created with transactionPoid: {}", transactionPoid);

            // The manifest XML procedure reads the detail tables, so the selected lines have to be
            // persisted before the document is posted - exactly like the legacy save/afterSave order.
            if (request.getLineDetails() != null && !request.getLineDetails().isEmpty()) {
                saveItemDetails(
                        request.getLineDetails().stream()
                                .filter(dto -> !"ISDELETED".equalsIgnoreCase(dto.getActionType()))
                                .toList(),
                        transactionPoid);
            }
            if (request.getAmendBl() != null) {
                updateAmendBl(request.getAmendBl(), transactionPoid);
            }

            return submitAndCheckOFOQManifest(transactionPoid, MANIFEST_TYPE_MANIFEST, NO_BL_NUMBER,
                    resolveDocRef(savedHeader, request.getDocRef()));
        } catch (Exception e) {
            log.error("Error creating OFOQ manifest for docRef: {}", request.getDocRef(), e);
            throw e;
        }
    }


    @Override
    @Transactional
    public OFOQCheckStatusResponseDto checkStatus(OFOQCheckStatusDto request, String manifestType) {
        log.debug("Checking status for functionalReference: {}", request.getFunctionalReference());
        try {
            OfoqApiDataHdrEntity header = validateCheckStatusRequest(request);

            String functionalReference = resolveFunctionalReference(request, header);
            String docRef = resolveDocRef(header, request.getDocReference());
            // Legacy always checks the status with manifest type "M" and BL number "0" unless a
            // single BL is being amended.
            String blNumber = request.getBlNumber() != null ? request.getBlNumber() : NO_BL_NUMBER;
            String type = manifestType != null ? manifestType : MANIFEST_TYPE_MANIFEST;

            OFOQCheckStatusCustomsResponseDto customsResponse =
                    OFOQApiService.getManifestStatus(functionalReference);
            log.debug("Received customs response with statusCode: {}", customsResponse.getStatusCode());

            // The procedure updates the header and detail tables directly, so pending JPA changes
            // must be written first and the persistence context re-read afterwards.
            entityManager.flush();
            saveManifestStatusResponse(request.getTransactionPoid(), docRef, type, blNumber, customsResponse);
            entityManager.clear();

            logOFOQActivity(request.getTransactionPoid(), String.format(
                    "OFOQ status checked for Functional Reference: %s | Manifest Type: %s%s | Status Code: %s | Response: %s",
                    functionalReference,
                    type,
                    blSuffix(blNumber),
                    customsResponse.getStatusCode(),
                    abbreviate(customsResponse.getResponseMessage())));

            OfoqApiDataHdrEntity refreshedHeader =
                    ofoqApiDataHdrRepository
                            .findByTransactionPoid(request.getTransactionPoid())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Transaction",
                                            "transactionPoid",
                                            request.getTransactionPoid()
                                    )
                            );

            List<OFOQCheckStatusManifestResponse> manifestResponses =
                    OFOQManifestResponseDtlRepository
                            .findByTransactionPoid(request.getTransactionPoid())
                            .stream()
                            .map(ofoqMapper::toManifestResponseDto)
                            .toList();

            OFOQCheckStatusResponseDto checkStatusResponseDto = new OFOQCheckStatusResponseDto();
            checkStatusResponseDto.setHeader(ofoqMapper.toHeaderDto(refreshedHeader, functionalReference));
            checkStatusResponseDto.setManifestResponses(manifestResponses);

            log.info("Status check completed for transactionPoid: {} with statusCode: {}", request.getTransactionPoid(), customsResponse.getStatusCode());
            return checkStatusResponseDto;
        } catch (Exception e) {
            log.error("Error checking status for functionalReference: {}", request.getFunctionalReference(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public OFOQCheckStatusResponseDto updateShippingOFOQ(Long transactionPoid, ShippingOFOQV2UpdateRequestDto request) {
        log.info("Updating OFOQ manifest for transactionPoid: {}", transactionPoid);
        try {
            validateDocument(request.getArrivalDate(), request.getRotationNumber(), request.getVesselPoid(), request.getVoyageNo());
            OfoqApiDataHdrEntity existingEntity = findEntityById(transactionPoid);
            OfoqApiDataHdrEntity oldEntity = new OfoqApiDataHdrEntity();
            BeanUtils.copyProperties(existingEntity, oldEntity);
            existingEntity.setVoyageNo(request.getVoyageNo());
            existingEntity.setRemarks(request.getRemarks());
            existingEntity.setVesselPoid(request.getVesselPoid());
            existingEntity.setArrivalDate(
                    request.getArrivalDate() != null
                            ? request.getArrivalDate().atStartOfDay()
                            : null
            );
            existingEntity.setRotationNumber(request.getRotationNumber());
            existingEntity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
            existingEntity.setLastModifiedDate(LocalDateTime.now());

            ofoqApiDataHdrRepository.saveAndFlush(existingEntity);
            log.debug("OFOQ header updated for transactionPoid: {}", transactionPoid);
            if (request.getLineDetails() != null) {
                updateItemDetails(request.getLineDetails(), transactionPoid);
            }
            if (request.getAmendBl() != null) {
                updateAmendBl(request.getAmendBl(), transactionPoid);
            }
            loggingService.logChanges(oldEntity, existingEntity, OfoqApiDataHdrEntity.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");


            return submitAndCheckOFOQManifest(transactionPoid, MANIFEST_TYPE_MANIFEST, NO_BL_NUMBER,
                    resolveDocRef(existingEntity, request.getDocRef()));
        } catch (Exception e) {
            log.error("Error updating OFOQ manifest for transactionPoid: {}", transactionPoid, e);
            throw e;
        }
    }

    /**
     * Keeps the BL rows in sync with the request without losing the amendment response columns
     * (functional reference, status, amendment request number) already stored against a BL.
     */
    private void updateAmendBl(List<OFOQRequestAmendBlDto> amendBl, Long transactionPoid) {

        Map<String, OFOQAmendBlDtlEntity> existingByBl = new LinkedHashMap<>();
        for (OFOQAmendBlDtlEntity entity : OFOQAmendBlDtlRepository.findByTransactionPoid(transactionPoid)) {
            if (entity.getBlNumber() != null) {
                existingByBl.put(entity.getBlNumber(), entity);
            }
        }

        AtomicLong detRowCounter = new AtomicLong(
                existingByBl.values().stream()
                        .map(OFOQAmendBlDtlEntity::getDetRowId)
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0L));

        for (OFOQRequestAmendBlDto dto : amendBl) {
            if (dto.getBlNumber() == null || dto.getBlNumber().isBlank()) {
                continue;
            }
            if (existingByBl.remove(dto.getBlNumber()) != null) {
                continue;
            }

            Long detRowId = detRowCounter.incrementAndGet();
            OFOQAmendBlDtlRepository.save(OFOQAmendBlDtlEntity.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(detRowId)
                    .blNumber(dto.getBlNumber())
                    .build());
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(),
                    String.format("Row Created on Amend Bl with DetRowId: %s", detRowId));
        }

        // Rows no longer present in the request were removed from the screen.
        existingByBl.values().forEach(entity -> {
            OFOQAmendBlDtlRepository.delete(entity);
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(),
                    String.format("Row Deleted from Amend Bl with DetRowId: %s", entity.getDetRowId()));
        });
    }

    @Override
    @Transactional
    public void deleteShippingOFOQ(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting OFOQ manifest for transactionPoid: {}", transactionPoid);
        try {
            OfoqApiDataHdrEntity entity = findEntityById(transactionPoid);
            LocalDate transactionDate =
                    entity.getTransactionDate() != null
                            ? entity.getTransactionDate().toLocalDate()
                            : null;

            documentDeleteService.deleteDocument(
                    transactionPoid,
                    "OFOQ_API_DATA_HDR",
                    "TRANSACTION_POID",
                    deleteReasonDto,
                    transactionDate
            );
            log.info("OFOQ manifest deleted successfully for transactionPoid: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error deleting OFOQ manifest for transactionPoid: {}", transactionPoid, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public AmendBlDto amendBl(OFOQAmendBlRequestDto request) {

        if (request.getTransactionPoid() == null) {
            throw new ValidationException("Please save the document and try again.");
        }
        if (request.getBlNumber() == null || request.getBlNumber().isBlank()) {
            throw new ValidationException("BL number is mandatory to amend a BL.");
        }

        OfoqApiDataHdrEntity header = findEntityById(request.getTransactionPoid());

        if (request.getLineDetails() != null) {
            updateItemDetails(request.getLineDetails(), request.getTransactionPoid());
        }

        if (request.getAmendBl() != null) {
            updateAmendBl(request.getAmendBl(), request.getTransactionPoid());
        }

        return buildAmendBlResponse(request.getTransactionPoid(), request.getBlNumber(),
                resolveDocRef(header, request.getDocReference()));
    }


    private AmendBlDto buildAmendBlResponse(Long transactionPoid, String blNumber, String docReference) {

        submitAndCheckOFOQManifest(transactionPoid, MANIFEST_TYPE_AMEND, blNumber, docReference);

        OFOQAmendBlDtlEntity amendEntity = OFOQAmendBlDtlRepository.findByTransactionPoidAndBlNumber(transactionPoid, blNumber);

        List<OFOQManifestAmendmentResponseDtlEntity> amendmentEntities =
                OFOQManifestAmendmentResponseDtlRepository
                        .findByTransactionPoidAndXmlBlNumber(transactionPoid, blNumber);

        AmendBlDto dto = new AmendBlDto();
        dto.setAmendBL(ofoqMapper.toAmendBLDto(amendEntity));
        dto.setManifestAmendmentResponses(
                amendmentEntities.stream()
                        .map(ofoqMapper::toAmendmentResponseDto)
                        .toList());
        return dto;
    }

    /**
     * Mirrors the legacy CheckPostedDataStatus guard: the functional reference comes from the saved
     * document, so an unsaved document (or one that was never posted) cannot be checked.
     */
    private OfoqApiDataHdrEntity validateCheckStatusRequest(OFOQCheckStatusDto request) {
        if (request.getTransactionPoid() == null) {
            throw new ValidationException("Please save the document and try again.");
        }
        return findEntityById(request.getTransactionPoid());
    }

    /** Falls back to the reference stored on the document, which is what the legacy screen reads. */
    private String resolveFunctionalReference(OFOQCheckStatusDto request, OfoqApiDataHdrEntity header) {
        String functionalReference = request.getFunctionalReference();
        if (functionalReference == null || functionalReference.isBlank()) {
            functionalReference = header.getFunctionalRef();
        }
        if (functionalReference == null || functionalReference.isBlank()) {
            throw new ValidationException("Functional reference id is mandatory. Please save the document and try again.");
        }
        return functionalReference;
    }

    /**
     * Writes an audit entry for an OFOQ interaction. The manifest has already been handed to (or
     * read back from) OFOQ by the time this runs, so a logging failure must never roll the
     * transaction back.
     */
    private void logOFOQActivity(Long transactionPoid, String detail) {
        try {
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), detail);
        } catch (Exception e) {
            log.warn("Failed to write OFOQ activity log for transactionPoid: {} - {}", transactionPoid, detail, e);
        }
    }

    private String blSuffix(String blNumber) {
        return blNumber == null || NO_BL_NUMBER.equals(blNumber) ? "" : " | BL: " + blNumber;
    }

    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "-";
        }
        String single = message.replaceAll("\\s+", " ").trim();
        return single.length() <= 500 ? single : single.substring(0, 500) + "...";
    }

    private void saveManifestStatusResponse(Long transactionPoid, String docRef, String manifestType, String blNumber, OFOQCheckStatusCustomsResponseDto statusResponse) {
        shippingOFOQProcRepository.saveOFOQManifestResponse(
                transactionPoid,
                docRef,
                statusResponse.getFunctionalReference(),
                statusResponse.getStatusCode(),
                statusResponse.getStatusText(),
                statusResponse.getResponseBody(),
                manifestType,
                blNumber
        );
    }

    private void saveItemDetails(List<OFOQItemDtlDto> lineDetails, Long transactionPoid) {

        AtomicLong detRowIdCounter =
                new AtomicLong(OFOQItemDtlRepository.findMaxDetRowId(transactionPoid));
        lineDetails.forEach(dto -> {

            Long detRowId = detRowIdCounter.incrementAndGet();
            OFOQItemDtlEntity entity = OFOQItemDtlEntity.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(detRowId)
                    .vesselVoyagePoid(dto.getVesselVoyagePoid())
                    .lineName(dto.getLineName())
                    .vesselName(dto.getVesselName())
                    .voyageNo(dto.getVoyageNo())
                    .jobNo(dto.getJobNo())
                    .arrivalDate(dto.getArrivalDate() != null ? dto.getArrivalDate().atStartOfDay() : null)
                    .sailDate(dto.getSailDate() != null ? dto.getSailDate().atStartOfDay() : null)
                    .drilldownLinkInfo(dto.getDrillDownLinkInfo())
                    // Legacy only posts the lines the user ticked; an unticked row defaults to "N".
                    .checked(dto.getChecked() != null ? dto.getChecked() : "N")
                    .companyPoid(UserContext.getCompanyPoid())
                    .build();

            OFOQItemDtlRepository.save(entity);

            String logDetail = String.format(
                    "Row Created on items detail with DetRowId: %s",
                    detRowId
            );

            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail
            );
        });
    }

    private void validateDocument(LocalDate arrivalDate, Long rotationNumber, Long vesselPoid, String voyageNumber) {
        if (voyageNumber == null || voyageNumber.isBlank()) {
            throw new ValidationException("Voyage Number is required");
        }
        if (vesselPoid == null) {
            throw new ValidationException("Vessel POID is required");
        }
        if (arrivalDate == null) {
            throw new ValidationException("Arrival Date is required");
        }
        if (rotationNumber == null) {
            throw new ValidationException("Rotation Number is required");
        }
    }

    private OfoqApiDataHdrEntity findEntityById(Long transactionPoid) {
        return ofoqApiDataHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("OFOQ API Data Header", "transactionPoid", transactionPoid));
    }

    /** The document reference of the saved document wins, as in the legacy getCurrentDocumentRef(). */
    private String resolveDocRef(OfoqApiDataHdrEntity header, String requestDocRef) {
        if (header != null && header.getDocRef() != null && !header.getDocRef().isBlank()) {
            return header.getDocRef();
        }
        return requestDocRef;
    }

    private void updateItemDetails(List<OFOQItemDtlDto> lineDetails, Long transactionPoid) {

        lineDetails.stream()
                .filter(dto -> "ISDELETED".equalsIgnoreCase(dto.getActionType()) && dto.getDetRowId() != null)
                .forEach(dto -> {
                    OFOQItemDtlRepository.deleteByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId());
                    loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(),
                            "Row Deleted from Line Detail with DetRowId: " + dto.getDetRowId());
                });
        OFOQItemDtlRepository.flush();

        List<OFOQItemDtlDto> createdItems = lineDetails.stream()
                .filter(dto -> "ISCREATED".equalsIgnoreCase(
                        Optional.ofNullable(dto.getActionType()).orElse("NOCHANGE")))
                .collect(Collectors.toList());

        if (!createdItems.isEmpty()) {
            saveItemDetails(createdItems, transactionPoid);
        }
    }

    private OFOQCheckStatusResponseDto submitAndCheckOFOQManifest(Long transactionPoid, String manifestType, String blNumber, String docRef) {
        log.debug("Submitting OFOQ manifest for transactionPoid: {}", transactionPoid);
        try {
            // The XML is built by a procedure reading OFOQ_API_DATA_DTL / OFOQ_API_MANIFEST_BL_DTL,
            // so everything written above has to be visible to the database session first.
            entityManager.flush();

            List<OFOQManifestXmlDto> xmlDtos =
                    shippingOFOQProcRepository.loadOFOQManifestXml(transactionPoid, blNumber, manifestType, docRef);

            String xmlData =
                    xmlDtos.stream()
                            .map(OFOQManifestXmlDto::getXmlData)
                            .collect(Collectors.joining());

            if (xmlData.isBlank()) {
                throw new ValidationException(
                        "No manifest data available to post. Please load and select the line details first.");
            }
            log.debug("Calling OFOQ API with xmlData length: {}", xmlData.length());

            String functionalRefId = OFOQApiService.callOFOQApi(xmlData, manifestType, blNumber, transactionPoid, docRef);

            logOFOQActivity(transactionPoid, String.format(
                    "Manifest posted to OFOQ API | Manifest Type: %s%s | Functional Reference: %s",
                    manifestType,
                    blSuffix(blNumber),
                    functionalRefId != null ? functionalRefId : "not returned"));

            if (functionalRefId == null) {
                log.warn("No functional reference is available for transactionPoid: {}", transactionPoid);
                entityManager.clear();
                OfoqApiDataHdrEntity header = findEntityById(transactionPoid);
                OFOQCheckStatusResponseDto responseDto = new OFOQCheckStatusResponseDto();
                responseDto.setHeader(ofoqMapper.toHeaderDto(header, null));
                responseDto.setManifestResponses(new ArrayList<>());
                return responseDto;
            }

            log.info("OFOQ manifest submitted successfully with functionalRefId: {}", functionalRefId);
            return checkStatus(OFOQCheckStatusDto.builder()
                    .functionalReference(functionalRefId)
                    .transactionPoid(transactionPoid)
                    .docReference(docRef)
                    .blNumber(blNumber)
                    .build(), manifestType);
        } catch (Exception e) {
            log.error("Error submitting OFOQ manifest for transactionPoid: {}", transactionPoid, e);
            throw e;
        }
    }

}
