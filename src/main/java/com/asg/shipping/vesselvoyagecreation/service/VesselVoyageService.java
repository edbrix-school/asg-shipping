package com.asg.shipping.vesselvoyagecreation.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.vesselvoyagecreation.dto.CurrencyUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentTransferRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlFilter;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlRow;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlTab;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageResponse;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageUpsertRequest;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipEdiExceptionUploadEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipVoyageCurrencyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VesselVoyageService {

    Map<String, Object> listVoyages(FilterRequestDto request, Pageable pageable, String docId, LocalDate startDate, LocalDate endDate);

    VoyageResponse getVoyage(Long voyagePoid);

    VoyageResponse createVoyage(VoyageUpsertRequest request);

    VoyageResponse updateVoyage(Long voyagePoid, VoyageUpsertRequest request);

    Page<VoyageBlRow> listBls(Long voyagePoid, VoyageBlTab tab, VoyageBlFilter filter, Pageable pageable);

    List<VwShipEdiExceptionUploadEntity> getEdiErrors(Long voyagePoid);

    String reprocessEdi(Long voyagePoid);

    String uploadAndProcessEdi(Long voyagePoid, MultipartFile file);

    List<ShipVoyageTranshipDtlEntity> listTranshipments(Long voyagePoid);

    List<ShipVoyageTranshipDtlEntity> updateTranshipments(Long voyagePoid, TranshipmentUpdateRequest request);

    String transferTranshipments(Long voyagePoid, TranshipmentTransferRequest request);

    String importHnjnTranshipments(Long voyagePoid);

    List<VwShipVoyageCurrencyEntity> listVoyageCurrencies(Long voyagePoid);

    String updateCurrencyRates(Long voyagePoid, CurrencyUpdateRequest request);

    String resendCan(Long voyagePoid, Long blTransactionPoid);

    String createEmptyManifest(Long voyagePoid);

    String createTdr(Long voyagePoid, boolean createEmptyManifestFirst);

    String exportEdiCosco(Long voyagePoid, Long blPoid);

    String importGeneralCargo(Long voyagePoid);

    String importSelectedXl(Long voyagePoid);

    /**
     * Legacy: EdiMovesLoadDischarge — fires PROC_SHIP_CSCL_EDI_OUT_PP(ediDateValue, userPoid)
     */
    void ediMovesLoadDischarge(Long voyagePoid, String ediDateValue);

    byte[] downloadExcelExport(Long voyagePoid, String type, String outputFileName);

    Resource downloadManifestReport(Long voyagePoid, String freightCargo, String importExport);

    void deleteVoyage(Long voyagePoid);

    /**
     * Legacy: triggerMscDataLoad (DocumentAfterSave) — fires PROC_SHIP_BL_PAGE_SAVE_AFTER
     * when mscVesselVoyageReff is present and length > 5.
     */
    void triggerMscDataLoad(Long voyagePoid);

    /**
     * Legacy: fetchMscVoyageData (updateFetchVoyageData) — fires PROC_SHIP_DATA_TRN_EDI
     * to sync MSC voyage data. Returns result message.
     */
    String fetchMscVoyageData(Long voyagePoid);

    byte[] print(Long transactionPoid, String freightCargo, String importExport)
            throws net.sf.jasperreports.engine.JRException, java.sql.SQLException;
    
}


