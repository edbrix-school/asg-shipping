package com.asg.shipping.groupcontainertypes.repository;

import com.asg.shipping.groupcontainertypes.entity.ShipContainerTypeGrpMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ShipContainerTypeGrpMasterRepository  extends JpaRepository<ShipContainerTypeGrpMaster, Long> {

    /**
     * Find container group by POID and group POID
     */
    Optional<ShipContainerTypeGrpMaster> findByContainerGrpPoidAndGroupPoid(Long containerGrpPoid, Long groupPoid);

    /**
     * Check if container group code exists per group (excluding deleted records)
     */
    @Query("""
       SELECT COUNT(c) > 0 
       FROM ShipContainerTypeGrpMaster c
       WHERE c.containerGrpCode = :code
         AND c.groupPoid = :groupPoid
         AND (c.deleted IS NULL OR c.deleted = 'N')
       """)
    boolean existsByContainerGrpCodeAndGroupPoid(
            @Param("code") String code,
            @Param("groupPoid") Long groupPoid
    );
    /**
     * Check if container group code exists per group excluding a specific POID (for updates)
     */
    @Query("""
       SELECT COUNT(c) > 0
       FROM ShipContainerTypeGrpMaster c
       WHERE c.containerGrpCode = :code
         AND c.groupPoid = :groupPoid
         AND c.containerGrpPoid != :excludePoid
         AND (c.deleted IS NULL OR c.deleted = 'N')
       """)
    boolean existsByContainerGrpCodeAndGroupPoidExcludingPoid(
            @Param("code") String code,
            @Param("groupPoid") Long groupPoid,
            @Param("excludePoid") Long excludePoid
    );


    /**
     * Check if container group name exists per group (excluding deleted records)
     */
    @Query("""
       SELECT COUNT(c) > 0
       FROM ShipContainerTypeGrpMaster c
       WHERE c.containerGrpName = :name
         AND c.groupPoid = :groupPoid
         AND (c.deleted IS NULL OR c.deleted = 'N')
       """)
    boolean existsByContainerGrpNameAndGroupPoid(
            @Param("name") String name,
            @Param("groupPoid") Long groupPoid
    );

    /**
     * Check if container group name exists per group excluding a specific POID (for updates)
     */
    @Query("""
       SELECT COUNT(c) > 0
       FROM ShipContainerTypeGrpMaster c
       WHERE c.containerGrpName = :name
         AND c.groupPoid = :groupPoid
         AND c.containerGrpPoid != :excludePoid
         AND (c.deleted IS NULL OR c.deleted = 'N')
       """)
    boolean existsByContainerGrpNameAndGroupPoidExcludingPoid(
            @Param("name") String name,
            @Param("groupPoid") Long groupPoid,
            @Param("excludePoid") Long excludePoid
    );
}
