package com.asg.shipping.receipts.repository;

import com.asg.shipping.receipts.entity.ArShReceiptContainerDtl;
import com.asg.shipping.receipts.entity.TransactionDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArShReceiptContainerDtlRepository extends JpaRepository<ArShReceiptContainerDtl, TransactionDtlId> {
	List<ArShReceiptContainerDtl> findByIdTransactionPoid(Long transactionPoid);
	List<ArShReceiptContainerDtl> findByBlPoid(Long blPoid);
	List<ArShReceiptContainerDtl> findByContainerNo(String containerNo);
	void deleteByIdTransactionPoid(Long transactionPoid);

	@Query("SELECT COALESCE(MAX(a.id.detRowId), 0) FROM ArShReceiptContainerDtl a WHERE a.id.transactionPoid = :transactionPoid")
	Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}
