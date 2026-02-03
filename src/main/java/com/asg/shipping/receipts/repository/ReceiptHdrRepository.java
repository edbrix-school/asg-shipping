package com.asg.shipping.receipts.repository;

import com.asg.shipping.receipts.entity.ArShReceiptHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptHdrRepository extends JpaRepository<ArShReceiptHdr, Long> {
    Optional<ArShReceiptHdr> findByTransactionPoid(Long transactionPoid);
    Optional<ArShReceiptHdr> findByBlPoid(Long blPoid);
}
