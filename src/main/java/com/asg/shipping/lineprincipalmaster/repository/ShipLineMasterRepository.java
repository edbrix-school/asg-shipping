package com.asg.shipping.lineprincipalmaster.repository;

import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ShipLineMaster entity
 */
@Repository
public interface ShipLineMasterRepository extends JpaRepository<ShipLineMaster, Long> {

    /**
     * Find line by POID and Group POID
     */
    Optional<ShipLineMaster> findByLinePoidAndGroupPoid(Long linePoid, Long groupPoid);

    /**
     * Check if line code exists for the given group (excluding deleted records)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMasterPrincipal l WHERE l.lineCode = :lineCode AND l.groupPoid = :groupPoid AND l.deleted = 'N'")
    boolean existsByLineCodeAndGroupPoid(@Param("lineCode") String lineCode, @Param("groupPoid") Long groupPoid);

    /**
     * Check if line code exists for the given group excluding a specific line (for updates)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMasterPrincipal l WHERE l.lineCode = :lineCode AND l.groupPoid = :groupPoid AND l.linePoid != :excludeLinePoid AND l.deleted = 'N'")
    boolean existsByLineCodeAndGroupPoidExcluding(@Param("lineCode") String lineCode, @Param("groupPoid") Long groupPoid, @Param("excludeLinePoid") Long excludeLinePoid);

    /**
     * Check if line name exists for the given group (excluding deleted records)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMasterPrincipal l WHERE l.lineName = :lineName AND l.groupPoid = :groupPoid AND l.deleted = 'N'")
    boolean existsByLineNameAndGroupPoid(@Param("lineName") String lineName, @Param("groupPoid") Long groupPoid);

    /**
     * Check if line name exists for the given group excluding a specific line (for updates)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMasterPrincipal l WHERE l.lineName = :lineName AND l.groupPoid = :groupPoid AND l.linePoid != :excludeLinePoid AND l.deleted = 'N'")
    boolean existsByLineNameAndGroupPoidExcluding(@Param("lineName") String lineName, @Param("groupPoid") Long groupPoid, @Param("excludeLinePoid") Long excludeLinePoid);
}

