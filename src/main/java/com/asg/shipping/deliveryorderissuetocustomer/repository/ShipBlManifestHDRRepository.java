package com.asg.shipping.deliveryorderissuetocustomer.repository;

import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ShipBlManifestHdr entity
 */
@Repository
public interface ShipBlManifestHDRRepository extends JpaRepository<ShipBlManifestHDR, Long> {

    /**
     * Find BL manifest by TRANSACTION_POID, GROUP_POID, and COMPANY_POID
     */
    Optional<ShipBlManifestHDR> findByTransactionPoidAndGroupPoidAndCompanyPoid(Long transactionPoid, Long groupPoid, Long companyPoid);
}

