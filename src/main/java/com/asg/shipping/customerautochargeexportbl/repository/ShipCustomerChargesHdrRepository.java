package com.asg.shipping.customerautochargeexportbl.repository;

import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;


@Repository
public interface ShipCustomerChargesHdrRepository extends JpaRepository<ShipCustomerChargesHdrEntity, Long> {

    Optional<ShipCustomerChargesHdrEntity> findByDocRefAndGroupPoidAndDeleted(String docRef, Long groupPoid, String deleted);

    Optional<ShipCustomerChargesHdrEntity> findByDocRefAndDeleted(String docRef, String n);

    @Query("SELECT COUNT(h) FROM ShipCustomerChargesHdrEntity h " +
           "WHERE h.customerPoid = :customerPoid " +
           "AND h.periodFrom <= :periodTo AND h.periodTo >= :periodFrom " +
           "AND (h.deleted IS NULL OR h.deleted = 'N') " +
           "AND (:excludeId IS NULL OR h.transactionPoid <> :excludeId)")
    long countOverlappingPeriod(@Param("customerPoid") Long customerPoid,
                                @Param("periodFrom") LocalDate periodFrom,
                                @Param("periodTo") LocalDate periodTo,
                                @Param("excludeId") Long excludeId);
}

