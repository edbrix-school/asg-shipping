package com.asg.shipping.containertypeportchargestariff.repository;

import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShipPortChargesHdrRepository extends JpaRepository<ShipPortChargesHdr, Long> {
    @Query("""
                SELECT h FROM ShipPortChargesHdr h
                WHERE h.groupPoid = :groupPoid
                  AND h.portPoid = :portPoid
                  AND h.chargeLinePoid = :chargeLinePoid
                  AND h.chargeDivision = :chargeDivision
                  AND h.deleted <> 'Y'
                  AND (:excludeId IS NULL OR h.transactionPoid <> :excludeId)
                  AND :fromDate <= h.periodTo
                  AND :toDate >= h.periodFrom
            """)
    List<ShipPortChargesHdr> findOverlappingTariffs(
            @Param("groupPoid") Long groupPoid,
            @Param("portPoid") Long portPoid,
            @Param("chargeLinePoid") Long chargeLinePoid,
            @Param("chargeDivision") String chargeDivision,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludeId") Long excludeId
    );
}
