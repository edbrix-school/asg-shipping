package com.asg.shipping.linepayabletransfetasperreporting.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import org.springframework.data.domain.Pageable;

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
    Map<String, Object> searchLinePayableTransfer(String docId, FilterRequestDto filterRequest, Pageable pageable);

    /**
     * Delete (soft delete) a Line Payable Transfer As Per Reporting record
     */
    void deleteLinePayableTransfer(Long transactionPoid);

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
}
