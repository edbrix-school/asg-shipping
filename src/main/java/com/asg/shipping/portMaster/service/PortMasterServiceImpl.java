package com.asg.shipping.portMaster.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PortMasterServiceImpl implements PortMasterService {

	private final PortMasterRepository repository;
	private final GlobalCountryMasterRepository countryRepository;
	private final DocumentSearchService documentService;
	private final ShipTradeLaneService tradeLaneService; 

	@Override
	public Map<String, Object> createPort(Long groupPoid, PortMasterRequest request, String userId) {

		repository.findByGroupPoidAndPortCode(groupPoid, request.getPortCode()).ifPresent(p -> {
			throw new RuntimeException("Port Code already exists");
		});

		repository.findByGroupPoidAndPortName(groupPoid, request.getPortName()).ifPresent(p -> {
			throw new RuntimeException("Port Name already exists");
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
		entity.setCreatedBy(userId);
		entity.setCreatedDate(LocalDateTime.now());

		repository.save(entity);
		Long portPoid = repository.findByGroupPoidAndPortCode(groupPoid, request.getPortCode())
				.map(PortMaster::getPortPoid).orElseThrow(() -> new RuntimeException("Port not found after save"));
		return Map.of("portPoid", portPoid);
	}

	@Transactional
	public PortMasterResponse updatePort(Long groupPoid, Long portPoid, PortMasterRequest request, String userId) {

		PortMaster entity = repository.findById(new PortMasterId(groupPoid, portPoid))
				.orElseThrow(() -> new RuntimeException("Port not found"));

		if (!Objects.equals(entity.getPortCode(), request.getPortCode())) {

			repository.findByPortCode(request.getPortCode())
					.filter(pm -> !Objects.equals(pm.getPortPoid(), entity.getPortPoid())).ifPresent(pm -> {
						throw new RuntimeException("PortCode already exists");
					});

			entity.setPortCode(request.getPortCode());
		}

		if (!Objects.equals(entity.getPortName(), request.getPortName())) {

			repository.findByPortName(request.getPortName())
					.filter(pm -> !Objects.equals(pm.getPortPoid(), entity.getPortPoid())).ifPresent(pm -> {
						throw new RuntimeException("PortName already exists");
					});

			entity.setPortName(request.getPortName());
		}

		entity.setActive(request.getActive());
		entity.setBerths(request.getBerths());
		entity.setCountryPoid(request.getCountryPoid());
		entity.setTradelanePoid(request.getTradelanePoid());
		entity.setGlobalPortCode(request.getGlobalPortCode());
		entity.setSeqno(request.getSeqno());

		entity.setLastModifiedBy(userId);
		entity.setLastModifiedDate(LocalDateTime.now());

		repository.save(entity);

		return getPortById(groupPoid, portPoid);
	}

	@Override
	public Map<String, Object> getAllPorts(String docId, FilterRequestDto request, Pageable pageable) {
		return listPorts(docId, request, pageable);
	}

	@Override
	public PortMasterResponse getPortById(Long groupPoid, Long portPoid) {

		PortMaster entity=repository.findById(new PortMasterId(groupPoid, portPoid)).orElseThrow(() -> new RuntimeException("Port not found"));
		
		ShipTradelaneResponse tradeLaneResponse=tradeLaneService.getById(entity.getTradelanePoid());
		
		GlobalCountryMaster countryMaster=countryRepository.findById(entity.getCountryPoid()).orElseThrow(()-> new ResourceNotFoundException("Country Resource", "CountryPoid", null));
		
		return mapToResponse(entity, tradeLaneResponse, countryMaster);
		
	}

	@Override
	public void deletePort(Long groupPoid, Long portPoid, String userId) {
		PortMaster entity = repository.findById(new PortMasterId(groupPoid, portPoid))
				.orElseThrow(() -> new RuntimeException("Port not found"));

		if (entity.getDeleted().equalsIgnoreCase("Y"))
			throw new RuntimeException("Port has already been deleted.");

		entity.setDeleted("Y");
		entity.setLastModifiedBy(userId);
		entity.setLastModifiedDate(LocalDateTime.now());
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
		dto.setCountryDetail(mapReadOnlyresponse(tradeLaneResponse));
		dto.setTradelaneDetail(mapReadOnlyresponse(countryMaster));
		return dto;
	}
	
	private Map<String,Object> mapReadOnlyresponse(Object data){
		Map<String,Object> dto=new HashMap<>();
		if(data instanceof ShipTradelaneResponse) {
			ShipTradelaneResponse entity=(ShipTradelaneResponse) data;
			dto.put("poid", entity.getTradeLanePoid());
			dto.put("code", entity.getTradeLaneCode());
			dto.put("description", entity.getTradeLaneName());
		}
		else if(data instanceof GlobalCountryMaster) {
			GlobalCountryMaster entity=(GlobalCountryMaster) data;
			dto.put("poid", entity.getCountryPoid());
			dto.put("code", entity.getCountryCode());
			dto.put("description", entity.getCountryName());
		}
		
		return dto;
	}

}