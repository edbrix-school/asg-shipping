package com.asg.shipping.linecommission.repository;

import com.asg.shipping.linecommission.entity.ShipLineCommHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ShipLineCommHdrRepository extends JpaRepository<ShipLineCommHdrEntity, Long> {

    Optional<ShipLineCommHdrEntity> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    /**
     * SRS: periods must not overlap for the same Line
     */

    @Query("""
            select count(h) from ShipLineCommHdrEntity h
            where h.groupPoid = :groupPoid
              and (:companyPoid is null or h.companyPoid = :companyPoid)
              and h.linePoid = :linePoid
              and (h.deleted is null or upper(h.deleted) = 'N')
              and :fromDate <= h.periodTo
              and :toDate >= h.periodFrom
              and (:excludeId is null or h.transactionPoid <> :excludeId)
            """)
    long countOverlapping(@Param("excludeId") Long excludeId,
                          @Param("linePoid") Long linePoid,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate,
                          @Param("groupPoid") Long groupPoid,
                          @Param("companyPoid") Long companyPoid);
}


