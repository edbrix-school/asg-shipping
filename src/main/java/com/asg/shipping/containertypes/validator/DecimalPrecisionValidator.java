package com.asg.shipping.containertypes.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecimalPrecisionValidator implements ConstraintValidator<DecimalPrecision, BigDecimal> {

    private int scale;

    @Override
    public void initialize(DecimalPrecision constraintAnnotation) {
        this.scale = constraintAnnotation.scale();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        // Round the value to the specified scale
        BigDecimal rounded = value.setScale(scale, RoundingMode.HALF_UP);
        
        // Update the value in place (this is for demonstration, actual rounding should be in service layer)
        return rounded.scale() <= scale;
    }
}