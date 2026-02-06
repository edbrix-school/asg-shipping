package com.asg.shipping.collectionhandover.service;

import javax.sql.DataSource;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.collectionhandover.dto.*;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtlId;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverHdrRepository;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverDtlRepository;
import com.asg.shipping.collectionhandover.util.CollectionHandoverMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JasperReport;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Collection Handover operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionHandoverServiceImpl implements CollectionHandoverService {

    private final CollectionHandoverHdrRepository headerRepository;
    private final CollectionHandoverDtlRepository detailRepository;
    private final DocumentSearchService documentService;
    private final CollectionHandoverMapper mapper;
    private final LoggingService loggingService;
    private final PrintService printService;
	private final DataSource dataSource;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchCollectionHandovers(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        log.info("Searching collection handovers with docId: {}, page: {}, size: {}, startDate: {}, endDate: {}", docId, pageable.getPageNumber(), pageable.getPageSize(), startDate, endDate);

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);
        
        // Add date filters if provided
        if (startDate != null && endDate != null) {
            // Add date range filter for TRANSACTION_DATE field
            filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
        }

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DOC_REF",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionHandoverDto getCollectionHandover(Long id) {
        log.info("Getting collection handover with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Handover", "transactionPoid", id.toString()));

        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(id);

        CollectionHandoverDto dto = mapper.mapToDto(handover, detailList);
        enrichLovData(dto);

        log.info("Successfully retrieved collection handover with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public CollectionHandoverDto createCollectionHandover(CollectionHandoverCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating collection handover");

        validateCreateDTO(dto, groupPoid);

        ArShDayEndCloseHdr handover = new ArShDayEndCloseHdr();
        mapper.mapCreateDTOToEntity(dto, handover, groupPoid, userPoid);

        ArShDayEndCloseHdr saved = headerRepository.save(handover);
        createDetailRecords(saved.getTransactionPoid(), dto.getDetails());

        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        CollectionHandoverDto result = mapper.mapToDto(saved, detailList);
        enrichLovData(result);

        log.info("Successfully created collection handover with id: {}", saved.getTransactionPoid());
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getTransactionPoid().toString());
        return result;
    }

    @Override
    @Transactional
    public CollectionHandoverDto updateCollectionHandover(Long id, CollectionHandoverUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating collection handover with id: {}", id);

        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Handover", "transactionPoid", id.toString()));

        validateUpdateDTO(dto, id, groupPoid);
        ArShDayEndCloseHdr oldHandover=new ArShDayEndCloseHdr();
        BeanUtils.copyProperties(handover, oldHandover);

        mapper.mapUpdateDTOToEntity(dto, handover, groupPoid, userPoid);
        ArShDayEndCloseHdr saved = headerRepository.save(handover);

        updateDetailRecords(id, dto.getDetails());

        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(id);

        CollectionHandoverDto result = mapper.mapToDto(saved, detailList);
        enrichLovData(result);

        log.info("Successfully updated collection handover with id: {}", id);
        loggingService.logChanges(oldHandover, handover, ArShDayEndCloseHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
        return result;
    }

    @Override
    @Transactional
    public void deleteCollectionHandover(Long id) {
        log.info("Deleting collection handover with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Handover", "transactionPoid", id.toString()));

        if ("Y".equals(handover.getDeleted())) {
            log.info("Collection handover with id: {} is already deleted", id);
            return;
        }

        handover.setDeleted("Y");
        handover.setLastModifiedBy(getCurrentUser());
        handover.setLastModifiedDate(LocalDateTime.now());

        headerRepository.save(handover);

        log.info("Successfully deleted collection handover with id: {}", id);
    }

    @Override
    @Transactional
    public void toggleVerifyStatus(Long id, String verifiedRcvd, String mainOfcRemarks) {
        log.info("Toggling verify status for collection handover with id: {} to {}", id, verifiedRcvd);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Handover", "transactionPoid", id.toString()));

        if (verifiedRcvd != null && !verifiedRcvd.matches("^[YN]$")) {
            throw new ValidationException("Verified received must be Y or N");
        }

        handover.setVerifiedRcvd(verifiedRcvd);
        if (mainOfcRemarks != null) {
            handover.setMainOfcRemarks(mainOfcRemarks);
        }
        handover.setLastModifiedBy(getCurrentUser());
        handover.setLastModifiedDate(LocalDateTime.now());

        headerRepository.save(handover);

        log.info("Successfully updated verify status for collection handover with id: {}", id);
    }
    
    @Override
	public byte[] print(Long transactionPoid) throws Exception {
		Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-114");
		params.put("SH_DAY_CLOSE_CASH_SUBREPORT_1",
				printService.load("Shipping/SH/SH_DAY_CLOSE_CASH_subreport1.jrxml"));
		params.put("SH_DAY_CLOSE_CHQ_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_CHQ_subreport1.jrxml"));
		params.put("SH_DAY_CLOSE_SMRY_SUBREPORT_1",
				printService.load("Shipping/SH/SH_DAY_CLOSE_SMRY_subreport1.jrxml"));
		JasperReport mainReport = printService.load("Shipping/SH/SH_DAY_CLOSE.jrxml");
		return printService.fillReportToPdf(mainReport, params, dataSource);
	}

    private void createDetailRecords(Long transactionPoid, List<CollectionHandoverDetailCreateDTO> details) {
        if (details == null || details.isEmpty()) {
            return;
        }

        List<ArShDayEndCloseDtl> entities = details.stream()
                .map(dto -> {
                    ArShDayEndCloseDtl detail = new ArShDayEndCloseDtl();
                    detail.setTransactionPoid(transactionPoid);
                    detail.setDetRowId(dto.getDetRowId());
                    mapper.mapDetailCreateDTOToEntity(dto, detail);
                    return detail;
                })
                .collect(Collectors.toList());

        detailRepository.saveAll(entities);
    }

    private void updateDetailRecords(Long transactionPoid, List<CollectionHandoverDetailUpdateDTO> details) {
        if (details == null) {
            return;
        }

        List<ArShDayEndCloseDtl> existing = detailRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingIds = existing.stream()
                .map(ArShDayEndCloseDtl::getDetRowId)
                .collect(Collectors.toSet());

        Set<Long> newIds = details.stream()
                .map(CollectionHandoverDetailUpdateDTO::getDetRowId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<Long> toDelete = existingIds.stream()
                .filter(id -> !newIds.contains(id))
                .collect(Collectors.toSet());

        if (!toDelete.isEmpty()) {
            toDelete.forEach(detRowId -> {
                ArShDayEndCloseDtlId id = new ArShDayEndCloseDtlId();
                id.setTransactionPoid(transactionPoid);
                id.setDetRowId(detRowId);
                detailRepository.deleteById(id);
            });
        }

        List<ArShDayEndCloseDtl> entities = details.stream()
                .map(dto -> {
                    ArShDayEndCloseDtl detail;
                    if (dto.getDetRowId() != null && existingIds.contains(dto.getDetRowId())) {
                        detail = detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .orElse(new ArShDayEndCloseDtl());
                    } else {
                        detail = new ArShDayEndCloseDtl();
                        detail.setTransactionPoid(transactionPoid);
                        detail.setDetRowId(dto.getDetRowId() != null ? dto.getDetRowId() : generateNextDetRowId(transactionPoid));
                    }

                    mapper.mapDetailUpdateDTOToEntity(dto, detail);
                    return detail;
                })
                .collect(Collectors.toList());

        detailRepository.saveAll(entities);
    }

    private Long generateNextDetRowId(Long transactionPoid) {
        return detailRepository.getMaxDetRowId(transactionPoid) + 1;
    }

    private void enrichLovData(CollectionHandoverDto dto) {
        // LOV enrichment can be implemented later if needed
        // For now, just log that enrichment was called
        log.debug("LOV enrichment called for collection handover");
    }

    private void validateCreateDTO(CollectionHandoverCreateDTO dto, Long groupPoid) {
        if (dto.getDocRef() != null && headerRepository.existsByDocRef(dto.getDocRef())) {
            throw new ValidationException("Document reference already exists: " + dto.getDocRef());
        }
    }

    private void validateUpdateDTO(CollectionHandoverUpdateDTO dto, Long id, Long groupPoid) {
        if (dto.getDocRef() != null && headerRepository.existsByDocRefExcludingPoid(dto.getDocRef(), id)) {
            throw new ValidationException("Document reference already exists: " + dto.getDocRef());
        }
    }
}