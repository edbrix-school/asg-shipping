package com.asg.shipping.shippingofoqv2.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.shippingofoqv2.dto.ShippingOFOQV2Request;
import com.asg.shipping.shippingofoqv2.service.OFOQValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OFOQValidationServiceImpl implements OFOQValidationService {

    public void validateDocument(ShippingOFOQV2Request request) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(request.getVoyageNo())) {
            errors.add("Voyage number is mandatory");
        }

        if (request.getVesselPoid() == null) {
            errors.add("Vessel details is mandatory");
        }

        if (request.getArrivalDate() == null) {
            errors.add("Arrival date is mandatory");
        }

        if (request.getRotationNumber() == null) {
            errors.add("Rotation number is mandatory");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(", ", errors));
        }
    }
}

