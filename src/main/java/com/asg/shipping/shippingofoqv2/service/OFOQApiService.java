package com.asg.shipping.shippingofoqv2.service;

import com.asg.shipping.shippingofoqv2.dto.OFOQCheckStatusCustomsResponseDto;
import com.asg.shipping.shippingofoqv2.dto.OFOQManifestSubmitResponseDto;

public interface OFOQApiService {

    OFOQManifestSubmitResponseDto callOFOQApi(String xmlData, String manifestType, String blNumber, Long transactionPoId, String docRef);

    OFOQCheckStatusCustomsResponseDto getManifestStatus(String functionalRefId);

}
