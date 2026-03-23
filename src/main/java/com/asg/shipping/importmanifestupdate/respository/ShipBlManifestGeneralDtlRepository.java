package com.asg.shipping.importmanifestupdate.respository;

import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestGeneralDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipBlManifestGeneralDtlRepository  extends JpaRepository<ShipBlManifestGeneralDtl, ShipBlManifestDtlId> {

    List<ShipBlManifestGeneralDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
    SELECT COALESCE(MAX(d.id.detRowId), 0)
    FROM ShipBlManifestGeneralDtl d
    WHERE d.id.transactionPoid = :transactionPoid
""")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
