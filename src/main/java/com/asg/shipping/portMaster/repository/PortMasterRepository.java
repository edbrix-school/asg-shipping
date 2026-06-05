package com.asg.shipping.portMaster.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.asg.shipping.portMaster.entity.PortMaster;
import com.asg.shipping.portMaster.entity.PortMasterId;

public interface PortMasterRepository extends JpaRepository<PortMaster, PortMasterId> {

	Optional<PortMaster> findByGroupPoidAndPortCode(Long groupPoid, String portCode);

	boolean existsByPortPoidAndGroupPoid(Long portPoid, Long groupPoid);

	@Query("SELECT p FROM PortMaster p WHERE p.groupPoid = :groupPoid AND LOWER(p.portCode) = LOWER(:portCode)")
	Optional<PortMaster> findByGroupPoidAndPortCodeIgnoreCase(@Param("groupPoid") Long groupPoid, @Param("portCode") String portCode);

	@Query("SELECT p FROM PortMaster p WHERE p.groupPoid = :groupPoid AND LOWER(p.portName) = LOWER(:portName)")
	Optional<PortMaster> findByGroupPoidAndPortNameIgnoreCase(@Param("groupPoid") Long groupPoid, @Param("portName") String portName);

	@Query("SELECT p FROM PortMaster p WHERE LOWER(p.portCode) = LOWER(:portCode)")
	Optional<PortMaster> findByPortCodeIgnoreCase(@Param("portCode") String portCode);

	@Query("SELECT p FROM PortMaster p WHERE LOWER(p.portName) = LOWER(:portName)")
	Optional<PortMaster> findByPortNameIgnoreCase(@Param("portName") String portName);

}
