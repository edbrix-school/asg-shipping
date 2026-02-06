package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlChargesDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportManifestBlChargesDtlRepository extends JpaRepository<ExportManifestBlChargesDtl, ShipBlManifestDtlId> {

 
    List<ExportManifestBlChargesDtl> findById_TransactionPoid(Long transactionPoid);

   
    void deleteById_TransactionPoid(Long transactionPoid);
}

