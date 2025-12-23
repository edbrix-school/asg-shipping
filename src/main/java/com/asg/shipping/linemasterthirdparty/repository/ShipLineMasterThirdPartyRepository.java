package com.asg.shipping.linemasterthirdparty.repository;

import com.asg.shipping.linemasterthirdparty.entity.ShipLineMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ShipLineMaster entity (Third Party lines only)
 * All queries filter by LINE_TYPE = 'THIRD_PARTY'
 */
@Repository
public interface ShipLineMasterThirdPartyRepository extends JpaRepository<ShipLineMaster, Long> {

    /**
     * Find line by POID, Group POID, and LINE_TYPE = 'THIRD_PARTY'
     */
    @Query("SELECT l FROM ShipLineMaster l WHERE l.linePoid = :linePoid AND l.groupPoid = :groupPoid AND l.lineType = 'THIRD_PARTY'")
    Optional<ShipLineMaster> findByLinePoidAndGroupPoidAndThirdParty(@Param("linePoid") Long linePoid, @Param("groupPoid") Long groupPoid);

    /**
     * Check if line code exists for the given group with LINE_TYPE = 'THIRD_PARTY' (excluding deleted records)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMaster l WHERE l.lineCode = :lineCode AND l.groupPoid = :groupPoid AND l.lineType = 'THIRD_PARTY' AND l.deleted = 'N'")
    boolean existsByLineCodeAndGroupPoidAndThirdParty(@Param("lineCode") String lineCode, @Param("groupPoid") Long groupPoid);

    /**
     * Check if line code exists for the given group with LINE_TYPE = 'THIRD_PARTY' excluding a specific line (for updates)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMaster l WHERE l.lineCode = :lineCode AND l.groupPoid = :groupPoid AND l.lineType = 'THIRD_PARTY' AND l.linePoid != :excludeLinePoid AND l.deleted = 'N'")
    boolean existsByLineCodeAndGroupPoidAndThirdPartyExcluding(@Param("lineCode") String lineCode, @Param("groupPoid") Long groupPoid, @Param("excludeLinePoid") Long excludeLinePoid);

    /**
     * Check if line name exists for the given group with LINE_TYPE = 'THIRD_PARTY' (excluding deleted records)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMaster l WHERE l.lineName = :lineName AND l.groupPoid = :groupPoid AND l.lineType = 'THIRD_PARTY' AND l.deleted = 'N'")
    boolean existsByLineNameAndGroupPoidAndThirdParty(@Param("lineName") String lineName, @Param("groupPoid") Long groupPoid);

    /**
     * Check if line name exists for the given group with LINE_TYPE = 'THIRD_PARTY' excluding a specific line (for updates)
     */
    @Query("SELECT COUNT(l) > 0 FROM ShipLineMaster l WHERE l.lineName = :lineName AND l.groupPoid = :groupPoid AND l.lineType = 'THIRD_PARTY' AND l.linePoid != :excludeLinePoid AND l.deleted = 'N'")
    boolean existsByLineNameAndGroupPoidAndThirdPartyExcluding(@Param("lineName") String lineName, @Param("groupPoid") Long groupPoid, @Param("excludeLinePoid") Long excludeLinePoid);
}

