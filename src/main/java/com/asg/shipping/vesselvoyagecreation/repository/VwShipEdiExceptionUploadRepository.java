package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.entity.VwShipEdiExceptionUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwShipEdiExceptionUploadRepository extends JpaRepository<VwShipEdiExceptionUploadEntity, Long> {
    List<VwShipEdiExceptionUploadEntity> findByTransactionPoidOrderByPkIdRowAsc(Long transactionPoid);
}










