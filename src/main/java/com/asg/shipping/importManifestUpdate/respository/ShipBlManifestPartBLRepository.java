package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestPartBL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestPartBLRepository extends JpaRepository<ShipBlManifestPartBL, ShipBlManifestDtlId> {

    List<ShipBlManifestPartBL> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipBlManifestPartBL> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestContainerPrt d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
