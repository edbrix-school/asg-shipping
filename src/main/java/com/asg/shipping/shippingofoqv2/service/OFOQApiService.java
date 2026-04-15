package com.asg.shipping.shippingofoqv2.service;

import com.asg.shipping.shippingofoqv2.dto.OFOQCheckStatusCustomsResponseDto;

public interface OFOQApiService {

    String callOFOQApi(String xmlData, String manifestType, String blNumber, Long transactionPoId, String docRef);

    OFOQCheckStatusCustomsResponseDto getManifestStatus(String functionalRefId);

}
