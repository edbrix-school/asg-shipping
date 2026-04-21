package com.asg.shipping.linetariffs.repository;

import com.asg.shipping.linetariffs.entity.ShipLineTariffHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShipLineTariffHdr entity
 */
@Repository
public interface ShipLineTariffHdrRepository extends JpaRepository<ShipLineTariffHdr, Long> {

    /**
     * Find tariff by TRANSACTION_POID and GROUP_POID
     */
    Optional<ShipLineTariffHdr> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    /**
     * Check if DOC_REF already exists
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipLineTariffHdr t " +
           "WHERE t.docRef = :docRef AND t.deleted = 'N'")
    boolean existsByDocRef(@Param("docRef") String docRef);

    /**
     * Check if DOC_REF already exists excluding a specific TRANSACTION_POID (for updates)
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipLineTariffHdr t " +
           "WHERE t.docRef = :docRef AND t.transactionPoid != :excludeTransactionPoid AND t.deleted = 'N'")
    boolean existsByDocRefExcludingPoid(@Param("docRef") String docRef, @Param("excludeTransactionPoid") Long excludeTransactionPoid);

    /**
     * Check if period overlaps with existing tariffs for the same LINE_POID
     * Overlap condition: (periodFrom <= existing.periodTo AND periodTo >= existing.periodFrom)
     */
    @Query("SELECT COUNT(t) > 0 FROM ShipLineTariffHdr t " +
           "WHERE t.linePoid = :linePoid " +
           "AND t.groupPoid = :groupPoid " +
           "AND t.companyPoid = :companyPoid " +
           "AND t.deleted = 'N' " +
           "AND (:periodFrom <= t.periodTo AND :periodTo >= t.periodFrom) " +
           "AND (:excludeTransactionPoid IS NULL OR t.transactionPoid != :excludeTransactionPoid)")
    boolean existsOverlappingPeriod(
            @Param("linePoid") Long linePoid,
            @Param("groupPoid") Long groupPoid,
            @Param("companyPoid") Long companyPoid,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo,
            @Param("excludeTransactionPoid") Long excludeTransactionPoid
    );

    @Modifying
    @Query(value = "BEGIN COPY_LINE_TARIFF(:transactionPoid); END;", nativeQuery = true)
    void callCopyLineTariff(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT t FROM ShipLineTariffHdr t WHERE t.linePoid = :linePoid AND t.groupPoid = :groupPoid AND t.deleted = 'N' ORDER BY t.transactionPoid DESC")
    List<ShipLineTariffHdr> findLatestByLinePoidAndGroupPoid(@Param("linePoid") Long linePoid, @Param("groupPoid") Long groupPoid);
}

