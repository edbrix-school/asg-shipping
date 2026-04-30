package com.asg.shipping.contractsandagreements.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementsHdrRepository;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContractsAndAgreementsValidationServiceImpl implements ContractsAndAgreementsValidationService {

    private final AdminContractsAgreementsHdrRepository headerRepo;
    private final JdbcTemplate jdbcTemplate;


    @Override
    public boolean checkForDuplicateAgreementName(String agreementName) {
        return headerRepo.existsByAgreementName(agreementName);
    }

    @Override
    public boolean checkForDuplicateAgreementName(
            String agreementName,
            Long transactionPoid
    ) {
        return headerRepo.existsByAgreementNameAndTransactionPoidNot(
                agreementName,
                transactionPoid
        );
    }

    @Override
    public void expiryDateValidationForRenew(LocalDate expiryDate, LocalDate effectiveStartDate) {

        if (expiryDate == null || effectiveStartDate == null) {
            throw new ValidationException("Expiry date and Effective start date must not be null");
        }

        if (effectiveStartDate.isAfter(expiryDate)) {
            throw new ValidationException("Effective start date cannot be after expiry date");
        }
    }

    @Override
    public void expiryDateValidation(
            LocalDate expiryDate,
            LocalDate effectiveDate,
            LocalDate terminationDate) {


        if (terminationDate != null) {

            boolean beforeEffective = terminationDate.isBefore(effectiveDate);
            boolean afterExpiry = terminationDate.isAfter(expiryDate);

            if (beforeEffective || afterExpiry) {
                throw new ValidationException(
                        "Termination Date must be between Effective Date and Expiry Date"
                );
            }
        }
    }

    @Override
    public void partyValidation(String partyType, Long partyPoid) {

        if (partyType == null || partyPoid == null) {
            throw new ValidationException("Party Type or Party Poid cannot be null");
        }

        String sql = switch (partyType.toUpperCase()) {
            case "CUSTOMER" -> """
                    SELECT 1 
                    FROM SALES_CUSTOMER_MASTER
                    WHERE CUSTOMER_POID = ?
                    AND NVL(DELETED,'N') <> 'Y'
                    """;
            case "SUPPLIER" -> """
                    SELECT 1 
                    FROM AP_SUPPLIER_MASTER
                    WHERE SUPPLIER_POID = ?
                    AND NVL(DELETED,'N') <> 'Y'
                    """;
            case "PRINCIPAL" -> """
                    SELECT 1 
                    FROM SHIP_PRINCIPAL_MASTER
                    WHERE PRINCIPAL_POID = ?
                    AND NVL(DELETED,'N') <> 'Y'
                    """;
            default -> throw new ValidationException("Invalid Party Type");
        };

        boolean exists = exists(sql, partyPoid);

        if (!exists) {
            throw new ValidationException("Invalid Party Name for Party Type: " + partyType);
        }
    }

    private boolean exists(String sql, Long poid) {

        List<Integer> result = jdbcTemplate.query(
                sql,
                ps -> ps.setLong(1, poid),
                (rs, rowNum) -> rs.getInt(1)
        );

        return !result.isEmpty();
    }


}