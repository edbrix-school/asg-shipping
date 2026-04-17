package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.*;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtl;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiDtlRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiHdrRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipReadOnlyRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.util.MafiTrailerDateUpdateFormMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MafiTrailerDateUpdateFormServiceImpl implements MafiTrailerDateUpdateFormService {

    private final DocumentSearchService documentService;
    private final ShipBlMafiHdrRepository headerRepository;
    private final ShipBlMafiDtlRepository detailRepository;
    private final ShipReadOnlyRepository readOnlyRepository;
    private final MafiTrailerDateUpdateFormMapper mapper;
    private final LoggingService loggingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAll(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {

        return listMafiTrailers(docId, request, pageable, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public MafiTrailerDateUpdateFormResponse getById(Long transactionPoid) {

        ShipBlMafiHdr header = headerRepository
                .findByTransactionPoidAndDeleted(transactionPoid, "N")
                .orElseThrow(() -> new EntityNotFoundException(
                        "Mafi trailer entry not found for transactionPoid: " + transactionPoid));

        List<ShipBlMafiDtl> details = detailRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        VoyageProjection voyage = readOnlyRepository.findVoyageDetailsById(header.getVoyageTransactionPoid())
                .orElseThrow(
                        () -> new EntityNotFoundException("Voyage not found for transactionPoid: " + transactionPoid));

        return mapper.toMafiTrailerResponse(header, voyage, details);
    }

    @Override
    @Transactional
    public MafiTrailerDateUpdateFormResponse update(Long transactionPoid, MafiTrailerDateUpdateFormRequest request) {

        String userId=UserContext.getUserId();

        ShipBlMafiHdr existingEntity = headerRepository
                .findByTransactionPoidAndDeleted(transactionPoid, "N")
                .orElseThrow(() -> new EntityNotFoundException(
                        "Mafi trailer entry not found for transactionPoid: " + transactionPoid));
        ShipBlMafiHdr headerEntity = new ShipBlMafiHdr();
        BeanUtils.copyProperties(existingEntity, headerEntity);

        MafitrailerHeaderDTO headerDto = request.getMafiHeader();

        if (!Objects.equals(headerDto.getAgentReference(), headerEntity.getAgentReference())
                || !Objects.equals(headerDto.getRemarks(), headerEntity.getRemarks())) {

            mapper.updateShipBlMafiHdr(headerEntity, request, userId);
            headerRepository.updateByTransactionPoid(headerEntity.getTransactionPoid(),
                    headerEntity.getAgentReference(), headerEntity.getRemarks(), userId);
        }

        List<LogRequestDto<ShipBlMafiDtl>> logRequests = new ArrayList<>();

        String docId = UserContext.getDocumentId();

        processDetails(transactionPoid, request.getMafiDetails());

        loggingService.createLogBatch(logRequests);
        loggingService.logChanges(existingEntity, headerEntity, ShipBlMafiHdr.class, docId, transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return getById(transactionPoid);
    }

    private void processDetails(Long transactionPoid, List<MafiDetailDtoRequest> details) {
        List<LogRequestDto<ShipBlMafiDtl>> logRequests = new ArrayList<>();
        String docId = UserContext.getDocumentId();

        // Auto-generate detRowId for new records
        Long maxDetRowId = detailRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        AtomicLong detRowIdSeq = new AtomicLong(maxDetRowId != null ? maxDetRowId + 1 : 1);

        for (MafiDetailDtoRequest detail : details) {
            String actionType = detail.getActionType() != null ? detail.getActionType().toUpperCase() : "ISCREATED";

            switch (actionType) {
                case "ISCREATED" -> {

                    Long newDetRowId = detRowIdSeq.getAndIncrement();
                    detail.setDetRowId(newDetRowId);

                    ShipBlMafiDtl newEntity = getShipBlMafiDtl(transactionPoid, detail, newDetRowId);
                    detailRepository.save(newEntity);
                    String logDetail = String.format("Row Created on Mafi trailer update form Detail with detRowId: %s", newEntity.getDetRowId());
                    loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
                }
                case "ISUPDATED" -> {
                    validateDetRowID(detail.getDetRowId());
                    ShipBlMafiDtl existing = detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, detail.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException("Mafi Trailer Update Form Detail", "detRowId", detail.getDetRowId()));

                    ShipBlMafiDtl oldEntity = new ShipBlMafiDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    existing.setBlPoid(detail.getBlPoid());
                    existing.setMafiRef(detail.getMafiRef());
                    existing.setMafiSize(detail.getMafiSize());
                    existing.setMafiFreeDays(detail.getMafiFreeDays());
                    existing.setBackLoadDate(detail.getBackLoadDate());
                    existing.setMafiEmptyDate(detail.getMafiEmptyDate());
                    existing.setRemarks(detail.getRemarks());
                    detailRepository.save(existing);

                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detail.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlMafiDtl.class, docId, transactionPoid.toString(), logDetailForUpdate));
                }
                case "ISDELETED" -> {
                    validateDetRowID(detail.getDetRowId());
                    detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, detail.getDetRowId())
                            .ifPresent(entity -> {
                                detailRepository.delete(entity);
                                loggingService.logDelete(detail, docId, transactionPoid.toString());
                            });
                }
            }
        }

        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private void validateDetRowID(Long detRowId){
        Optional.ofNullable(detRowId).orElseThrow(()-> new ValidationException(String.format("Validation Error on %s RowId is Required","detail")));
    }

    private static ShipBlMafiDtl getShipBlMafiDtl(Long transactionPoid, MafiDetailDtoRequest detail, Long newDetRowId) {
        ShipBlMafiDtl newEntity = new ShipBlMafiDtl();
        newEntity.setTransactionPoid(transactionPoid);
        newEntity.setDetRowId(newDetRowId);
        newEntity.setBlPoid(detail.getBlPoid());
        newEntity.setMafiRef(detail.getMafiRef());
        newEntity.setMafiSize(detail.getMafiSize());
        newEntity.setMafiFreeDays(detail.getMafiFreeDays());
        newEntity.setBackLoadDate(detail.getBackLoadDate());
        newEntity.setMafiEmptyDate(detail.getMafiEmptyDate());
        newEntity.setRemarks(detail.getRemarks());
        return newEntity;
    }

    private Map<String, Object> listMafiTrailers(String docId, FilterRequestDto filters, Pageable pageable, LocalDate startDate, LocalDate endDate) {

        log.info("get LOR MafiTrailerUpdateForm started for docId={}", docId);

        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filtersList = documentService.resolveDateFilters(filters,"TRANSACTION_DATE", startDate,
                endDate);


        RawSearchResult raw = documentService.search(docId, filtersList, operator, pageable, isDeleted, "DOC_REF",
                "JOB_NO");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        log.info("get LOR MafiTrailerUpdateForm completed for docId={} count={}", docId, page.getNumber());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }
}
