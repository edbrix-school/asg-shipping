package com.asg.shipping.receipts.repository;

import com.asg.shipping.receipts.dto.ReceiptAutoPopulateChargeDto;
import com.asg.shipping.receipts.dto.ReceiptAutoPopulateContainerDto;
import com.asg.shipping.receipts.dto.TaxConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReceiptAutoPopulateRepository {

    List<ReceiptAutoPopulateChargeDto> findAvailableChargesForBl(
            Long blPoid,
            Long currentReceiptId
    );

    List<ReceiptAutoPopulateContainerDto> findAvailableContainersForBl(
            Long blPoid,
            Long currentReceiptId
    );

    ReceiptAutoPopulateChargeDto findDemurrageChargeRow(Long blPoid, Long companyPoid, BigDecimal totalDemAmount);

    BigDecimal findLinePoidByBlPoid(Long blPoid);

    LocalDate findArrivalDate(Long blPoid, String containerNo);

    Long findCompanyPoidByBlPoid(Long blPoid);

}