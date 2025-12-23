package com.asg.shipping.containertypeportchargestariff.validation;

import com.asg.shipping.containertypeportchargestariff.dto.PortChargesDetailCreateDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class AtLeastOnePositiveAmountValidator implements ConstraintValidator<AtLeastOnePositiveAmount, PortChargesDetailCreateDto> {

    @Override
    public boolean isValid(PortChargesDetailCreateDto detail, ConstraintValidatorContext context) {
        if (detail == null) {
            return true;
        }

        return isPositive(detail.getAmount20()) ||
               isPositive(detail.getAmount40()) ||
               isPositive(detail.getAmount53()) ||
               isPositive(detail.getAmountOther());
    }

    private boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}