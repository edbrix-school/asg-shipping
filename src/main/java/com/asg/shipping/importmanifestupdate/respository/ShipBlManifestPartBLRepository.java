package com.asg.shipping.importmanifestupdate.respository;

import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestPartBL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipBlManifestPartBLRepository extends JpaRepository<ShipBlManifestPartBL, ShipBlManifestDtlId> {


    List<ShipBlManifestPartBL>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
        SELECT COALESCE(MAX(d.id.detRowId), 0)
        FROM ShipBlManifestPartBL d
        WHERE d.id.transactionPoid = :transactionPoid
    """)
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
