package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestContainerDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestContainerDtlRepository extends JpaRepository<ShipBlManifestContainerDtl, ShipBlManifestDtlId> {

    //List<ShipBlManifestContainerDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    //Optional<ShipBlManifestContainerDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

   // Optional<ShipBlManifestContainerDtl> findByTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

   // void deleteByTransactionPoid(Long transactionPoid);

   /* @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestContainerDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);*/

    List<ShipBlManifestContainerDtl>
    findByIdTransactionPoidOrderByIdDetRowId(Long transactionPoid);

    Optional<ShipBlManifestContainerDtl>
    findByIdTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
        SELECT COALESCE(MAX(d.id.detRowId), 0)
        FROM ShipBlManifestContainerDtl d
        WHERE d.id.transactionPoid = :transactionPoid
    """)
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

    List<ShipBlManifestContainerDtl>
    findByIdTransactionPoid(Long transactionPoid);
}
