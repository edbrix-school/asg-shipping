package com.asg.shipping.contractsandagreements.repository;

import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminContractsAgreementsHdrRepository extends JpaRepository<AdminContractsAgreementHdr,Long> {

    boolean existsByAgreementName(String agreementName);

    boolean existsByAgreementNameAndTransactionPoidNot(
            String agreementName,
            Long transactionPoid
    );

}
