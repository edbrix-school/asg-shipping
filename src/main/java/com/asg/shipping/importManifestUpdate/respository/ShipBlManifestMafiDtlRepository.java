package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestMafiDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestMafiDtlRepository extends JpaRepository<ShipBlManifestMafiDtl, ShipBlManifestDtlId> {
    List<ShipBlManifestMafiDtl> findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    //Optional<ShipBlManifestMafiDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.id.detRowId), 0) FROM ShipBlManifestMafiDtl d WHERE d.id.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
