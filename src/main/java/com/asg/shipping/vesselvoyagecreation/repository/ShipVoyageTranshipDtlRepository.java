package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShipVoyageTranshipDtlRepository extends JpaRepository<ShipVoyageTranshipDtlEntity, ShipVoyageTranshipDtlId> {
    List<ShipVoyageTranshipDtlEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(e.detRowId), 0) FROM ShipVoyageTranshipDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    Long findMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}










