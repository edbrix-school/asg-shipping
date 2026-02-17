package com.asg.shipping.contractsandagreements.repository;

import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;

import com.asg.shipping.contractsandagreements.entity.key.AdminContractsAgreementDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminContractsAgreementPicDtlRepository
        extends JpaRepository<AdminContractsAgreementPicDtl, AdminContractsAgreementDtlId> {

    List<AdminContractsAgreementPicDtl> findByIdTransactionPoid(Long transactionPoid);

    Optional<AdminContractsAgreementPicDtl>
    findByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);

    void deleteByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);

    @Query("""
        select coalesce(max(p.id.detRowId), 0)
        from AdminContractsAgreementPicDtl p
        where p.id.transactionPoid = :transactionPoid
    """)
    Long findMaxDetRowId(@Param("transactionPoid") Long transactionPoid);
}


