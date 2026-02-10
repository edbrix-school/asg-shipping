package com.asg.shipping.shippingofoqv2.service.impl;

import com.asg.shipping.shippingofoqv2.dto.OFOQCheckStatusCustomsResponseDto;
import com.asg.shipping.shippingofoqv2.dto.OFOQManifestSubmitResponseDto;
import com.asg.shipping.shippingofoqv2.repository.ShippingOFOQProcRepository;
import com.asg.shipping.shippingofoqv2.service.OFOQApiService;
import com.asg.common.lib.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OFOQApiServiceImpl implements OFOQApiService {

    private final ShippingOFOQProcRepository procRepository;
    private final RestTemplate restTemplates;

    private String getParameterValue(String parameterName) {
        return procRepository.getParameterValue(parameterName);
    }

    private void validateOFOQConfiguration() {
        String apiUrl = getParameterValue("OFOQ_API_LINK");
        String credentials = getParameterValue("OFOQ_API_USER_AUTH");
        
        if (apiUrl == null || credentials == null) {
            log.error("Failed to call OFOQ API: Missing configuration parameters");
            throw new ValidationException("Failed to call OFOQ API: Missing OFOQ_API_LINK or OFOQ_API_USER_AUTH configuration");
        }
    }

    private HttpHeaders createHeaders() {
        String credentials = getParameterValue("OFOQ_API_USER_AUTH");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        String auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + auth);
        return headers;
    }

    private String getApiUrl() {
        return getParameterValue("OFOQ_API_LINK");
    }

    @Override
    public OFOQManifestSubmitResponseDto callOFOQApi(
            String xmlData,
            String manifestType,
            String blNumber,
            Long transactionPoId,
            String docRef) {

        log.info("Submitting OFOQ manifest for transactionPoid: {}, docRef: {}", transactionPoId, docRef);

        validateOFOQConfiguration();

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(xmlData, headers);

            ResponseEntity<String> response = restTemplates.exchange(
                    getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("OFOQ API response status: {}", response.getStatusCode());

            String message = extractMessage(response.getBody());
            String functionalRefId = null;

            try {
                functionalRefId = procRepository.saveOFOQApiResponse(
                        transactionPoId,
                        docRef,
                        manifestType,
                        response.getStatusCode().value(),
                        message,
                        response.getBody()
                );
            } catch (Exception e) {
                log.warn("Failed to save OFOQ API response to DB", e);
            }

            return OFOQManifestSubmitResponseDto.builder()
                    .functionalRefId(functionalRefId)
                    .status(response.getStatusCode().toString())
                    .message(message)
                    .build();

        }
        catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error(
                    "Customs API failed | status={} | response={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw ex;
        }
        catch (Exception e) {
            log.error("Unexpected error calling customs API", e);
            throw new RuntimeException("Error while calling customs API", e);
        }
    }


    @Override
    public OFOQCheckStatusCustomsResponseDto getManifestStatus(String functionalRefId) {
        log.debug("Fetching OFOQ manifest status for functionalRefId: {}", functionalRefId);
        try {
            validateOFOQConfiguration();

            Thread.sleep(3000);

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String url = getApiUrl() + functionalRefId + "/";
            ResponseEntity<String> response = restTemplates.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            log.debug("OFOQ manifest status response code: {}", response.getStatusCode());
            String message = extractMessage(response.getBody());
            log.info("OFOQ manifest status retrieved for functionalRefId: {} with statusCode: {}", functionalRefId, response.getStatusCode());

            return OFOQCheckStatusCustomsResponseDto.builder()
                    .functionalReference(functionalRefId)
                    .statusCode(String.valueOf(response.getStatusCode()))
                    .responseMessage(message)
                    .responseBody(response.getBody())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching manifest status for functionalRefId: {}", functionalRefId, e);
            throw new RuntimeException("Interrupted while fetching manifest status", e);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("OFOQ manifest status fetch failed with status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return OFOQCheckStatusCustomsResponseDto.builder()
                    .functionalReference(functionalRefId)
                    .statusCode(String.valueOf(ex.getStatusCode()))
                    .responseMessage(ex.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error fetching OFOQ manifest status for functionalRefId: {}", functionalRefId, e);
            throw new RuntimeException("Failed to fetch manifest status: " + e.getMessage(), e);
        }
    }

    private String extractMessage(String xmlData) {
        if (xmlData == null) return null;
        int start = xmlData.indexOf("<Message>");
        int end = xmlData.indexOf("</Message>");
        if (start != -1 && end != -1) return xmlData.substring(start + 9, end);
        return null;
    }

}
