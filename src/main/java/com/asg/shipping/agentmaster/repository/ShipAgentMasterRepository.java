package com.asg.shipping.agentmaster.repository;

import com.asg.shipping.agentmaster.entity.ShipAgentMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipAgentMasterRepository  extends JpaRepository<ShipAgentMasterEntity, Long> {
}
