package com.asg.shipping.chargegroupmaster.repository;

import com.asg.shipping.chargegroupmaster.entity.ShipChargeGroupMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipChargeGroupMasterRepository extends JpaRepository<ShipChargeGroupMaster, Long> {
    Optional<ShipChargeGroupMaster> findByChargeGroupCode(String code);
    Optional<ShipChargeGroupMaster> findByChargeGroupName(String name);

}
