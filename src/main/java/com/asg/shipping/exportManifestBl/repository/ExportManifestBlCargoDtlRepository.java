package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlCargoDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestCargoDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportManifestBlCargoDtlRepository extends JpaRepository<ExportManifestBlCargoDtl, ShipBlManifestCargoDtlId> {

   
    List<ExportManifestBlCargoDtl> findById_TransactionPoid(Long transactionPoid);

    void deleteById_TransactionPoid(Long transactionPoid);
}

