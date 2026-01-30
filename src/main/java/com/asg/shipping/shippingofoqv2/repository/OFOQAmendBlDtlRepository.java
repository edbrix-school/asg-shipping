package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.entity.OFOQAmendBlDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OFOQAmendBlDtlRepository extends JpaRepository<OFOQAmendBlDtlEntity, TransactionDetailId> {
    
    @Query("SELECT e FROM OFOQAmendBlDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<OFOQAmendBlDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    OFOQAmendBlDtlEntity findByTransactionPoidAndBlNumber(Long transactionPoid, String blNumber);
}
