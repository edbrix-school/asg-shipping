package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestGeneralDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestGeneralDtlRepository  extends JpaRepository<ShipBlManifestGeneralDtl, ShipBlManifestDtlId> {

    //List<ShipBlManifestGeneralDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    //Optional<ShipBlManifestGeneralDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    //void deleteByTransactionPoid(Long transactionPoid);

    List<ShipBlManifestGeneralDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

/*    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestGeneralDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);*/

    @Query("""
    SELECT COALESCE(MAX(d.id.detRowId), 0)
    FROM ShipBlManifestGeneralDtl d
    WHERE d.id.transactionPoid = :transactionPoid
""")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
