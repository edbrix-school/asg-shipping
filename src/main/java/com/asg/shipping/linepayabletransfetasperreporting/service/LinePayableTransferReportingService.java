package com.asg.shipping.linepayabletransfetasperreporting.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.ApplyExchangeRateRequest;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingCreateDTO;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingDtlDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LinePayableTransferReportingUpdateDTO;
import com.asg.shipping.linepayabletransfetasperreporting.dto.LoadDataByDateRangeRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Line Payable Transfer As Per Reporting operations
 */
public interface LinePayableTransferReportingService {

    /**
     * Create a new Line Payable Transfer As Per Reporting record
     */
    LinePayableTransferReportingDto createLinePayableTransfer(LinePayableTransferReportingCreateDTO createDTO);

    /**
     * Update an existing Line Payable Transfer As Per Reporting record
     */
    LinePayableTransferReportingDto updateLinePayableTransfer(Long transactionPoid, LinePayableTransferReportingUpdateDTO updateDTO);

    /**
     * Get Line Payable Transfer As Per Reporting record by transaction POID
     */
    LinePayableTransferReportingDto getLinePayableTransferById(Long transactionPoid);

    /**
     * Search Line Payable Transfer As Per Reporting records
     */
    Map<String, Object> searchLinePayableTransfer(String docId, FilterRequestDto filterRequest, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * Delete (soft delete) a Line Payable Transfer As Per Reporting record
     */
    void deleteLinePayableTransfer(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Load data by date range using stored procedure
     */
    List<LinePayableTransferReportingDtlDto> loadDataByDateRange(Long transactionPoid, LoadDataByDateRangeRequest request);

    /**
     * Load data before create (without transaction ID)
     */
    List<LinePayableTransferReportingDtlDto> loadDataBeforeCreate(LoadDataByDateRangeRequest request);

    /**
     * Process weekly BL report data
     */
    List<LinePayableTransferReportingDtlDto> processWeeklyBlReport(Long transactionPoid, LoadDataByDateRangeRequest request);

    /**
     * Process weekly BL report before create (without transaction ID)
     */
    List<LinePayableTransferReportingDtlDto> processWeeklyBeforeCreate(LoadDataByDateRangeRequest request);

    /**
     * Apply a new exchange rate to every detail row carrying the given currency code
     */
    List<LinePayableTransferReportingDtlDto> applyExchangeRate(ApplyExchangeRateRequest request);
}
