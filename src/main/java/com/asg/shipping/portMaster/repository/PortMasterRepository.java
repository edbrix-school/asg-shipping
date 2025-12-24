package com.asg.shipping.portMaster.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asg.shipping.portMaster.entity.PortMaster;
import com.asg.shipping.portMaster.entity.PortMasterId;

public interface PortMasterRepository extends JpaRepository<PortMaster, PortMasterId> {

	Optional<PortMaster> findByGroupPoidAndPortCode(Long groupPoid, String portCode);

	Optional<PortMaster> findByGroupPoidAndPortName(Long groupPoid, String portName);

	List<PortMaster> findByGroupPoidAndDeletedNot(Long groupPoid, String deleted);

	Optional<PortMaster> findByPortCode(String portCode);

	Optional<PortMaster> findByPortName(String portName);

	boolean existsByPortPoidAndGroupPoid(Long portPoid, Long groupPoid);

}
