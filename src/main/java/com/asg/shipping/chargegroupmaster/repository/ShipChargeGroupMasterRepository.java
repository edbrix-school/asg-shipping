package com.asg.shipping.chargegroupmaster.repository;

import com.asg.shipping.chargegroupmaster.entity.ShipChargeGroupMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipChargeGroupMasterRepository extends JpaRepository<ShipChargeGroupMaster, Long> {
    Optional<ShipChargeGroupMaster> findByChargeGroupCodeIgnoreCase(String code);
    Optional<ShipChargeGroupMaster> findByChargeGroupNameIgnoreCase(String name);
    boolean existsByChargeGroupNameAndChargeGroupPoidNot(String chargeGroupName, Long poid);
    boolean existsByChargeGroupCodeAndChargeGroupPoidNot(String chargeGroupCode, Long poid);

    boolean existsByChargeGroupNameIgnoreCaseAndChargeGroupPoidNot(
            String chargeGroupName, Long chargeGroupPoid);

    boolean existsByChargeGroupCodeIgnoreCaseAndChargeGroupPoidNot(
            String chargeGroupCode, Long chargeGroupPoid);

}
