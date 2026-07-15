package com.asg.shipping.linetariffs.util;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.linetariffs.dto.TariffDetailUpdateDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineTariffSlabValidatorTest {

    @Test
    void validate_InvalidSlabOrder_ThrowsValidationException() {
        TariffDetailUpdateDTO detail = TariffDetailUpdateDTO.builder()
                .detRowId(1L)
                .containerTypePoid(50L)
                .slab1Tilldays(2)
                .slab2Tilldays(3)
                .slab3Tilldays(2)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> LineTariffSlabValidator.validate(detail));

        assertTrue(ex.getMessage().contains("Slab days should be in incremental order"));
        assertTrue(ex.getMessage().contains("Slab 3 days (2) must be greater than Slab 2 days (3)"));
    }

    @Test
    void validate_DuplicateSlabDays_ThrowsValidationException() {
        TariffDetailUpdateDTO detail = TariffDetailUpdateDTO.builder()
                .detRowId(1L)
                .containerTypePoid(50L)
                .slab1Tilldays(2)
                .slab2Tilldays(2)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> LineTariffSlabValidator.validate(detail));

        assertTrue(ex.getMessage().contains("Slab 2 days (2) must be greater than Slab 1 days (2)"));
    }

    @Test
    void validate_SkippedSlab_IsAllowed() {
        TariffDetailUpdateDTO detail = TariffDetailUpdateDTO.builder()
                .detRowId(1L)
                .containerTypePoid(50L)
                .slab1Tilldays(2)
                .slab3Tilldays(4)
                .slab3Rate(BigDecimal.TEN)
                .build();

        assertDoesNotThrow(() -> LineTariffSlabValidator.validate(detail));
    }

    @Test
    void validate_ValidIncrementalOrder_Succeeds() {
        TariffDetailUpdateDTO detail = TariffDetailUpdateDTO.builder()
                .slab1Tilldays(2)
                .slab2Tilldays(3)
                .slab3Tilldays(4)
                .build();

        assertDoesNotThrow(() -> LineTariffSlabValidator.validate(detail));
    }
}
