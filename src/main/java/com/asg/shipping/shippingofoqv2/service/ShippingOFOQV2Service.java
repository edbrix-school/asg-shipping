package com.asg.shipping.shippingofoqv2.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shippingofoqv2.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ShippingOFOQV2Service {

    Map<String, Object> listShippingOFOQ(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    OFOQVoyageDataResponse getShippingOFOQById(Long transactionPoid);

    OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request);

    OFOQCheckStatusResponseDto createShippingOFOQ(ShippingOFOQV2Request request);

    OFOQCheckStatusResponseDto checkStatus(@Valid OFOQCheckStatusDto request);

    OFOQCheckStatusResponseDto updateShippingOFOQ(Long transactionPoid, ShippingOFOQV2Request request);

    void deleteShippingOFOQ(Long transactionPoid);

    AmendBlDto amendBl(@Valid OFOQAmendBlRequestDto request);
}

