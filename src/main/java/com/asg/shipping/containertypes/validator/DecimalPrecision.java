package com.asg.shipping.containertypes.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DecimalPrecisionValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DecimalPrecision {
    String message() default "Decimal value must not exceed {scale} digits after decimal point";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int scale() default 3;
}