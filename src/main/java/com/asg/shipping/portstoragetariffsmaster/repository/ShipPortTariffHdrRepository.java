package com.asg.shipping.portstoragetariffsmaster.repository;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for ShipPortTariffHdr entity
 */
@Repository
public interface ShipPortTariffHdrRepository extends JpaRepository<ShipPortTariffHdr, Long> {

    /**
     * Find tariff by POID and group POID
     */
    Optional<ShipPortTariffHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    /**
     * Check if document reference exists (excluding deleted records)
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipPortTariffHdr t " +
            "WHERE t.docRef = :docRef AND t.deleted = 'N'")
    boolean existsByDocRef(@Param("docRef") String docRef);

    /**
     * Check if document reference exists excluding a specific POID (for updates)
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipPortTariffHdr t " +
            "WHERE t.docRef = :docRef AND t.transactionPoid != :excludePoid AND t.deleted = 'N'")
    boolean existsByDocRefExcludingPoid(@Param("docRef") String docRef, @Param("excludePoid") Long excludePoid);

    /**
     * Check for date overlap: Period ranges that overlap with the given period for same port and tariff type
     * Excludes deleted records and optionally excludes a specific transaction POID
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipPortTariffHdr t " +
            "WHERE t.portPoid = :portPoid " +
            "AND t.tariffType = :tariffType " +
            "AND t.groupPoid = :groupPoid " +
            "AND t.deleted = 'N' " +
            "AND (:excludePoid IS NULL OR t.transactionPoid != :excludePoid) " +
            "AND NOT (t.periodTo < :periodFrom OR t.periodFrom > :periodTo)")
    boolean existsOverlappingPeriod(
            @Param("portPoid") Long portPoid,
            @Param("tariffType") String tariffType,
            @Param("groupPoid") Long groupPoid,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo,
            @Param("excludePoid") Long excludePoid
    );
}
