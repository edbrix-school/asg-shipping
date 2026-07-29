package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.dto.LoadOFOQDetailsRequest;
import com.asg.shipping.shippingofoqv2.dto.OFOQLoadItemDetailsResponse;
import com.asg.shipping.shippingofoqv2.dto.OFOQManifestXmlDto;

import java.util.List;

public interface ShippingOFOQProcRepository {

    OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request);

    List<OFOQManifestXmlDto> loadOFOQManifestXml(Long transactionPoid, String blNumber, String manifestType, String docRef);

    /**
     * Mirrors PROC_SAVE_OFOQ_API_RESPONSE in the legacy bean: the HTTP reason phrase goes to
     * P_RESPONSE_MSG and the {@code <Message>} extracted from the XML body goes to P_RESPONSE.
     */
    String saveOFOQApiResponse(Long transactionPoid, String docRef, String manifestType,
                               int responseCode, String httpStatusText, String extractedMessage);

    /**
     * Mirrors PROC_SAVE_OFOQ_API_MANIFEST_RESPONSE in the legacy bean: the HTTP reason phrase goes
     * to P_RESPONSE_MSG and the full response body goes to P_XML_RESPONSE.
     */
    String saveOFOQManifestResponse(Long transactionPoid, String docRef, String functionalRefId,
                                    String responseCode, String httpStatusText, String xmlResponse,
                                    String manifestType, String blNumber);

}
