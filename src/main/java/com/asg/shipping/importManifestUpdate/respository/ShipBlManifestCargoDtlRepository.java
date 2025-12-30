package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestCargoDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestCargoDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestCargoDtlRepository  extends JpaRepository<ShipBlManifestCargoDtl, ShipBlManifestCargoDtlId> {

    List<ShipBlManifestCargoDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipBlManifestCargoDtl> findByTransactionPoidAndDetRowIdAndDescriptionType(
            Long transactionPoid, Long detRowId, String descriptionType);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestCargoDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
