package com.asg.shipping.receipts.repository;

import com.asg.shipping.receipts.entity.ArShReceiptChargesDtl;
import com.asg.shipping.receipts.entity.TransactionDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArShReceiptChargesDtlRepository extends JpaRepository<ArShReceiptChargesDtl, TransactionDtlId> {
	List<ArShReceiptChargesDtl> findByIdTransactionPoid(Long transactionPoid);
	List<ArShReceiptChargesDtl> findByBlPoid(Long blPoid);
	void deleteByIdTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(a.id.detRowId), 0) FROM ArShReceiptChargesDtl a WHERE a.id.transactionPoid = :transactionPoid")
	Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
