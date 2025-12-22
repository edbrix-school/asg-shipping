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

    /**
     * Check if vessel type code exists globally (excluding deleted records)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselTypeMaster v " +
            "WHERE v.vesselTypeCode = :code AND v.deleted = 'N'")
    boolean existsByVesselTypeCode(@Param("code") String code);

    /**
     * Check if vessel type code exists globally excluding a specific POID (for updates - though code is not updateable)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselTypeMaster v " +
            "WHERE v.vesselTypeCode = :code AND v.vesselTypePoid != :excludePoid AND v.deleted = 'N'")
    boolean existsByVesselTypeCodeExcludingPoid(
            @Param("code") String code,
            @Param("excludePoid") Long excludePoid
    );

    /**
     * Check if vessel type name exists globally (excluding deleted records)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselTypeMaster v " +
            "WHERE v.vesselTypeName = :name AND v.deleted = 'N'")
    boolean existsByVesselTypeName(@Param("name") String name);

    /**
     * Check if vessel type name exists globally excluding a specific POID (for updates)
     */
    @Query("SELECT COUNT(v) > 0 FROM ShipVesselTypeMaster v " +
            "WHERE v.vesselTypeName = :name AND v.vesselTypePoid != :excludePoid AND v.deleted = 'N'")
    boolean existsByVesselTypeNameExcludingPoid(
            @Param("name") String name,
            @Param("excludePoid") Long excludePoid
    );
}
