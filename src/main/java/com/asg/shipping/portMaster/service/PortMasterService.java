package com.asg.shipping.portMaster.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;

public interface PortMasterService {

	void createPort(Long groupPoid, PortMasterRequest request, String userId);

	PortMasterResponse updatePort(Long groupPoid, Long portPoid, PortMasterRequest request, String userId);

	Map<String, Object> getAllPorts(String docId, FilterRequestDto request, Pageable pageable);

	PortMasterResponse getPortById(Long groupPoid, Long portPoid);

	void deletePort(Long groupPoid, Long portPoid, String userId);
}
