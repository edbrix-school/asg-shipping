package com.asg.shipping.shipcommisiontransfer.repository;

import com.asg.shipping.shipcommisiontransfer.entity.PdaFdaDtl;
import com.asg.shipping.shipcommisiontransfer.entity.PdaFdaDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdaFdaDtlRepository extends JpaRepository<PdaFdaDtl, PdaFdaDtlId> {

    List<PdaFdaDtl> findByIdTransactionPoid(Long transactionPoid);
}
