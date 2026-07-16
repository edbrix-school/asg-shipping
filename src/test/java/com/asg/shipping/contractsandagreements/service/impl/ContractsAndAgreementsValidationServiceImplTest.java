package com.asg.shipping.contractsandagreements.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementsHdrRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractsAndAgreementsValidationServiceImplTest {

    @Mock
    private AdminContractsAgreementsHdrRepository headerRepo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ContractsAndAgreementsValidationServiceImpl validationService;

    @Test
    void testCheckForDuplicateAgreementName() {
        when(headerRepo.existsByAgreementName("test")).thenReturn(true);
        assertTrue(validationService.checkForDuplicateAgreementName("test"));
        
        when(headerRepo.existsByAgreementName("test2")).thenReturn(false);
        assertFalse(validationService.checkForDuplicateAgreementName("test2"));
    }

    @Test
    void testCheckForDuplicateAgreementNameWithId() {
        when(headerRepo.existsByAgreementNameAndTransactionPoidNot("test", 1L)).thenReturn(true);
        assertTrue(validationService.checkForDuplicateAgreementName("test", 1L));
        when(headerRepo.existsByAgreementNameAndTransactionPoidNot("test2", 1L)).thenReturn(false);
        assertFalse(validationService.checkForDuplicateAgreementName("test2", 1L));
    }

    @Test
    void testExpiryDateValidation_Success() {
        LocalDate effective = LocalDate.of(2023, 1, 1);
        LocalDate expiry = LocalDate.of(2024, 1, 1);
        LocalDate term = LocalDate.of(2023, 6, 1);

        assertDoesNotThrow(() -> validationService.expiryDateValidation(expiry, effective, term));
        // test null term
        assertDoesNotThrow(() -> validationService.expiryDateValidation(expiry, effective, null));
    }

    @Test
    void testExpiryDateValidation_BeforeEffective() {
        LocalDate effective = LocalDate.of(2023, 1, 1);
        LocalDate expiry = LocalDate.of(2024, 1, 1);
        LocalDate term = LocalDate.of(2022, 12, 31);

        ValidationException ex = assertThrows(ValidationException.class, 
                () -> validationService.expiryDateValidation(expiry, effective, term));
        assertEquals("Termination Date must be between Effective Date and Expiry Date", ex.getMessage());
    }

    @Test
    void testExpiryDateValidation_AfterExpiry() {
        LocalDate effective = LocalDate.of(2023, 1, 1);
        LocalDate expiry = LocalDate.of(2024, 1, 1);
        LocalDate term = LocalDate.of(2024, 1, 2);

        ValidationException ex = assertThrows(ValidationException.class, 
                () -> validationService.expiryDateValidation(expiry, effective, term));
        assertEquals("Termination Date must be between Effective Date and Expiry Date", ex.getMessage());
    }

    @Test
    void testPartyValidation_NullValues() {
        assertThrows(ValidationException.class, () -> validationService.partyValidation(null, 1L));
        assertThrows(ValidationException.class, () -> validationService.partyValidation("CUSTOMER", null));
    }

    @Test
    void testPartyValidation_InvalidType() {
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> validationService.partyValidation("INVALID", 1L));
        assertEquals("Invalid Party Type", ex.getMessage());
    }

    @Test
    void testPartyValidation_CustomerExists() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(1));
        assertDoesNotThrow(() -> validationService.partyValidation("CUSTOMER", 1L));
    }

    @Test
    void testPartyValidation_SupplierNotExists() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> validationService.partyValidation("SUPPLIER", 1L));
        assertTrue(ex.getMessage().contains("Invalid Party Name for Party Type"));
    }
    
    @Test
    void testPartyValidation_PrincipalExists() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(1));
        assertDoesNotThrow(() -> validationService.partyValidation("PRINCIPAL", 1L));
    }

    @Test
    void testPartyValidation_SupplierExists() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(1));
        assertDoesNotThrow(() -> validationService.partyValidation("supplier", 1L));
    }

    @Test
    void testValidatePicPeriodDates_Success() {
        LocalDate periodFrom = LocalDate.of(2026, 1, 1);
        LocalDate periodTo = LocalDate.of(2026, 12, 31);

        assertDoesNotThrow(() -> validationService.validatePicPeriodDates(periodFrom, periodTo));
        assertDoesNotThrow(() -> validationService.validatePicPeriodDates(periodFrom, null));
        assertDoesNotThrow(() -> validationService.validatePicPeriodDates(periodFrom, periodFrom));
    }

    @Test
    void testValidatePicPeriodDates_PeriodFromAfterPeriodTo() {
        LocalDate periodFrom = LocalDate.of(2026, 12, 31);
        LocalDate periodTo = LocalDate.of(2026, 1, 1);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validationService.validatePicPeriodDates(periodFrom, periodTo));
        assertEquals("Period From cannot be after Period To", ex.getMessage());
    }
}
