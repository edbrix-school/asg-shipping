package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.entity.ExportManifestBlContainerDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportManifestBlContainerDtlRepository extends JpaRepository<ExportManifestBlContainerDtl, ShipBlManifestDtlId> {

    List<ExportManifestBlContainerDtl> findById_TransactionPoid(Long transactionPoid);

  
    void deleteById_TransactionPoid(Long transactionPoid);

    Optional<ExportManifestBlContainerDtl> findById_TransactionPoidAndContainerNo(Long transactionPoid, String containerNo);
}

