package com.asg.shipping.agentMaster.repository;

import com.asg.shipping.agentMaster.entity.ShipAgentMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipAgentMasterRepository  extends JpaRepository<ShipAgentMasterEntity, Long> {
}
