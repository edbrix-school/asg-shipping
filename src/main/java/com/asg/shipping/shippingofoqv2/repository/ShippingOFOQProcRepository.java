package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.dto.LoadOFOQDetailsRequest;
import com.asg.shipping.shippingofoqv2.dto.OFOQLoadItemDetailsResponse;
import com.asg.shipping.shippingofoqv2.dto.OFOQManifestXmlDto;

import java.util.List;

public interface ShippingOFOQProcRepository {

    OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request);

    List<OFOQManifestXmlDto> loadOFOQManifestXml( Long transactionPoid,String blNumber,String manifestType,String docRef,Long vesselPoid);

    String saveOFOQApiResponse(Long transactionPoid, String docRef, String manifestType,
                               int responseCode, String responseMessage, String xmlResponse);

    String saveOFOQManifestResponse(Long transactionPoid, String docRef, String functionalRefId,
                                    String responseCode, String responseMessage, String xmlResponse,
                                    String manifestType, String blNumber);

    String getParameterValue(String parameterName);
}
