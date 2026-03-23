package com.asg.shipping.portmaster.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.portmaster.dto.PortMasterRequest;
import com.asg.shipping.portmaster.dto.PortMasterResponse;

public interface PortMasterService {

	Map<String, Object> createPort(PortMasterRequest request);

	PortMasterResponse updatePort( Long portPoid, PortMasterRequest request);

	Map<String, Object> getAllPorts(String docId, FilterRequestDto request, Pageable pageable);

	PortMasterResponse getPortById( Long portPoid);

	void deletePort(Long portPoid);
}
