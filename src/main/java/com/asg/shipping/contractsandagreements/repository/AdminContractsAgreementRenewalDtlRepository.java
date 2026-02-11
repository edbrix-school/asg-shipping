package com.asg.shipping.contractsandagreements.repository;

import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;
import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminContractsAgreementRenewalDtlRepository
        extends JpaRepository<AdminContractsAgreementRenewalEntity, AdminContractsAgreementDtlId> {

    List<AdminContractsAgreementRenewalEntity>
    findByIdTransactionPoid(Long transactionPoid);

    Optional<AdminContractsAgreementRenewalEntity>
    findByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);

    void deleteByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);

    @Query("""
        select coalesce(max(p.id.detRowId), 0)
        from AdminContractsAgreementRenewalEntity p
        where p.id.transactionPoid = :transactionPoid
    """)
    Long findMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}

