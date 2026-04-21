package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.entity.ShipLineMasterTypeId;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShipLineMasterTypeRepository extends JpaRepository<ShipLineMasterType, ShipLineMasterTypeId> {

    /**
     * Load container types linked to a Line (SHIP_LINE_MASTER_TYPE_DTL.LINE_POID),
     * returning master entities from SHIP_CONTAINER_TYPE_MASTER.
     */
    @Query("""
            select distinct m
            from ShipLineMasterType d, ShipContainerTypeMaster m
            where d.containerTypePoid = m.containerTypePoid
              and d.linePoid = :linePoid
              and d.containerTypePoid is not null
            order by m.containerTypeCode
            """)
    List<ShipContainerTypeMaster> findContainerTypeMastersByLine(@Param("linePoid") Long linePoid);

    /**
     * Find all container type details for a line ordered by detail row ID
     */
    @Query("select d from ShipLineMasterType d where d.linePoid = :linePoid order by d.detRowId")
    List<ShipLineMasterType> findByLinePoidOrderByDetRowId(@Param("linePoid") Long linePoid);

    /**
     * Find the maximum detail row ID for a line
     */
    @Query("select max(d.detRowId) from ShipLineMasterType d where d.linePoid = :linePoid")
    Long findMaxDetRowIdByLinePoid(@Param("linePoid") Long linePoid);

    /**
     * Check if a container type POID exists for a line
     */
    @Query("select case when count(d) > 0 then true else false end from ShipLineMasterType d where d.linePoid = :linePoid and d.containerTypePoid = :containerTypePoid")
    boolean existsByLinePoidAndContainerTypePoid(@Param("linePoid") Long linePoid, @Param("containerTypePoid") Long containerTypePoid);

    /**
     * Check if a container type POID exists for a line, excluding a specific detail row
     */
    @Query("select case when count(d) > 0 then true else false end from ShipLineMasterType d where d.linePoid = :linePoid and d.containerTypePoid = :containerTypePoid and d.detRowId != :detRowId")
    boolean existsByLinePoidAndContainerTypePoidExcluding(@Param("linePoid") Long linePoid, @Param("containerTypePoid") Long containerTypePoid, @Param("detRowId") Long detRowId);

    /**
     * Load container types for a line excluding already-used poids, active only (VALID_UNTIL is null or >= today)
     */
    @Query("""
            select distinct d.containerTypePoid
            from ShipLineMasterType d
            where d.linePoid = :linePoid
              and d.containerTypePoid is not null
              and d.containerTypePoid not in :excludedPoids
              and (d.validUntil is null or d.validUntil >= current_date)
            order by d.containerTypePoid
            """)
    List<Long> findAvailableContainerTypePoids(
            @Param("linePoid") Long linePoid,
            @Param("excludedPoids") List<Long> excludedPoids
    );
}
