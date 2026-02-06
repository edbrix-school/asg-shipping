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
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.shippingofoqv2.dto.*;
import com.asg.shipping.shippingofoqv2.entity.*;
import com.asg.shipping.shippingofoqv2.mapper.OFOQMapper;
import com.asg.shipping.shippingofoqv2.repository.*;
import com.asg.shipping.shippingofoqv2.service.OFOQApiService;
import com.asg.shipping.shippingofoqv2.service.OFOQValidationService;
import com.asg.shipping.shippingofoqv2.service.ShippingOFOQV2Service;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingOFOQV2ServiceImpl implements ShippingOFOQV2Service {

    private final DocumentSearchService documentSearchService;
    private final OfoqApiDataHdrRepository ofoqApiDataHdrRepository;
    private final OFOQItemDtlRepository OFOQItemDtlRepository;
    private final OFOQManifestResponseDtlRepository OFOQManifestResponseDtlRepository;
    private final OFOQManifestAmendmentResponseDtlRepository OFOQManifestAmendmentResponseDtlRepository;
    private final OFOQAmendBlDtlRepository OFOQAmendBlDtlRepository;
    private final ShippingOFOQProcRepository shippingOFOQProcRepository;
    private final OFOQMapper ofoqMapper;
    private final OFOQValidationService validationService;
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
        
        OFOQApiDataHdrDto header = ofoqMapper.toHeaderDto(headerEntity,null);
        
        List<OFOQItemDtlDto> lineDetail = OFOQItemDtlRepository
                .findByTransactionPoid(transactionPoid).stream()
                .map(ofoqMapper::toLineDetailDto)
                .toList();

        List<OFOQCheckStatusManifestResponse> manifestResponses =
                OFOQManifestResponseDtlRepository
                        .findByTransactionPoid(transactionPoid)
                        .stream()
                        .map(entity -> {
                            OFOQCheckStatusManifestResponse dto =
                                    new OFOQCheckStatusManifestResponse();
                            dto.setTransactionPoid(entity.getTransactionPoid());
                            dto.setDetRowId(entity.getDetRowId());
                            dto.setDate(entity.getResponseDate());
                            dto.setFunctionalReference(entity.getFunctionalRef());
                            dto.setStatusCode(dto.getStatusCode());
                            dto.setResponseMessage(dto.getResponseMessage());
                            dto.setProcessingStatus(entity.getProcessingStatus());
                            return dto;
                        })
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
    public OFOQCheckStatusResponseDto createShippingOFOQ(ShippingOFOQV2Request request) {
        log.info("Creating OFOQ manifest for docRef: {}", request.getDocRef());
        try {
            validationService.validateDocument(request);

            OfoqApiDataHdrEntity savedHeader =
                    ofoqApiDataHdrRepository.saveAndFlush(
                            OfoqApiDataHdrEntity.builder()
                                    .docRef(request.getDocRef())
                                    .voyageNo(request.getVoyageNo())
                                    .vesselPoid(request.getVesselPoid())
                                    .arrivalDate(
                                            request.getArrivalDate() == null
                                                    ? null
                                                    : request.getArrivalDate().atTime(LocalTime.now())
                                    )
                                    .rotationNumber(request.getRotationNumber())
                                    .remarks(request.getRemarks())
                                    .transactionDate(LocalDateTime.now())
                                    .deleted("N")
                                    .createdBy(ASGHelperUtils.getCurrentUser())
                                    .createdDate(LocalDateTime.now())
                                    .lastModifiedBy(ASGHelperUtils.getCurrentUser())
                                    .lastModifiedDate(LocalDateTime.now())
                                    .build()
                    );

            Long transactionPoid = savedHeader.getTransactionPoid();
            if (request.getLineDetails() != null) {
                saveItemDetails(request.getLineDetails(),transactionPoid);
            }
            if (request.getAmendBl()!= null){

                saveAmendBl(request.getAmendBl(),transactionPoid);
            }
            String key = savedHeader.getTransactionPoid().toString();
            loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);
            log.debug("OFOQ header created with transactionPoid: {}", transactionPoid);

            return submitAndCheckOFOQManifest(transactionPoid,"M",null,request.getDocRef(),request.getVesselPoid());
        } catch (Exception e) {
            log.error("Error creating OFOQ manifest for docRef: {}", request.getDocRef(), e);
            throw e;
        }
    }


    @Override
    public OFOQCheckStatusResponseDto checkStatus(OFOQCheckStatusDto request,String manifestType) {
        log.debug("Checking status for functionalReference: {}", request.getFunctionalReference());
        try {
            validateCheckStatusRequest(request);

            OFOQCheckStatusCustomsResponseDto customsResponse =
                    OFOQApiService.getManifestStatus(request.getFunctionalReference());
            log.debug("Received customs response with statusCode: {}", customsResponse.getStatusCode());
            
            saveManifestStatusResponse(request.getTransactionPoid(), request.getDocReference(), manifestType, request.getBlNumber(), customsResponse);
            
            entityManager.clear();
            
            OfoqApiDataHdrEntity header =
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
                            .map(entity -> {
                                OFOQCheckStatusManifestResponse dto =
                                        new OFOQCheckStatusManifestResponse();
                                dto.setTransactionPoid(entity.getTransactionPoid());
                                dto.setDetRowId(entity.getDetRowId());
                                dto.setDate(entity.getResponseDate());
                                dto.setFunctionalReference(customsResponse.getFunctionalReference());
                                dto.setStatusCode(customsResponse.getStatusCode());
                                dto.setResponseMessage(customsResponse.getResponseMessage());
                                dto.setProcessingStatus(entity.getProcessingStatus());
                                return dto;
                            })
                            .toList();

            OFOQCheckStatusResponseDto checkStatusResponseDto = new OFOQCheckStatusResponseDto();
            checkStatusResponseDto.setHeader(ofoqMapper.toHeaderDto(header,request.getFunctionalReference()));
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
    public OFOQCheckStatusResponseDto updateShippingOFOQ(Long transactionPoid, ShippingOFOQV2Request request) {
        log.info("Updating OFOQ manifest for transactionPoid: {}", transactionPoid);
        try {
            validatePostRequest(request);
            OfoqApiDataHdrEntity existingEntity = findEntityById(transactionPoid);
            OfoqApiDataHdrEntity oldEntity = new OfoqApiDataHdrEntity();
            BeanUtils.copyProperties(existingEntity,oldEntity);
            existingEntity.setVoyageNo(request.getVoyageNo());
            existingEntity.setRemarks(request.getRemarks());
            existingEntity.setVesselPoid(request.getVesselPoid());
            existingEntity.setArrivalDate(
                    request.getArrivalDate() != null
                            ? request.getArrivalDate().atTime(LocalTime.now())
                            : null
            );
            existingEntity.setRotationNumber(request.getRotationNumber());
            existingEntity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
            existingEntity.setLastModifiedDate(LocalDateTime.now());
            
            ofoqApiDataHdrRepository.save(existingEntity);
            log.debug("OFOQ header updated for transactionPoid: {}", transactionPoid);
            if (request.getLineDetails() != null) {
                updateItemDetails(request.getLineDetails(), transactionPoid);
            }
            if (request.getAmendBl() != null){
                updateAmendBl(request.getAmendBl(), transactionPoid);
            }
            loggingService.logChanges(oldEntity, existingEntity, OfoqApiDataHdrEntity.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "PRINCIPAL_POID");


            return submitAndCheckOFOQManifest( transactionPoid,"M",null,request.getDocRef(),request.getVesselPoid());
        } catch (Exception e) {
            log.error("Error updating OFOQ manifest for transactionPoid: {}", transactionPoid, e);
            throw e;
        }
    }

    @Transactional
    private void updateAmendBl(List<OFOQRequestAmendBlDto> amendBl, Long transactionPoid) {
        amendBl.forEach(dto -> {
                OFOQAmendBlDtlEntity entity = OFOQAmendBlDtlEntity.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId(dto.getDetRowId())
                        .blNumber(dto.getBlNumber())
                        .createdBy(ASGHelperUtils.getCurrentUser())
                        .createdDate(LocalDateTime.now())
                        .lastModifiedBy(ASGHelperUtils.getCurrentUser())
                        .lastModifiedDate(LocalDateTime.now())
                        .build();
                OFOQAmendBlDtlRepository.save(entity);
                String logDetail = String.format("Row Created on Amend Bl with DetRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
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
    public AmendBlDto amendBl(OFOQAmendBlRequestDto request) {

        submitAndCheckOFOQManifest(request.getTransactionPoid(),"AM",request.getBlNumber(), request.getDocReference(),request.getVesselPoid());
        AmendBlDto amendBlDto = new AmendBlDto();
        OFOQAmendBlDtlEntity ofoqAmendBlDtlEntity = OFOQAmendBlDtlRepository.findByTransactionPoidAndBlNumber(
                request.getTransactionPoid(),
                request.getBlNumber()
        );

        List<OFOQManifestAmendmentResponseDtlEntity> ofoqManifestAmendmentResponseDtlEntity =
                OFOQManifestAmendmentResponseDtlRepository.findByTransactionPoidAndXmlBlNumber(
                        request.getTransactionPoid(),
                        request.getBlNumber()
                );

        amendBlDto.setAmendBL(ofoqMapper.toAmendBLDto(ofoqAmendBlDtlEntity));
        amendBlDto.setManifestAmendmentResponses(
                ofoqManifestAmendmentResponseDtlEntity.stream()
                        .map(ofoqMapper::toAmendmentResponseDto)
                        .toList()
        );
        return amendBlDto;


    }

    private void validateCheckStatusRequest(OFOQCheckStatusDto request) {
        if (request.getTransactionPoid() == null){
            throw new ValidationException("Document must be saved ");
        }
        if(request.getFunctionalReference() ==  null ){
            throw new ValidationException("Functional reference id is mandatory");
        }
    }

    private void saveManifestStatusResponse(Long transactionPoid, String docRef, String manifestType, String blNumber, OFOQCheckStatusCustomsResponseDto statusResponse) {
        shippingOFOQProcRepository.saveOFOQManifestResponse(
                transactionPoid,
                docRef,
                statusResponse.getFunctionalReference(),
                statusResponse.getStatusCode(),
                statusResponse.getResponseMessage(),
                statusResponse.getResponseBody(),
                manifestType,
                blNumber
        );
    }

    private void saveAmendBl(List<OFOQRequestAmendBlDto> amendBl, Long transactionPoid) {
        amendBl.forEach(dto -> {
            OFOQAmendBlDtlEntity entity = OFOQAmendBlDtlEntity.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(dto.getDetRowId())
                    .blNumber(dto.getBlNumber())
                    .createdBy(ASGHelperUtils.getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .build();
            OFOQAmendBlDtlRepository.save(entity);
            String logDetail = String.format("Row Created on Amend Bl with DetRowId: %s", entity.getDetRowId());
            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString() , logDetail);
        });
    }

    @Transactional
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
                    .arrivalDate(
                    dto.getArrivalDate() != null
                            ? dto.getArrivalDate().atTime(LocalTime.now())
                            : null)
                    .sailDate(dto.getSailDate() != null ? dto.getSailDate().atTime(LocalTime.now()) : null)
                    .drilldownLinkInfo(dto.getDrillDownLinkInfo())
                    .checked(dto.getChecked())
                    .createdBy(ASGHelperUtils.getCurrentUser())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedBy(ASGHelperUtils.getCurrentUser())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();

            OFOQItemDtlRepository.save(entity);

            String logDetail = String.format(
                    "Row Created on items detail with DetRowId: %s",
                    detRowId
            );

            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    transactionPoid.toString(),
                    logDetail
            );
        });
    }

    private void validatePostRequest(ShippingOFOQV2Request request) {
        if (request.getVoyageNo() == null || request.getVoyageNo().isBlank()) {
            throw new ValidationException("Voyage Number is required");
        }
        if (request.getVesselPoid() == null) {
            throw new ValidationException("Vessel POID is required");
        }
        if (request.getArrivalDate() == null) {
            throw new ValidationException("Arrival Date is required");
        }
        if (request.getRotationNumber() == null) {
            throw new ValidationException("Rotation Number is required");
        }
    }

    private OfoqApiDataHdrEntity findEntityById(Long transactionPoid) {
        return ofoqApiDataHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("OFOQ API Data Header", "transactionPoid", transactionPoid));
    }


    @Transactional
    private void updateItemDetails(List<OFOQItemDtlDto> lineDetails, Long transactionPoid) {
        for (OFOQItemDtlDto dto : lineDetails) {
            String action = dto.getActionType() != null
                    ? dto.getActionType().toUpperCase()
                    : "NOCHANGE";

            switch (action) {

                case "ISCREATED":
                    saveItemDetails(List.of(dto), transactionPoid);
                    break;

                case "ISDELETED":
                    if (dto.getDetRowId() != null) {
                        OFOQItemDtlRepository
                                .deleteByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId());

                        String logDetail = String.format(
                                "Row Deleted from Line Detail with DetRowId: %s",
                                dto.getDetRowId()
                        );

                        loggingService.createLogSummaryEntry(
                                UserContext.getDocumentId(),
                                transactionPoid.toString(),
                                logDetail
                        );
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private OFOQCheckStatusResponseDto submitAndCheckOFOQManifest( Long transactionPoid,String manifestType,String blNumber,String docRef,Long vesselPoid){
        log.debug("Submitting OFOQ manifest for transactionPoid: {}", transactionPoid);
        try {
            List<OFOQManifestXmlDto> xmlDtos =
                    shippingOFOQProcRepository.loadOFOQManifestXml(transactionPoid,blNumber,manifestType,docRef ,vesselPoid);

            String xmlData =
                    xmlDtos.stream()
                            .map(OFOQManifestXmlDto::getXmlData)
                            .collect(Collectors.joining());

            OFOQManifestSubmitResponseDto apiResponse =
                    OFOQApiService.callOFOQApi(
                            xmlData,
                            manifestType,
                            blNumber,
                            transactionPoid,
                            docRef
                    );

            if (apiResponse.getFunctionalRefId() != null) {
                log.info("OFOQ manifest submitted successfully with functionalRefId: {}", apiResponse.getFunctionalRefId());
                return checkStatus(OFOQCheckStatusDto.builder()
                        .functionalReference(apiResponse.getFunctionalRefId())
                        .transactionPoid(transactionPoid)
                        .docReference(docRef)
                        .blNumber(blNumber)
                        .build(),manifestType);
            } else {
                log.warn("OFOQ manifest submission returned null functionalRefId for transactionPoid: {}", transactionPoid);
                OfoqApiDataHdrEntity header = findEntityById(transactionPoid);
                OFOQCheckStatusResponseDto responseDto = new OFOQCheckStatusResponseDto();
                responseDto.setHeader(ofoqMapper.toHeaderDto(header,apiResponse.getFunctionalRefId()));
                responseDto.setManifestResponses(new ArrayList<>());
                return responseDto;
            }
        } catch (Exception e) {
            log.error("Error submitting OFOQ manifest for transactionPoid: {}", transactionPoid, e);
            throw e;
        }
    }

}
