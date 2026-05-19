package com.asg.shipping.portMaster.service;

import java.util.Map;

import com.asg.common.lib.dto.DeleteReasonDto;
import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;

public interface PortMasterService {

	Map<String, Object> createPort(PortMasterRequest request);

	PortMasterResponse updatePort( Long portPoid, PortMasterRequest request);

	Map<String, Object> getAllPorts(String docId, FilterRequestDto request, Pageable pageable);

	PortMasterResponse getPortById( Long portPoid);

	void deletePort(Long portPoid, DeleteReasonDto deleteReasonDto);
}
