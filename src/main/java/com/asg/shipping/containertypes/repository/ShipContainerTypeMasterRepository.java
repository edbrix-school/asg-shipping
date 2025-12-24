package com.asg.shipping.containertypes.repository;

import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShipContainerTypeMaster entity
 */
@Repository
public interface ShipContainerTypeMasterRepository extends JpaRepository<ShipContainerTypeMaster, Long> {

    /**
     * Find container type by ID and group POID
     */
    Optional<ShipContainerTypeMaster> findByContainerTypePoidAndGroupPoid(Long containerTypePoid, Long groupPoid);

    /**
     * Check if container type code exists for the given group (excluding deleted records)
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipContainerTypeMaster c " +
           "WHERE c.containerTypeCode = :code")
    boolean existsByContainerTypeCode(@Param("code") String code);

    /**
     * Check if container type code exists for the given group excluding a specific POID (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipContainerTypeMaster c " +
           "WHERE c.containerTypeCode = :code AND c.groupPoid = :groupPoid " +
           "AND c.containerTypePoid != :excludePoid AND c.deleted = 'N'")
    boolean existsByContainerTypeCodeAndGroupPoidExcludingPoid(
            @Param("code") String code,
            @Param("groupPoid") Long groupPoid,
            @Param("excludePoid") Long excludePoid
    );

    /**
     * Check if container type name exists for the given group (excluding deleted records)
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipContainerTypeMaster c " +
           "WHERE c.containerTypeName = :name")
    boolean existsByContainerTypeName(@Param("name") String name);

    /**
     * Check if container type name exists for the given group excluding a specific POID (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipContainerTypeMaster c " +
           "WHERE c.containerTypeName = :name " +
           "AND c.containerTypePoid != :excludePoid")
    boolean existsByContainerTypeNameExcludingPoid(
            @Param("name") String name,
            @Param("excludePoid") Long excludePoid
    );
}


