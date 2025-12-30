package com.asg.shipping.customerautochargeexportbl.repository;

import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
;
import org.springframework.stereotype.Repository;


import java.util.Optional;


@Repository
public interface ShipCustomerChargesHdrRepository extends JpaRepository<ShipCustomerChargesHdrEntity, Long> {

    Optional<ShipCustomerChargesHdrEntity> findByDocRefAndGroupPoidAndDeleted(String docRef, Long groupPoid, String deleted);

    Optional<ShipCustomerChargesHdrEntity> findByDocRefAndDeleted(String docRef, String n);
}

