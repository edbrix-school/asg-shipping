package com.asg.shipping.vesseltypemaster.repository;


import com.asg.shipping.vesseltypemaster.entity.ShipVesselTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShipVesselTypeMaster entity
 */
@Repository
public interface ShipVesselTypeMasterRepository extends JpaRepository<ShipVesselTypeMaster, Long> {

    /**
     * Find vessel type by ID and group POID
     */
    Optional<ShipVesselTypeMaster> findByVesselTypePoidAndGroupPoid(Long vesselTypePoid, Long groupPoid);
}
