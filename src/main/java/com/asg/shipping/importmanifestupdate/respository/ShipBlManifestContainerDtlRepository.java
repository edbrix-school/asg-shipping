package com.asg.shipping.importmanifestupdate.respository;

import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestContainerDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestContainerDtlRepository extends JpaRepository<ShipBlManifestContainerDtl, ShipBlManifestDtlId> {

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

    boolean existsByIdTransactionPoidAndContainerNo(Long transactionPoid, String containerNo);

    @Query("""
            select d.totalAmountCollected
            from ShipBlManifestContainerDtl d
            where d.id.transactionPoid = :transactionPoid
              and d.containerNo = :containerNo
            """)
    Optional<BigDecimal> findTotalAmountCollected(@Param("transactionPoid") Long transactionPoid, @Param("containerNo") String containerNo);
}
