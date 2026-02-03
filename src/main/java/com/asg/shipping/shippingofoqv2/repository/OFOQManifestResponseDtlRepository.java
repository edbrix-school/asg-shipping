package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.entity.OFOQManifestResponseDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OFOQManifestResponseDtlRepository extends JpaRepository<OFOQManifestResponseDtlEntity, TransactionDetailId> {
    
    @Query("SELECT e FROM OFOQManifestResponseDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<OFOQManifestResponseDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
