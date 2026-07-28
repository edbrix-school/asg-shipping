package com.asg.shipping.shippingofoqv2.service.impl;

import com.asg.common.lib.service.GlobalParameterService;
import com.asg.shipping.shippingofoqv2.dto.OFOQCheckStatusCustomsResponseDto;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OFOQApiServiceImpl implements OFOQApiService {

    /**
     * The provisional/functional reference is generated asynchronously on the OFOQ side; the legacy
     * bean waits before asking for the status of a freshly submitted manifest.
     */
    private static final long STATUS_POLL_DELAY_MS = 3000L;

    private final ShippingOFOQProcRepository procRepository;
    private final RestTemplate restTemplates;
    private final GlobalParameterService globalParameterService;


    private String requireApiUrl() {
        String apiUrl = getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            log.error("Failed to call OFOQ API: OFOQ_API_LINK is not configured");
            throw new ValidationException("Failed to call OFOQ API: Missing OFOQ_API_LINK configuration");
        }
        return apiUrl;
    }

    private HttpHeaders createHeaders() {
        String credentials = getCredentials();
        if (credentials == null || credentials.isBlank()) {
            log.error("Failed to call OFOQ API: OFOQ_API_USER_AUTH is not configured");
            throw new ValidationException("Failed to call OFOQ API: Missing OFOQ_API_USER_AUTH configuration");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        String auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + auth);
        return headers;
    }

    private String getCredentials() {
        return globalParameterService.getParameterValue("OFOQ_API_USER_AUTH", "GROUP", "1", "String");
    }

    private String getApiUrl() {
        return globalParameterService.getParameterValue("OFOQ_API_LINK", "GROUP", "1", "String");
    }

    @Override
    public String callOFOQApi(
            String xmlData,
            String manifestType,
            String blNumber,
            Long transactionPoId,
            String docRef) {

        log.info("Submitting OFOQ manifest for transactionPoid: {}, docRef: {}", transactionPoId, docRef);

        String apiUrl = requireApiUrl();
        HttpHeaders headers = createHeaders();

        int statusCode;
        String responseBody;

        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(xmlData, headers);

            ResponseEntity<String> response = restTemplates.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("OFOQ API response status: {}", response.getStatusCode());
            statusCode = response.getStatusCode().value();
            responseBody = response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Legacy reads the error stream and stores the failed response just like a successful one.
            log.error(
                    "OFOQ API failed | status={} | response={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            statusCode = ex.getStatusCode().value();
            responseBody = ex.getResponseBodyAsString();

        } catch (Exception e) {
            log.error("Unexpected error calling OFOQ API", e);
            throw new ValidationException("Error while calling OFOQ API: " + e.getMessage());
        }

        String extractedMessage = extractMessage(responseBody);

        return procRepository.saveOFOQApiResponse(
                transactionPoId,
                docRef,
                manifestType,
                statusCode,
                reasonPhrase(statusCode),
                // Legacy never passes NULL here - an unparsable body yields an empty message.
                extractedMessage != null ? extractedMessage : ""
        );
    }

    @Override
    public OFOQCheckStatusCustomsResponseDto getManifestStatus(String functionalRefId) {
        log.debug("Fetching OFOQ manifest status for functionalRefId: {}", functionalRefId);

        String url = requireApiUrl() + functionalRefId + "/";
        HttpHeaders headers = createHeaders();

        try {
            Thread.sleep(STATUS_POLL_DELAY_MS);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplates.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            int statusCode = response.getStatusCode().value();
            log.info("OFOQ manifest status retrieved for functionalRefId: {} with statusCode: {}", functionalRefId, statusCode);

            return OFOQCheckStatusCustomsResponseDto.builder()
                    .functionalReference(functionalRefId)
                    .statusCode(String.valueOf(statusCode))
                    .statusText(reasonPhrase(statusCode))
                    .responseMessage(extractMessage(response.getBody()))
                    .responseBody(response.getBody())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching manifest status for functionalRefId: {}", functionalRefId, e);
            throw new ValidationException("Interrupted while fetching manifest status");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            log.error("OFOQ manifest status fetch failed with status {}: {}", statusCode, ex.getResponseBodyAsString());
            return OFOQCheckStatusCustomsResponseDto.builder()
                    .functionalReference(functionalRefId)
                    .statusCode(String.valueOf(statusCode))
                    .statusText(reasonPhrase(statusCode))
                    .responseMessage(extractMessage(ex.getResponseBodyAsString()))
                    .responseBody(ex.getResponseBodyAsString())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error fetching OFOQ manifest status for functionalRefId: {}", functionalRefId, e);
            throw new ValidationException("Failed to fetch manifest status: " + e.getMessage());
        }
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception e) {
            log.debug("XML parser does not support all hardening options: {}", e.getMessage());
        }
        return factory;
    }

    private String reasonPhrase(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status.getReasonPhrase() : String.valueOf(statusCode);
    }

    /**
     * Extracts the text of the first {@code <Message>} element, mirroring the legacy ExtractMessage.
     */
    private String extractMessage(String xmlData) {
        if (xmlData == null || xmlData.isBlank()) {
            return null;
        }
        try {
            Document document = secureDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();

            NodeList messages = document.getElementsByTagNameNS("*", "Message");
            if (messages.getLength() == 0) {
                messages = document.getElementsByTagName("Message");
            }
            if (messages.getLength() > 0) {
                return messages.item(0).getTextContent();
            }
            log.debug("No <Message> element found in OFOQ response");
            return null;
        } catch (Exception e) {
            log.warn("Unable to parse OFOQ XML response, falling back to plain text extraction: {}", e.getMessage());
            int start = xmlData.indexOf("<Message>");
            int end = xmlData.indexOf("</Message>");
            if (start != -1 && end > start) {
                return xmlData.substring(start + "<Message>".length(), end);
            }
            return null;
        }
    }

}
