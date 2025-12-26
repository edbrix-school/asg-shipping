package com.asg.shipping.vesselmaster.service;

import com.asg.shipping.common.dto.LovItem;

import java.util.List;

/**
 * Service interface for Vessel Master LOV (List of Values) operations
 */
public interface VesselMasterLovService {

    /**
     * Get LINE_MASTER LOV list
     * Filters by LinePoid if provided
     *
     * @param linePoid Optional Line POID to filter by
     * @return List of LovItem containing POID, CODE, and DESCRIPTION
     */
    List<LovItem> getLineMasterLov(Long linePoid);

    /**
     * Get VESSEL_TYPE LOV list
     * Filters by vesselTypePoid if provided
     *
     * @param vesselTypePoid Optional Vessel Type POID to filter by
     * @return List of LovItem containing POID, CODE, and DESCRIPTION
     */
    List<LovItem> getVesselTypeLov(Long vesselTypePoid);

    /**
     * Get AGENT_MASTER LOV list
     * Filters by AgentPoid if provided
     *
     * @param agentPoid Optional Agent POID to filter by
     * @return List of LovItem containing POID, CODE, and DESCRIPTION
     */
    List<LovItem> getAgentMasterLov(Long agentPoid);
}

