package com.asg.shipping.vvc.edi.repository.jpa;

import com.asg.shipping.vvc.edi.entity.VwShipEdiExceptionUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwShipEdiExceptionUploadRepository extends JpaRepository<VwShipEdiExceptionUploadEntity, Long> {
    List<VwShipEdiExceptionUploadEntity> findByTransactionPoidOrderByPkIdRowAsc(Long transactionPoid);
}


