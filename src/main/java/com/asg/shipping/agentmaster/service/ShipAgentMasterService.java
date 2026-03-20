package com.asg.shipping.agentmaster.service;

import com.asg.shipping.agentmaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentmaster.dto.ShipAgentMasterResponseDto;

import java.util.Map;

public interface ShipAgentMasterService {

    ShipAgentMasterResponseDto createAgentMaster(ShipAgentMasterRequestDto request);

    ShipAgentMasterResponseDto updateAgentMaster(Long agentPoid, ShipAgentMasterRequestDto request);

    ShipAgentMasterResponseDto findByIdAgentMaster(Long agentPoid);

    void deleteAgentMaster(Long agentPoid);

    Map<String, Object> listAgents(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);
}
