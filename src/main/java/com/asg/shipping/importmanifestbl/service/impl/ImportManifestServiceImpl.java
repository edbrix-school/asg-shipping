package com.asg.shipping.importmanifestbl.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.dto.LoadEmailFaxRequestDto;
import com.asg.shipping.importManifestUpdate.entity.*;
import com.asg.shipping.importManifestUpdate.respository.*;
import com.asg.shipping.importManifestUpdate.util.ImportManifestBlMapper;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestbl.repository.ContainerDropdownRepository;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import com.asg.shipping.importmanifestbl.util.ImportManifestDropdownMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.utility.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;

import javax.sql.DataSource;


@Service
@Slf4j
@RequiredArgsConstructor
public class ImportManifestServiceImpl implements ImportManifestService {

    private final ShipBlManifestHdrRepository headerRepository;
    private final ImportManifestBlProcRepository procRepository;
    private final AddressDetailsRepository addressDetailsRepository;
    private final com.asg.shipping.importManifestUpdate.service.ImportManifestBlServiceImpl updateService;
    private final ImportManifestBlMapper mapper;
    private final DocumentSearchService documentService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final ContainerDropdownRepository containerDropdownRepository;
    private final BlManifestValidationRepository validationRepository;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    public ImportManifestBlRequestDto getImportManifest(Long transactionPoId) {
        ShipBlManifestHdr entity = findEntityById(transactionPoId);
        log.info("Getting Import Manifest BL with id: {}", transactionPoId);


        if (entity.getBlType() != null && !"IMPORT".equalsIgnoreCase(entity.getBlType())) {
            throw new ResourceNotFoundException("Import Manifest BL", "transactionPoid", transactionPoId.toString());
        }

        ImportManifestBlRequestDto requestDto = mapper.mapToDto(entity);
        ImportManifestBlRequestDto dto =   updateService.loadDetailTables(requestDto,transactionPoId);
        log.info("Successfully retrieved Import Manifest BL with id: {}", transactionPoId);
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long transactionPoId, DeleteReasonDto deleteReasonDto) {
        try {

            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            LocalDate transactionDate = entity.getTransactionDate() == null
                    ? null
                    : LocalDate.from(entity.getTransactionDate());

            documentDeleteService.deleteDocument(transactionPoId,"SHIP_BL_MANIFEST_HDR","TRANSACTION_POID",
                    deleteReasonDto,transactionDate);
            log.info("Soft deleted header for transactionPoId: {}", transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to delete: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting Import Manifest BL for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request) {
        try {
            findEntityById(transactionPoId);
            return procRepository.updateEmailVerification(transactionPoId, request);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to update: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error updating email verification for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResendCanResponseDto resendCan(Long transactionPoId) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            return procRepository.resendCan(entity.getVoyageTransactionPoid(), transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to resend CAN: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error resending CAN for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.sendEdiEmails(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to send EDI emails: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error sending EDI emails for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request) {
        try {
            findEntityById(transactionPoId);
            var addressDetails = addressDetailsRepository.findByAddressMasterPoidAndAddressType(
                    request.getAddressMasterPoid(), "CAN");
            var emailFaxDetails = addressDetails.stream()
                    .map(ad -> EmailFaxDetailDto.builder()
                            .addressPoid(Long.valueOf(ad.getAddressPoid()))
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .fax(ad.getFax())
                            .addressType(request.getAddressType())
                            .build())
                    .toList();
            log.info("Loaded email/fax data for transactionPoId: {}, count: {}", transactionPoId, emailFaxDetails.size());
            return LoadEmailFaxResponseDto.builder().emailFaxDetails(emailFaxDetails).build();
        } catch (ResourceNotFoundException e) {
            log.error("Failed to load email/fax: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error loading email/fax data for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    public BlStatusResponseDto getBlStatus(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.getBlStatus(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to get BL status: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting BL status for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    public ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto request, Long companyPoid, Long groupPoid) {
      return updateService.createImportManifestBl(request);
    }

    @Override
    public ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid) {
        return updateService.updateImportManifestBl(id,dto,companyPoid,groupPoid);
    }

    @Override
    public ContainersDropDownDto getContainerTypesByVoyage(Long voyageTransPoid) {

        List<ContainerTypeDTO> containerTypes =
                containerDropdownRepository.findContainerTypes(voyageTransPoid)
                        .stream()
                        .map(ImportManifestDropdownMapper::mapContainer)
                        .toList();

        List<CommodityDTO> commodities =
                containerDropdownRepository.findAllCommodities()
                        .stream()
                        .map(ImportManifestDropdownMapper::mapCommodity)
                        .toList();

        return ContainersDropDownDto.builder()
                .containerTypes(containerTypes)
                .commodities(commodities)
                .build();
    }

    @Override
    public DefaultValueDto getDefaultValues(String docId) {
        return procRepository.callDefaultGetValue(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid(),
                docId
        );


    }

    @Override
    public byte[] printUnclearedCargoNotice(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        JasperReport mainReport = printService.load("Shipping/SH/CAN_SHIPPING_UNCLEARED.jrxml");
        return printService.fillReportToPdf(mainReport,params,dataSource);
    }

    @Override
    public byte[] printProformaInvoice(Long transactionPoid, LocalDate demChargesTill, Long percentage) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        params.put("P_DEMURRAGE_DATE", demChargesTill != null ? demChargesTill : LocalDate.now());
        params.put("P_DISCOUNT", percentage != null ? percentage : 0);
        params.put("SUBREPORT2",printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL_SUBREPORT2.jrxml"));
        params.put("SUBREPORT3",printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL_SUBREPORT3.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] printCargoArrivalNotice(Long voyageTransactionPoid, Long transactionPoid) throws Exception {

        Map<String, Object> params =
                printService.buildBaseParams(transactionPoid, "100-102");

        String lineCode = validationRepository.getLineCode(voyageTransactionPoid);
        String jrxmlPath = "Shipping/CAN_SHIPPING.jrxml";
        if ("MSC".equalsIgnoreCase(lineCode)) {
            jrxmlPath = "Shipping/SH/CAN_SHIPPING_msc.jrxml";
        } else if ("COS".equalsIgnoreCase(lineCode)) {
            jrxmlPath = "Shipping/SH/CAN_SHIPPING_COS.jrxml";
        }

        JasperReport mainReport = printService.load(jrxmlPath);

        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] printCargoManifest(Long transactionPoid, boolean isCargoManifestPrint) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        params.put("P_FREIGHTCARGO", isCargoManifestPrint ? "FALSE" : "TRUE");
        params.put("SUBREPORT_MARK_INFO", printService.load("Shipping/SH/cargo/Mark_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_FREIGHT_DETAIL", printService.load("Shipping/SH/cargo/Freight_Detail_Subreport1.jrxml"));
        params.put("SUBREPORT_CONTAINER_INFO", printService.load("Shipping/SH/cargo/Container_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_DESCRIPTION_INFO", printService.load("Shipping/SH/cargo/Description_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_TOTAL_COUNT", printService.load("Shipping/SH/cargo/TotalCount_By_Size.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/cargo/Manifest_Cargo_WithCharges.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] printCheckPortCharges(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        params.put("P_TILL_DATE", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        params.put("SUBREPORT_PORT_STORAGE", printService.load("Shipping/SH/PORT_STORAGE_IMP_FULL.jrxml"));
        params.put("SUBREPORT_PORT_STORAGE_EMPTY", printService.load("Shipping/SH/PORT_STORAGE_IMP_FULL_EMPTY_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/PORT_STORAGE_CALC.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public Map<String, Object> list(FilterRequestDto request, Pageable pageable) {
        try {

            String operator = documentService.resolveOperator(request);
            String isDeleted = documentService.resolveIsDeleted(request);
            List<FilterDto> filters = documentService.resolveFilters(request);


            RawSearchResult raw = documentService.search(
                    UserContext.getDocumentId(),
                    filters,
                    operator,
                    pageable,
                    isDeleted,
                    "BL_NUMBER",
                    "TRANSACTION_POID"
            );
            
            Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
            return PaginationUtil.wrapPage(page, raw.displayFields());
        } catch (Exception e) {
            log.error("Error listing Import Manifest BLs", e);
            throw e;
        }
    }

    private ShipBlManifestHdr findEntityById(Long transactionPoId) {
        return headerRepository.findById(transactionPoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship BL Manifest", "transactionPoId", transactionPoId));
    }



}
