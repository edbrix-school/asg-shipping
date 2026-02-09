package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.entity.OFOQItemDtlEntity;
import com.asg.shipping.shippingofoqv2.entity.TransactionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OFOQItemDtlRepository extends JpaRepository<OFOQItemDtlEntity, TransactionDetailId> {
    
    @Query("SELECT e FROM OFOQItemDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    List<OFOQItemDtlEntity> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    @Query("SELECT COALESCE(MAX(e.detRowId), 0) FROM OFOQItemDtlEntity e WHERE e.transactionPoid = :transactionPoid")
    Long findMaxDetRowId(@Param("transactionPoid") Long transactionPoid);


}
