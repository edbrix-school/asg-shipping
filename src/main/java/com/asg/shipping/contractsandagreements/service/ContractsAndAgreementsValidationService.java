package com.asg.shipping.contractsandagreements.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ContractsAndAgreementsValidationService {
    boolean checkForDuplicateAgreementName(String agreementName);
    void expiryDateValidation(LocalDate expiryDate, LocalDate effectiveDate, LocalDate terminationDate);
    void partyValidation(String partyType, Long partyPoid);
     boolean checkForDuplicateAgreementName(
            String agreementName,
            Long transactionPoid
    );
}
