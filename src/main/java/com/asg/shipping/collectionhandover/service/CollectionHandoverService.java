package com.asg.shipping.collectionhandover.service;

import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CollectionHandoverService {

    Map<String, Object> searchCollectionHandovers(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable, LocalDate startDate, LocalDate endDate);

    CollectionHandoverDto getCollectionHandover(Long id);

    /** Fetch auto-populated header data for a new record (DocumentAfterNew logic) */
    DayCloseSummaryProjection getNewHandoverData(Long groupPoid, Long companyPoid, String transactionDate);

    /** Load denomination rows for the given currency code (LoadDinominationCurTypes logic) */
    List<Map<String, Object>> getDenominations(String currencyCode);

    CollectionHandoverDto createCollectionHandover(CollectionHandoverCreateDTO dto, Long groupPoid, Long userPoid);

    CollectionHandoverDto updateCollectionHandover(Long id, CollectionHandoverUpdateDTO dto, Long groupPoid, Long userPoid);

    void deleteCollectionHandover(Long id);

    void toggleVerifyStatus(Long id, String verifiedRcvd, String mainOfcRemarks);

    byte[] print(Long transactionPoid) throws net.sf.jasperreports.engine.JRException, java.sql.SQLException;
}
