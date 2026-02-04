package com.asg.shipping.MafiTrailerDateUpdateForm.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtlId;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiDetailDto;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormResponse;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafitrailerHeaderDTO;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.VoyageProjection;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtl;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiDtlRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipBlMafiHdrRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.repository.ShipReadOnlyRepository;
import com.asg.shipping.MafiTrailerDateUpdateForm.util.MafiTrailerDateUpdateFormMapper;
import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
	public Map<String, Object> getAll(String docId, FilterRequestDto request, Pageable pageable) {

		return listMafiTrailers(docId, request, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public MafiTrailerDateUpdateFormResponse getById(Long transactionPoid, Long groupPoid, Long companyPoid) {

		ShipBlMafiHdr header = headerRepository
				.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(transactionPoid, groupPoid, companyPoid, "N")
				.orElseThrow(() -> new EntityNotFoundException(
						"Mafi trailer entry not found for transactionPoid: " + transactionPoid));

		List<ShipBlMafiDtl> details = detailRepository.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid);

		VoyageProjection voyage = readOnlyRepository.findVoyageDetailsById(header.getVoyageTransactionPoid())
				.orElseThrow(
						() -> new EntityNotFoundException("Voyage not found for transactionPoid: " + transactionPoid));

		return mapper.toMafiTrailerResponse(header, voyage, details);
	}

	@Override
	@Transactional
	public void update(Long transactionPoid, MafiTrailerDateUpdateFormRequest request, Long groupPoid, Long companyPoid,
			String userId) {

		boolean anyUpdateDone = false;

		ShipBlMafiHdr existingEntity = headerRepository
				.findByTransactionPoidAndGroupPoidAndCompanyPoidAndDeleted(transactionPoid, groupPoid, companyPoid, "N")
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
			anyUpdateDone = true;
		}

		List<ShipBlMafiDtl> existingDetails = detailRepository
				.findByIdTransactionPoidOrderByIdDetRowId(transactionPoid);

		Map<Long, ShipBlMafiDtl> detailMap = existingDetails.stream()
				.collect(Collectors.toMap(d -> d.getId().getDetRowId(), d -> d));

        List<LogRequestDto<ShipBlMafiDtl>> logRequests = new ArrayList<>();

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

		for (MafiDetailDto dto : request.getMafiDetails()) {

			ShipBlMafiDtl entity = detailMap.get(dto.getDetRowId());
			if (entity == null) {
				continue;
			}

			boolean changed = !Objects.equals(dto.getRemarks(), entity.getRemarks())
					|| !Objects.equals(dto.getBackLoadDate(), entity.getBackLoadDate())
					|| !Objects.equals(dto.getMafiEmptyDate(), entity.getMafiEmptyDate());

			if (changed) {
				detailRepository.updateByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId(),
						dto.getRemarks(), dto.getBackLoadDate(), dto.getMafiEmptyDate(), userId);
				anyUpdateDone = true;
                ShipBlMafiDtl newEntity=new ShipBlMafiDtl();
                ShipBlMafiDtlId id=new ShipBlMafiDtlId();
                id.setTransactionPoid(transactionPoid);
                id.setDetRowId(dto.getDetRowId());
                newEntity.setId(id);
                newEntity.setBlPoid(dto.getBlPoid());
                newEntity.setMafiRef(dto.getMafiRef());
                newEntity.setMafiSize(dto.getMafiSize());
                newEntity.setMafiFreeDays(dto.getMafiFreeDays());
                newEntity.setBackLoadDate(dto.getBackLoadDate());
                newEntity.setMafiEmptyDate(dto.getMafiEmptyDate());
                newEntity.setRemarks(dto.getRemarks());
                newEntity.setCreatedBy(entity.getCreatedBy());
                newEntity.setCreatedDate(entity.getCreatedDate());
                newEntity.setLastModifiedBy(userId);
                newEntity.setLastModifiedDate(LocalDateTime.now());
                logRequests.add(new LogRequestDto<>(entity, newEntity, ShipBlMafiDtl.class, docId,
                        docKeyPoid, "BLMAFIDTL DET_ROW_ID: " + dto.getDetRowId()));
			}
		}

		if (!anyUpdateDone) {
			throw new IllegalStateException("No data to update.");
		}
        loggingService.createLogBatch(logRequests);
		loggingService.logChanges(existingEntity, headerEntity, ShipBlMafiHdr.class, docId, transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");
		
	}

	private Map<String, Object> listMafiTrailers(String docId, FilterRequestDto request, Pageable pageable) {

		String operator = documentService.resolveOperator(request);
		String isDeleted = documentService.resolveIsDeleted(request);
		List<FilterDto> filters = documentService.resolveFilters(request);

		RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "DOC_REF",
				"JOB_NO");

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

		return PaginationUtil.wrapPage(page, raw.displayFields());
	}
}
