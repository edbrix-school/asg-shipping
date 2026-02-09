package com.asg.shipping.deliveryorderissuetocustomer.validator;

import com.asg.shipping.deliveryorderissuetocustomer.annotation.ValidDoCntToOthers;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class DoCntToOthersValidator implements ConstraintValidator<ValidDoCntToOthers, UpdateDeliveryOrderRequestDto> {

    @Override
    public boolean isValid(UpdateDeliveryOrderRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) return true;

        if ("y".equalsIgnoreCase(dto.getDoCntToOthers())) {
            return StringUtils.isNotBlank(dto.getDoCntToOthersMails());
        }
        return true;
    }
}
