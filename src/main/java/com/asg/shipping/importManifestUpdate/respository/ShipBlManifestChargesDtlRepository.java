package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestChargesDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestChargesDtlRepository  extends JpaRepository<ShipBlManifestChargesDtl, ShipBlManifestDtlId> {

    //List<ShipBlManifestChargesDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    //Optional<ShipBlManifestChargesDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    //void deleteByTransactionPoid(Long transactionPoid);

   /* @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestChargesDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);*/


    List<ShipBlManifestChargesDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
        SELECT COALESCE(MAX(d.id.detRowId), 0)
        FROM ShipBlManifestChargesDtl d
        WHERE d.id.transactionPoid = :transactionPoid
    """)
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
