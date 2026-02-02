package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.entity.OFOQManifestAmendmentResponseDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OFOQManifestAmendmentResponseDtlRepository extends JpaRepository<OFOQManifestAmendmentResponseDtlEntity, TransactionDetailId> {
    
    @Query("SELECT e FROM OFOQManifestAmendmentResponseDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<OFOQManifestAmendmentResponseDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    List<OFOQManifestAmendmentResponseDtlEntity> findByTransactionPoidAndXmlBlNumber(Long transactionPoid, String xmlBlNumber);
}
