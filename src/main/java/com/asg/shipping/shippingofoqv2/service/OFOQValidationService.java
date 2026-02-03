package com.asg.shipping.shippingofoqv2.service;

import com.asg.shipping.shippingofoqv2.dto.ShippingOFOQV2Request;
public interface OFOQValidationService {

    void validateDocument(ShippingOFOQV2Request request);
}
