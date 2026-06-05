package com.asg.shipping.portMaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.entity.GlobalCountryMaster;
import com.asg.shipping.common.repository.GlobalCountryMasterRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;
import com.asg.shipping.portMaster.entity.PortMaster;
import com.asg.shipping.portMaster.entity.PortMasterId;
import com.asg.shipping.portMaster.repository.PortMasterRepository;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PortMasterServiceImpl implements PortMasterService {

	private final PortMasterRepository repository;
	private final GlobalCountryMasterRepository countryRepository;
	private final DocumentSearchService documentService;
	private final ShipTradeLaneService tradeLaneService;
	private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    
    private static final String PORT_NOT_FOUND="Port not found";

	@Override
	public Map<String, Object> createPort(PortMasterRequest request) {

        Long groupPoid = UserContext.getGroupPoid();
		repository.findByGroupPoidAndPortCodeIgnoreCase(groupPoid, request.getPortCode()).ifPresent(p -> {
			throw new IllegalArgumentException("Port Code already exists");
		});

		repository.findByGroupPoidAndPortNameIgnoreCase(groupPoid, request.getPortName()).ifPresent(p -> {
			throw new IllegalStateException("Port Name already exists");
		});

		PortMaster entity = new PortMaster();
		entity.setGroupPoid(groupPoid);
		entity.setPortCode(request.getPortCode());
		entity.setPortName(request.getPortName());
		entity.setPortName2(request.getPortName2());
		entity.setGlobalPortCode(request.getGlobalPortCode());
		entity.setCountryPoid(request.getCountryPoid());
		entity.setTradelanePoid(request.getTradelanePoid());
		entity.setBerths(request.getBerths());
		entity.setSeqno(request.getSeqno());
		entity.setActive(request.getActive() != null ? request.getActive() : "Y");
		entity.setDeleted("N");

		repository.save(entity);
		Long portPoid = repository.findByGroupPoidAndPortCode(groupPoid, request.getPortCode())
				.map(PortMaster::getPortPoid).orElseThrow(() -> new RuntimeException("Port not found after save"));
		loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), portPoid.toString());
		return Map.of("portPoid", portPoid);
	}

	@Transactional
	public PortMasterResponse updatePort( Long portPoid, PortMasterRequest request) {

        Long groupPoid = UserContext.getGroupPoid();
		PortMaster entity = repository.findById(new PortMasterId(groupPoid, portPoid))
				.orElseThrow(() -> new RuntimeException(PORT_NOT_FOUND));

        PortMaster oldData = new PortMaster();
        BeanUtils.copyProperties(entity, oldData);

		if (!Objects.equals(entity.getPortCode(), request.getPortCode())) {

			repository.findByPortCodeIgnoreCase(request.getPortCode())
					.filter(pm -> !Objects.equals(pm.getPortPoid(), entity.getPortPoid())).ifPresent(pm -> {
						throw new IllegalArgumentException("Port Code already exists");
					});

			entity.setPortCode(request.getPortCode());
		}

		if (!Objects.equals(entity.getPortName(), request.getPortName())) {

			repository.findByPortNameIgnoreCase(request.getPortName())
					.filter(pm -> !Objects.equals(pm.getPortPoid(), entity.getPortPoid())).ifPresent(pm -> {
						throw new IllegalArgumentException("Port Name already exists");
					});

			entity.setPortName(request.getPortName());
		}

		entity.setActive(request.getActive());
		entity.setBerths(request.getBerths());
        entity.setPortName2(request.getPortName2());
		entity.setCountryPoid(request.getCountryPoid());
		entity.setTradelanePoid(request.getTradelanePoid());
		entity.setGlobalPortCode(request.getGlobalPortCode());
		entity.setSeqno(request.getSeqno());

		repository.save(entity);
		String key = entity.getPortPoid().toString();
		String docId = UserContext.getDocumentId();
		loggingService.logChanges(oldData, entity, PortMaster.class, docId, key, LogDetailsEnum.MODIFIED, "PORT_POID");
		return getPortById(portPoid);
	}

	@Override
	public Map<String, Object> getAllPorts(String docId, FilterRequestDto request, Pageable pageable) {
		return listPorts(docId, request, pageable);
	}

	@Override
	public PortMasterResponse getPortById( Long portPoid) {
        Long groupPoid = UserContext.getGroupPoid();
		PortMaster entity=repository.findById(new PortMasterId(groupPoid, portPoid)).orElseThrow(() -> new RuntimeException(PORT_NOT_FOUND));
		
		ShipTradelaneResponse tradeLaneResponse=tradeLaneService.getById(entity.getTradelanePoid());
		
		GlobalCountryMaster countryMaster=countryRepository.findById(entity.getCountryPoid()).orElseThrow(()-> new ResourceNotFoundException("Country Resource", "CountryPoid", null));
		
		return mapToResponse(entity, tradeLaneResponse, countryMaster);
		
	}

	@Override
	public void deletePort( Long portPoid, @Valid DeleteReasonDto deleteReasonDto) {
        Long groupPoid = UserContext.getGroupPoid();
		PortMaster entity = repository.findById(new PortMasterId(groupPoid, portPoid))
				.orElseThrow(() -> new RuntimeException(PORT_NOT_FOUND));

		if (entity.getDeleted().equalsIgnoreCase("Y"))
			throw new IllegalArgumentException("Port has already been deleted.");

        documentDeleteService.deleteDocument(portPoid, "SHIP_PORT_MASTER", "PORT_POID", deleteReasonDto,
                null);

	}

	private Map<String, Object> listPorts(String docId, FilterRequestDto request, Pageable pageable) {

		String operator = documentService.resolveOperator(request);
		String isDeleted = documentService.resolveIsDeleted(request);
		List<FilterDto> filters = documentService.resolveFilters(request);

		RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "PORT_NAME", // label
				"PORT_POID");

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	private PortMasterResponse mapToResponse(PortMaster entity,ShipTradelaneResponse tradeLaneResponse,GlobalCountryMaster countryMaster) {
		PortMasterResponse dto = new PortMasterResponse();
		dto.setPortPoid(entity.getPortPoid());
		dto.setPortCode(entity.getPortCode());
		dto.setPortName(entity.getPortName());
		dto.setPortName2(entity.getPortName2());
		dto.setGlobalPortCode(entity.getGlobalPortCode());
		dto.setCountryPoid(entity.getCountryPoid());
		dto.setTradelanePoid(entity.getTradelanePoid());
		dto.setBerths(entity.getBerths());
		dto.setSeqno(entity.getSeqno());
		dto.setActive(entity.getActive());
		dto.setCountryDetail(mapReadOnlyresponse(countryMaster));
		dto.setTradelaneDetail(mapReadOnlyresponse(tradeLaneResponse));
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
		return dto;
	}

    private Map<String, Object> mapReadOnlyresponse(Object data) {
        Map<String, Object> dto = new HashMap<>();

        if (data instanceof ShipTradelaneResponse entity) {
            dto.put("poid", entity.getTradeLanePoid());
            dto.put("code", entity.getTradeLaneCode());
            dto.put("description", entity.getTradeLaneName());
        }
        else if (data instanceof GlobalCountryMaster entity) {
            dto.put("poid", entity.getCountryPoid());
            dto.put("code", entity.getCountryCode());
            dto.put("description", entity.getCountryName());
        }

        return dto;
    }

}