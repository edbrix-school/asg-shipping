package com.asg.shipping.importManifestUpdate.respository;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestEmailFaxDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestEmailFaxId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlManifestEmailFaxDtlRepository  extends JpaRepository<ShipBlManifestEmailFaxDtl, ShipBlManifestEmailFaxId> {
    List<ShipBlManifestEmailFaxDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipBlManifestEmailFaxDtl> findByTransactionPoidAndDetRowIdAndAddressType(Long transactionPoid, Long detRowId, String addressType);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipBlManifestEmailFaxDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
