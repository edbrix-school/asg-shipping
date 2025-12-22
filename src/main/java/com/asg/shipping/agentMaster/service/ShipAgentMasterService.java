package com.asg.shipping.agentMaster.service;

import com.asg.shipping.agentMaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterResponseDto;

import java.util.Map;

public interface ShipAgentMasterService {

    ShipAgentMasterResponseDto createAgentMaster(ShipAgentMasterRequestDto request);

    ShipAgentMasterResponseDto updateAgentMaster(Long agentPoid, ShipAgentMasterRequestDto request);

    ShipAgentMasterResponseDto findByIdAgentMaster(Long agentPoid);

    void deleteAgentMaster(Long agentPoid);

    Map<String, Object> listAgents(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);
}
