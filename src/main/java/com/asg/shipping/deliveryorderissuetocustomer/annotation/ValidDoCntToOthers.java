package com.asg.shipping.deliveryorderissuetocustomer.annotation;

import com.asg.shipping.deliveryorderissuetocustomer.validator.DoCntToOthersValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DoCntToOthersValidator.class)
@Documented
public @interface ValidDoCntToOthers {
    String message() default "DO count to others mails is required when doCntToOthers is 'y'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
