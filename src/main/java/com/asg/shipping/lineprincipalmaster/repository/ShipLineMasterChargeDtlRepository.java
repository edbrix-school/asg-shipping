package com.asg.shipping.lineprincipalmaster.repository;

import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ShipLineMasterChargeDtl entity
 */
@Repository
public interface ShipLineMasterChargeDtlRepository extends JpaRepository<ShipLineMasterChargeDtl, ShipLineMasterChargeDtlId> {

    /**
     * Find all charges for a line, ordered by detRowId
     */
    List<ShipLineMasterChargeDtl> findByLinePoidOrderByDetRowId(Long linePoid);

    /**
     * Find charge by line POID and charge POID
     */
    Optional<ShipLineMasterChargeDtl> findByLinePoidAndChargePoid(Long linePoid, Long chargePoid);

    /**
     * Check if charge POID exists for the given line
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipLineMasterChargeDtl c WHERE c.linePoid = :linePoid AND c.chargePoid = :chargePoid")
    boolean existsByLinePoidAndChargePoid(@Param("linePoid") Long linePoid, @Param("chargePoid") Long chargePoid);

    /**
     * Check if charge POID exists for the given line excluding a specific detail row (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM ShipLineMasterChargeDtl c WHERE c.linePoid = :linePoid AND c.chargePoid = :chargePoid AND c.detRowId != :excludeDetRowId")
    boolean existsByLinePoidAndChargePoidExcluding(@Param("linePoid") Long linePoid, @Param("chargePoid") Long chargePoid, @Param("excludeDetRowId") Long excludeDetRowId);

    /**
     * Delete all charges for a line
     */
    void deleteByLinePoid(Long linePoid);
}

