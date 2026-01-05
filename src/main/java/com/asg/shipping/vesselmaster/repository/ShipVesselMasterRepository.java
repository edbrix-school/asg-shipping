package com.asg.shipping.vesselmaster.repository;

import com.asg.shipping.vesselmaster.entity.ShipVesselMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ShipVesselMaster entity
 */
@Repository
public interface ShipVesselMasterRepository extends JpaRepository<ShipVesselMaster, Long> {

    /**
     * Find vessel by POID and Group POID
     */
    Optional<ShipVesselMaster> findByVesselPoidAndGroupPoid(Long vesselPoid, Long groupPoid);

    /**
     * Check if IMO number exists (excluding deleted records)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselMaster v WHERE v.imoNumber = :imoNumber AND v.groupPoid = :groupPoid AND v.deleted = 'N'")
    boolean existsByImoNumberAndGroupPoid(@Param("imoNumber") String imoNumber, @Param("groupPoid") Long groupPoid);

    /**
     * Check if IMO number exists excluding a specific vessel (for updates)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselMaster v WHERE v.imoNumber = :imoNumber AND v.groupPoid = :groupPoid AND v.vesselPoid != :excludeVesselPoid AND v.deleted = 'N'")
    boolean existsByImoNumberAndGroupPoidExcluding(@Param("imoNumber") String imoNumber, @Param("groupPoid") Long groupPoid, @Param("excludeVesselPoid") Long excludeVesselPoid);
}

