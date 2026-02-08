package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlGeneralDtl;
import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportManifestBlGeneralDtlRepository extends JpaRepository<ExportManifestBlGeneralDtl, ShipBlManifestDtlId> {

  
    List<ExportManifestBlGeneralDtl> findById_TransactionPoid(Long transactionPoid);

   
    void deleteById_TransactionPoid(Long transactionPoid);
}

