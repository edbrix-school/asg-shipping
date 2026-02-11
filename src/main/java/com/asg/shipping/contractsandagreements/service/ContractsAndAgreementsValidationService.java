package com.asg.shipping.contractsandagreements.service;

import java.time.LocalDateTime;

public interface ContractsAndAgreementsValidationService {
    boolean checkForDuplicateAgreementName(String agreementName);
    void expiryDateValidation(LocalDateTime expiryDate,LocalDateTime effectiveDate,LocalDateTime terminationDate);
    void partyValidation(String partyType, Long partyPoid);
     boolean checkForDuplicateAgreementName(
            String agreementName,
            Long transactionPoid
    );
}
