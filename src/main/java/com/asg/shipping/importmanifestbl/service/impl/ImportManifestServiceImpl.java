package com.asg.shipping.importmanifestbl.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.service.LovService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.entity.*;
import com.asg.shipping.importmanifestupdate.event.BlManifestSaveEvent;
import com.asg.shipping.importmanifestupdate.respository.*;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import com.asg.shipping.importmanifestupdate.constants.BlManifestValidationMessages;
import com.asg.shipping.importmanifestupdate.util.ImportManifestBlMapper;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestbl.repository.ContainerDropdownRepository;
import com.asg.shipping.importmanifestbl.service.ImportManifestService;
import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import com.asg.shipping.importmanifestupdate.service.ImportManifestBlServiceImpl;
import com.asg.shipping.importmanifestbl.util.ImportManifestDropdownMapper;
import com.asg.shipping.importmanifestbl.util.ImportManifestMapper;
import com.asg.shipping.shippingffchargemaster.repository.ShipChargeMasterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.utility.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;

import javax.sql.DataSource;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportManifestServiceImpl implements ImportManifestService {

    private static final String CARGO_TYPE_DESCRIPTION = "CARGO";
    private static final String CARGO_TYPE_MARKS = "MARKS";

    private final ShipBlManifestHdrRepository headerRepository;
    private final ImportManifestBlProcRepository procRepository;
    private final AddressDetailsRepository addressDetailsRepository;
    private final ShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ShipBlManifestPartBLRepository containerPrtRepository;
    private final ShipBlManifestEmailFaxDtlRepository emailFaxDtlRepository;
    private final ShipBlManifestMafiDtlRepository mafiDtlRepository;
    private final ImportManifestBlServiceImpl updateService;
    private final ImportManifestBlMapper mapper;
    private final DocumentSearchService documentService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final ContainerDropdownRepository containerDropdownRepository;
    private final BlManifestValidationRepository validationRepository;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    private final BlManifestValidationService blManifestValidationService;
    private final ShipChargeMasterRepository chargeMasterRepository;
    private final LovService lovService;
    private static final String ACTION_ISCREATED = "ACTION_ISCREATED";
    private static final String ACTION_ISUPDATED = "ACTION_ISUPDATED";
    private static final String ACTION_ISDELETED = "ACTION_ISDELETED";
    private static final String ACTION_NOCHANGES = "ACTION_NOCHANGES";

    private static final String LOG_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";

    @PersistenceContext
    private final EntityManager entityManager;


    @Override
    public ImportManifestBlDto getImportManifest(Long transactionPoId) {
        ShipBlManifestHdr entity = findEntityById(transactionPoId);
        log.info("Getting Import Manifest BL with id: {}", transactionPoId);




        ImportManifestBlDto dto = ImportManifestMapper.mapToDto(entity);

        ImportManifestBlRequestDto updateDto = updateService.getImportManifestBl(transactionPoId);

        dto.setSimpleCargoDescription(getCargoDescriptionByType(updateDto.getCargoDescriptions(), CARGO_TYPE_DESCRIPTION, "DESCRIPTION"));
        dto.setSimpleCargoMarks(getCargoDescriptionByType(updateDto.getCargoDescriptions(), CARGO_TYPE_MARKS, "MARKS"));
        dto.setDescriptionsAndMarks(ImportManifestMapper.mapToDescriptionAndMarks(updateDto.getCargoDescriptions()));
        // otherNotifies already set from entity in mapToDto(entity) above — keep it, don't overwrite from updateDto
        dto.setGeneralCargoDetails(ImportManifestMapper.mapToGeneralCargoDetails(updateDto.getGeneralCargoDetails()));
        dto.setContainers(ImportManifestMapper.mapToContainers(updateDto.getContainers()));
        dto.setCharges(ImportManifestMapper.mapToCharges(updateDto.getChargeDetails()));
        dto.setOtherCharges(ImportManifestMapper.mapToChargesOther(updateDto.getChargeDetails()));
        dto.setPartBls(ImportManifestMapper.mapToPartBls(updateDto.getPartBls()));
        dto.setMafiDetails(ImportManifestMapper.mapToMafiDetails(updateDto.getMafiDetails()));
        dto.setAddressDetails(ImportManifestMapper.mapToAddressDetails(updateDto.getNotifyParties()));
        enrichLovData(dto);

        log.info("Successfully retrieved Import Manifest BL with id: {}", transactionPoId);
        return dto;
    }

    private void enrichLovData(ImportManifestBlDto dto) {
        if (dto == null) return;

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        // --- Batch collect all poids/codes per LOV name ---
        // Header
        List<Long> quotationPoids = dto.getQuotationPoid() != null ? List.of(dto.getQuotationPoid()) : List.of();

        // General cargo
        List<GeneralCargoDto> generalCargos = dto.getGeneralCargoDetails() != null ? dto.getGeneralCargoDetails() : List.of();
        List<Long> commodityPoidsGC = generalCargos.stream().map(GeneralCargoDto::getCommodityPoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<Long> destPortPoidsGC = generalCargos.stream().map(GeneralCargoDto::getDestinationPortPoid).filter(p -> p != null).distinct().collect(Collectors.toList());

        // Containers
        List<ContainerDto> containers = dto.getContainers() != null ? dto.getContainers() : List.of();
        List<Long> commodityPoidsC = containers.stream().map(ContainerDto::getCommodityPoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<String> isoTypes = containers.stream().map(ContainerDto::getEquipmentIsoType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> imcoTypes = containers.stream().map(ContainerDto::getImcoType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> oogTypes = containers.stream().map(ContainerDto::getOogType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());

        // Charges
        List<ChargeDto> charges = dto.getCharges() != null ? dto.getCharges() : List.of();
        List<Long> chargePoids = charges.stream().map(ChargeDto::getChargePoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<String> chargeTypes = charges.stream().map(ChargeDto::getChargeType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> currencyCodes = charges.stream().map(ChargeDto::getCurrencyCode).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> freightTypes = charges.stream().map(ChargeDto::getFreightType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> basisCodes = charges.stream().map(ChargeDto::getBasisPoid).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<Long> paidAtPortPoids = charges.stream().map(ChargeDto::getPaidAtPortPoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<Long> receiptInvoicePoids = charges.stream().map(ChargeDto::getReceiptInvoicePoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<Long> taxPoids = charges.stream().map(ChargeDto::getTaxPoid).filter(p -> p != null).distinct().collect(Collectors.toList());

        // Other charges
        List<ChargeOtherDto> otherCharges = dto.getOtherCharges() != null ? dto.getOtherCharges() : List.of();
        List<Long> otherChargePoids = otherCharges.stream().map(ChargeOtherDto::getChargePoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<String> otherChargeTypes = otherCharges.stream().map(ChargeOtherDto::getChargeType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> otherCurrencyCodes = otherCharges.stream().map(ChargeOtherDto::getCurrencyCode).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> otherFreightTypes = otherCharges.stream().map(ChargeOtherDto::getFreightType).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<String> otherBasisCodes = otherCharges.stream().map(ChargeOtherDto::getBasis).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<Long> otherPaidAtPortPoids = otherCharges.stream().map(ChargeOtherDto::getPaidAtPortPoid).filter(p -> p != null).distinct().collect(Collectors.toList());

        // Part BLs
        List<PartBlDto> partBls = dto.getPartBls() != null ? dto.getPartBls() : List.of();
        List<Long> partBlCommodityPoids = partBls.stream().map(PartBlDto::getCommodityPoid).filter(p -> p != null).distinct().collect(Collectors.toList());
        List<String> containerNos = partBls.stream().map(PartBlDto::getContainerNo).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.toList());
        List<Long> partBlContainerPoids = new ArrayList<>();
        partBls.forEach(pb -> {
            if (pb.getContainerPoid() != null) partBlContainerPoids.add(pb.getContainerPoid());
            String containerNo = pb.getContainerNo() != null ? pb.getContainerNo().trim() : null;
            if (containerNo != null && containerNo.matches("\\d+")) partBlContainerPoids.add(Long.parseLong(containerNo));
        });
        List<Long> distinctPartBlContainerPoids = partBlContainerPoids.stream().distinct().collect(Collectors.toList());

        // Merge port poids across sections
        List<Long> allPortPoids = new ArrayList<>();
        allPortPoids.addAll(destPortPoidsGC);
        allPortPoids.addAll(paidAtPortPoids);
        allPortPoids.addAll(otherPaidAtPortPoids);
        List<Long> distinctPortPoids = allPortPoids.stream().distinct().collect(Collectors.toList());

        // Merge commodity poids
        List<Long> allCommodityPoids = new ArrayList<>();
        allCommodityPoids.addAll(commodityPoidsGC);
        allCommodityPoids.addAll(commodityPoidsC);
        allCommodityPoids.addAll(partBlCommodityPoids);
        List<Long> distinctCommodityPoids = allCommodityPoids.stream().distinct().collect(Collectors.toList());

        // Merge charge types / currency / freight types
        List<String> allChargeTypes = new ArrayList<>(chargeTypes);
        allChargeTypes.addAll(otherChargeTypes);
        List<String> allCurrencyCodes = new ArrayList<>(currencyCodes);
        allCurrencyCodes.addAll(otherCurrencyCodes);
        List<String> allFreightTypes = new ArrayList<>(freightTypes);
        allFreightTypes.addAll(otherFreightTypes);
        List<String> allBasisCodes = new ArrayList<>(basisCodes);
        allBasisCodes.addAll(otherBasisCodes);
        List<Long> allChargePoids = new ArrayList<>(chargePoids);
        allChargePoids.addAll(otherChargePoids);

        // --- Single batch fetch per LOV name ---
        Map<Long, LovItem> quotationMap = lovService.getLovItemsByPoids(quotationPoids, "SHIP_QUOTATION_IMPORT", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> commodityMap = lovService.getLovItemsByPoids(distinctCommodityPoids, "COMODITY", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> portMap = lovService.getLovItemsByPoids(distinctPortPoids, "PORT_MASTER", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> isoTypeMap = lovService.getLovItemsByCodes(isoTypes, "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> imcoTypeMap = lovService.getLovItemsByCodes(imcoTypes, "IMCO_CLASS", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> oogTypeMap = lovService.getLovItemsByCodes(oogTypes, "OOG_TYPE", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> chargeMasterMap = lovService.getLovItemsByPoids(allChargePoids, "CHARGE_MASTER", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> chargeTypeMap = lovService.getLovItemsByCodes(allChargeTypes.stream().distinct().collect(Collectors.toList()), "CHARGE_TYPE", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> currencyMap = lovService.getLovItemsByCodes(allCurrencyCodes.stream().distinct().collect(Collectors.toList()), "CURRENCY", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> freightTypeMap = lovService.getLovItemsByCodes(allFreightTypes.stream().distinct().collect(Collectors.toList()), "SHIP_FREIGHT_TYPE", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> basisMap = lovService.getLovItemsByCodes(allBasisCodes.stream().distinct().collect(Collectors.toList()), "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> receiptInvoiceMap = lovService.getLovItemsByPoids(receiptInvoicePoids, "MANIFEST_RECEIPT_INVOICE", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> taxMap = lovService.getLovItemsByPoids(taxPoids, "TAX_MASTER", groupPoid, companyPoid, userPoid);
        Map<String, LovItem> containerPartMap = lovService.getLovItemsByCodes(containerNos, "SH_CONTAINER_PART", groupPoid, companyPoid, userPoid);
        Map<Long, LovItem> containerPartByPoidMap = lovService.getLovItemsByPoids(distinctPartBlContainerPoids, "SH_CONTAINER_PART", groupPoid, companyPoid, userPoid);

        // --- Apply from maps ---
        if (dto.getQuotationPoid() != null)
            dto.setQuotationDet(quotationMap.get(dto.getQuotationPoid()));

        for (GeneralCargoDto gc : generalCargos) {
            if (gc.getCommodityPoid() != null) gc.setCommodityDet(commodityMap.get(gc.getCommodityPoid()));
            if (gc.getDestinationPortPoid() != null) gc.setDestinationPortDet(portMap.get(gc.getDestinationPortPoid()));
        }

        for (ContainerDto c : containers) {
            if (c.getCommodityPoid() != null) c.setCommodityDet(commodityMap.get(c.getCommodityPoid()));
            if (c.getEquipmentIsoType() != null) c.setEquipmentIsoTypeDet(isoTypeMap.get(c.getEquipmentIsoType().toUpperCase()));
            if (c.getImcoType() != null) c.setImcoTypeDet(imcoTypeMap.get(c.getImcoType().toUpperCase()));
            if (c.getOogType() != null) c.setOogTypeDet(oogTypeMap.get(c.getOogType().toUpperCase()));
        }

        for (ChargeDto ch : charges) {
            if (ch.getChargePoid() != null) ch.setChargeDet(chargeMasterMap.get(ch.getChargePoid()));
            if (ch.getChargeType() != null) ch.setChargeTypeDet(chargeTypeMap.get(ch.getChargeType().toUpperCase()));
            if (ch.getCurrencyCode() != null) ch.setCurrencyCodeDet(currencyMap.get(ch.getCurrencyCode().toUpperCase()));
            if (ch.getFreightType() != null) ch.setFreightTypeDet(freightTypeMap.get(ch.getFreightType().toUpperCase()));
            if (ch.getBasisPoid() != null) ch.setBasisDet(basisMap.get(ch.getBasisPoid().toUpperCase()));
            if (ch.getPaidAtPortPoid() != null) ch.setPaidAtPortDet(portMap.get(ch.getPaidAtPortPoid()));
            if (ch.getReceiptInvoicePoid() != null) ch.setReceiptInvoiceDet(receiptInvoiceMap.get(ch.getReceiptInvoicePoid()));
            if (ch.getTaxPoid() != null) ch.setTaxDet(taxMap.get(ch.getTaxPoid()));
        }

        for (ChargeOtherDto co : otherCharges) {
            if (co.getChargePoid() != null) co.setChargeDet(chargeMasterMap.get(co.getChargePoid()));
            if (co.getChargeType() != null) co.setChargeTypeDet(chargeTypeMap.get(co.getChargeType().toUpperCase()));
            if (co.getCurrencyCode() != null) co.setCurrencyCodeDet(currencyMap.get(co.getCurrencyCode().toUpperCase()));
            if (co.getFreightType() != null) co.setFreightTypeDet(freightTypeMap.get(co.getFreightType().toUpperCase()));
            if (co.getBasis() != null) co.setBasisDet(basisMap.get(co.getBasis().toUpperCase()));
            if (co.getPaidAtPortPoid() != null) co.setPaidAtPortDet(portMap.get(co.getPaidAtPortPoid()));
        }

        for (PartBlDto pb : partBls) {
            if (pb.getCommodityPoid() != null) pb.setCommodityDet(commodityMap.get(pb.getCommodityPoid()));

            LovItem containerDet = pb.getContainerPoid() != null ? containerPartByPoidMap.get(pb.getContainerPoid()) : null;
            if (containerDet == null && pb.getContainerNo() != null && !pb.getContainerNo().isBlank()) {
                String containerNo = pb.getContainerNo().trim();
                if (containerNo.matches("\\d+")) {
                    containerDet = containerPartByPoidMap.get(Long.parseLong(containerNo));
                }
                if (containerDet == null) {
                    containerDet = containerPartMap.get(containerNo.toUpperCase());
                }
            }
            if (containerDet != null && containerDet.getCode() != null) {
                pb.setContainerNo(containerDet.getCode());
            }
            pb.setContainerNoDet(containerDet);
        }
    }

    @Override
    @Transactional
    public void delete(Long transactionPoId, DeleteReasonDto deleteReasonDto) {
        try {

            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            LocalDate transactionDate = entity.getTransactionDate() == null
                    ? null
                    : LocalDate.from(entity.getTransactionDate());

            documentDeleteService.deleteDocument(transactionPoId, "SHIP_BL_MANIFEST_HDR", "TRANSACTION_POID",
                    deleteReasonDto, transactionDate);
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
    public EmailVerificationResponseDto updateEmailVerification(Long transactionPoId,
            EmailVerificationRequestDto request) {
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
    public ResendCanResponseDto resendCan(Long transactionPoId, String updateDemurrage) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            return procRepository.resendCan(entity.getVoyageTransactionPoid(), transactionPoId, updateDemurrage);
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
            return procRepository.getEdiEmails(transactionPoId);
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
    public LoadEmailFaxResponseDto loadEmailFax(Long addressMasterPoid, String addressType) {
        try {
            var addressDetails = addressDetailsRepository.findByAddressMasterPoidAndAddressType(
                    addressMasterPoid, "CAN");
            var emailFaxDetails = addressDetails.stream()
                    .map(ad -> EmailFaxDetailDto.builder()
                            .actionType("isCreated")
                            .addressPoid(ad.getAddressPoid() != null ? new BigDecimal(ad.getAddressPoid()) : null)
                            .addressType(addressType)
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .sendYesNo("N")
                            .sendEmailFax("EMAIL")
                            .build())
                    .toList();
            log.info("Loaded email/fax data for addressMasterPoid: {}, count: {}", addressMasterPoid, emailFaxDetails.size());
            return LoadEmailFaxResponseDto.builder().emailFaxDetails(emailFaxDetails).build();
        } catch (Exception e) {
            log.error("Error loading email/fax data for addressMasterPoid: {}", addressMasterPoid, e);
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
    @Transactional
    public ImportManifestBlResponseDto createImportManifestBl(ImportManifestBlDto dto, Long companyPoid,
            Long groupPoid) {
        log.info("Creating new Import Manifest BL");

        performBlManifestValidations(dto, null);

        ShipBlManifestHdr entity = ImportManifestMapper.mapToEntity(dto, new ShipBlManifestHdr());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setGroupPoid(UserContext.getGroupPoid());
        autoPopulateDefaults(entity);
        updateService.formatEdiFields(entity);

        ShipBlManifestHdr saved = headerRepository.saveAndFlush(entity);
        entityManager.refresh(entity);

        Long transactionPoid = saved.getTransactionPoid();
        procRepository.validateBeforeSave(dto.getVesselVoyagePoid(), transactionPoid, dto.getQuotationPoid(),
                dto.getFreight(), dto.getBookedByPrincipal());

        log.info("BL Manifest header saved with transactionPoid: {}", transactionPoid);

        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), saved.getDocRef()));

        List<String> logEntries = new ArrayList<>();
        saveGeneralCargoDetails(dto.getGeneralCargoDetails(), transactionPoid, logEntries);
        saveCargoDescriptions(buildSimpleCargoDescriptions(dto), transactionPoid, logEntries);
        saveContainers(dto.getContainers(), transactionPoid, logEntries);
        saveChargeDetails(dto.getCharges(), transactionPoid, logEntries);
        saveOtherChargeDetails(dto.getOtherCharges(), transactionPoid, logEntries);
        savePartBls(dto.getPartBls(), transactionPoid, logEntries);
        saveNotifyParties(dto.getAddressDetails(), transactionPoid, logEntries, dto);
        saveMafiDetails(dto.getMafiDetails(), transactionPoid, logEntries);

        logSummaryEntries(logEntries, UserContext.getDocumentId(), transactionPoid.toString());

        boolean hasManualTotals = saved.getTotalNetVolume() != null
                || saved.getTotalWeight() != null
                || saved.getTotalNetWeight() != null
                || saved.getTotalNoOfPacks() != null;

        if (!hasManualTotals) {
            eventPublisher.publishEvent(new BlManifestSaveEvent(saved, UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(), "AUTOSUMWEIGHTPACKATE"));
        }

        log.info("Successfully created Import Manifest BL with id: {}", transactionPoid);
        return new ImportManifestBlResponseDto("Import manifest created successfully", transactionPoid);
    }

    @Override
    @Transactional
    public ImportManifestBlResponseDto updateImportManifestBl(Long id, ImportManifestBlDto dto) {
        log.info("Updating Import Manifest BL with id: {}", id);

        ShipBlManifestHdr existingEntity = headerRepository.findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Import Manifest BL", "transactionPoid", id.toString()));

        ShipBlManifestHdr oldEntity = new ShipBlManifestHdr();
        BeanUtils.copyProperties(existingEntity, oldEntity);

        performBlManifestValidations(dto, id);

        procRepository.validateBeforeSave(dto.getVesselVoyagePoid(), id, dto.getQuotationPoid(),
                dto.getFreight(), dto.getBookedByPrincipal());

        String oldFreightStatus = existingEntity.getFreightStatus();
        String oldDoNo = existingEntity.getDoNo();

        String existingDocRef = existingEntity.getDocRef();
        ShipBlManifestHdr entity = ImportManifestMapper.mapToEntity(dto, existingEntity);
        entity.setDocRef(existingDocRef); // DOC_REF is DB-generated on INSERT, must never change on UPDATE
        if (hasAnyEdiChange(dto)) {
            updateService.formatEdiFields(entity);
        }

        ShipBlManifestHdr saved = headerRepository.saveAndFlush(entity);
        updateDetailTables(dto, saved.getTransactionPoid());

        loggingService.logChanges(oldEntity, existingEntity, ShipBlManifestHdr.class, UserContext.getDocumentId(),
                id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        updateService.registerAfterCommitActions(saved, UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
                oldDoNo, oldFreightStatus, oldDoNo, dto.getFreight());

        log.info("Successfully updated Import Manifest BL with id: {}", id);
        return ImportManifestBlResponseDto.builder()
                .status("Import manifest updated successfully").transactionPoid(id).build();
    }

    @Override
    public ContainersDropDownDto getContainerTypesByVoyage(Long voyageTransPoid) {

        List<ContainerTypeDTO> containerTypes = containerDropdownRepository.findContainerTypes(voyageTransPoid)
                .stream()
                .map(ImportManifestDropdownMapper::mapContainer)
                .toList();

        List<CommodityDTO> commodities = containerDropdownRepository.findAllCommodities()
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
                docId);

    }

    @Override
    @Transactional
    public String saveEmails(Long transactionPoId, SaveEmailsRequestDto request) {
        if (!Boolean.TRUE.equals(request.getUpdateConsignee()) && !Boolean.TRUE.equals(request.getUpdateNotify())) {
            throw new IllegalArgumentException("Select at least Consignee or Notify");
        }
        if (request.getEmailsText() == null || request.getEmailsText().trim().isEmpty()) {
            throw new IllegalArgumentException("Emails cannot be empty");
        }

        // P_CN_NF_FLAG: C=Consignee only, N=Notify only, B=Both
        String cnNfFlag;
        if (Boolean.TRUE.equals(request.getUpdateConsignee()) && Boolean.TRUE.equals(request.getUpdateNotify())) {
            cnNfFlag = "B";
        } else if (Boolean.TRUE.equals(request.getUpdateConsignee())) {
            cnNfFlag = "C";
        } else {
            cnNfFlag = "N";
        }

        // P_CURRENT_BOTH: whether to also insert into GLOBAL_ADDRESS_DETAILS (B=yes, C/N=manifest only)
        String currentBoth = request.getScope() != null ? request.getScope() : "C";

        String[] emails = request.getEmailsText().split(",");
        for (int i = 0; i < emails.length; i += 2) {
            String email1 = emails[i].trim();
            String email2 = (i + 1 < emails.length) ? emails[i + 1].trim() : null;
            procRepository.saveEmailsToDb(transactionPoId, cnNfFlag, email1, email2, currentBoth);
        }

        return "Emails saved successfully";
    }

    @Override
    public byte[] printUnclearedCargoNotice(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        JasperReport mainReport = printService.load("Shipping/SH/CAN_SHIPPING_UNCLEARED.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] printProformaInvoice(Long transactionPoid, LocalDate demChargesTill, Long percentage)
            throws Exception {
        String demDate = (demChargesTill != null ? demChargesTill : LocalDate.now()).format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        log.info("printProformaInvoice: transactionPoid={}, demDate={}, discount={}", transactionPoid, demDate, percentage);
        try (java.sql.Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("ALTER SESSION SET CURRENT_SCHEMA = QA_DB_USER");
            String sql = "SELECT COUNT(*) FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, transactionPoid);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) log.info("printProformaInvoice: HDR row count={}", rs.getInt(1));
            }
        }
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        params.put("P_DEMURRAGE_DATE", demDate);
        params.put("P_DISCOUNT", String.valueOf(percentage != null ? percentage : 0));
        params.put("SUBREPORT2", printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL_SUBREPORT2.jrxml"));
        params.put("SUBREPORT3", printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL_SUBREPORT3.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/SH_PROFORMA_INV_IMP_MANFST_BL.jrxml");
        byte[] pdf = printService.fillReportToPdf(mainReport, params, dataSource);
        log.info("printProformaInvoice: pdf size={} bytes", pdf.length);
        return pdf;
    }

    @Override
    public byte[] printCargoArrivalNotice(Long voyageTransactionPoid, Long transactionPoid) throws Exception {

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");

        String lineCode = validationRepository.getLineCode(voyageTransactionPoid);
        String jrxmlPath = "Shipping/SH/CAN_SHIPPING.jrxml";
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
        params.put("SUBREPORT_MARK_INFO", printService.load("Shipping/SH/Cargo/Mark_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_FREIGHT_DETAIL", printService.load("Shipping/SH/Cargo/Freight_Detail_Subreport1.jrxml"));
        params.put("SUBREPORT_CONTAINER_INFO", printService.load("Shipping/SH/Cargo/Container_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_DESCRIPTION_INFO",
                printService.load("Shipping/SH/Cargo/Description_Info_Subreport1.jrxml"));
        params.put("SUBREPORT_TOTAL_COUNT", printService.load("Shipping/SH/Cargo/TotalCount_By_Size.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/Cargo/Manifest_Cargo_WithCharges.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] printCheckPortCharges(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-102");
        params.put("P_TILL_DATE", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        params.put("SUBREPORT_PORT_STORAGE", printService.load("Shipping/SH/PORT_STORAGE_IMP_FULL.jrxml"));
        params.put("SUBREPORT_PORT_STORAGE_EMPTY",
                printService.load("Shipping/SH/PORT_STORAGE_IMP_FULL_EMPTY_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/PORT_STORAGE_CALC.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public Map<String, Object> list(FilterRequestDto request, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        try {
            String operator = documentService.resolveOperator(request);
            String isDeleted = documentService.resolveIsDeleted(request);
            List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", fromDate, toDate);

            RawSearchResult raw = documentService.search(
                    UserContext.getDocumentId(),
                    filters,
                    operator,
                    pageable,
                    isDeleted,
                    "BL_NUMBER",
                    "TRANSACTION_POID");

            Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
            return PaginationUtil.wrapPage(page, raw.displayFields());
        } catch (Exception e) {
            log.error("Error listing Import Manifest BLs", e);
            throw e;
        }
    }

    private ShipBlManifestHdr findEntityById(Long transactionPoId) {
        return headerRepository.findById(transactionPoId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Ship BL Manifest", "transactionPoId", transactionPoId));
    }

    private void performBlManifestValidations(ImportManifestBlDto dto, Long transactionPoid) {
        blManifestValidationService.validateMandatoryFields(
                dto.getVesselVoyagePoid(), dto.getCargo(), dto.getBlNumber(), dto.getBlType());

        validateBlManifestDTO(dto, transactionPoid);

        blManifestValidationService.validateHoldReasons(
                dto.getHoldReason(), dto.getHoldCanAuto(), dto.getHoldRemarks());

        boolean addressFound = checkAddressesExist(dto);
        blManifestValidationService.validateAddressesForCan(
                dto.getHoldReason(), dto.getConsigneePoid(), dto.getNotify1Poid(),
                dto.getManualCanSend(), addressFound);

        List<ContainerDto> activeContainers = filterActiveContainers(dto.getContainers());
        List<ChargeDto> activeCharges = filterActiveCharges(dto.getCharges());

        blManifestValidationService.validateContainerFields(activeContainers);

        blManifestValidationService.validateFinancialGain(activeCharges);

        blManifestValidationService.validateFreightType(
                dto.getFreight(), dto.getHoldReason(), activeCharges, dto.getOtherCharges());

        blManifestValidationService.validateDemurrageChargeCode(activeCharges);
    }

    private List<ContainerDto> filterActiveContainers(List<ContainerDto> containers) {
        if (containers == null) {
            return List.of();
        }
        return containers.stream()
                .filter(c -> !ACTION_ISDELETED.equals(resolveAction(c.getActionType())))
                .toList();
    }

    private List<ChargeDto> filterActiveCharges(List<ChargeDto> charges) {
        if (charges == null) {
            return List.of();
        }
        return charges.stream()
                .filter(c -> !ACTION_ISDELETED.equals(resolveAction(c.getActionType())))
                .toList();
    }

    private void validateBlManifestDTO(ImportManifestBlDto dto, Long excludePoid) {

        validateVoyageCompany(dto.getVesselVoyagePoid());

        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().isEmpty()) {
            boolean exists = (excludePoid == null)
                    ? headerRepository.existsByVoyageTransactionPoidAndBlNumber(dto.getVesselVoyagePoid(),
                            dto.getBlNumber().trim())
                    : headerRepository.existsByVoyageTransactionPoidAndBlNumberExcludingPoid(dto.getVesselVoyagePoid(),
                            dto.getBlNumber().trim(), excludePoid);

            if (exists) {
                throw new ValidationException(
                        String.format(BlManifestValidationMessages.BL_NUMBER_EXISTS_FOR_VOYAGE,
                                dto.getBlNumber().trim()));
            }
        }

        if (dto.getLoadPortPoid() == null) {
            throw new ValidationException(BlManifestValidationMessages.PORT_OF_LOADING_REQUIRED);
        }

        validateFinancialYear(UserContext.getCompanyPoid(),
                dto.getTransactionDate() != null ? dto.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone());
    }

    private void validateFinancialYear(Long companyPoid, LocalDate transactionDate) {
        if (!validationRepository.isValidFinancialYear(companyPoid, transactionDate)) {
            log.error("Validation failed: Invalid financial year for company {} on date {}", companyPoid,
                    transactionDate);
        }
    }

    private void validateVoyageCompany(Long voyageTransactionPoid) {
        Long voyageCompanyPoid = validationRepository.getVoyageCompanyPoid(voyageTransactionPoid);
        if (voyageCompanyPoid == null || !voyageCompanyPoid.equals(UserContext.getCompanyPoid())) {
            log.error("Validation failed: Voyage company mismatch. Voyage company: {}, User company: {}",
                    voyageCompanyPoid, UserContext.getCompanyPoid());
            throw new ValidationException(BlManifestValidationMessages.VOYAGE_COMPANY_MISMATCH);
        }
    }

    private void logSummaryEntries(List<String> logEntries, String docId, String docKeyPoid) {
        if (logEntries != null) {
            logEntries.forEach(entry -> loggingService.createLogSummaryEntry(docId, docKeyPoid, entry));
        }
    }

    private void autoPopulateDefaults(ShipBlManifestHdr entity) {
        if (entity.getBlType() == null)
            entity.setBlType("IMPORT");
        if (entity.getCargoType() == null)
            entity.setCargoType("FCL-FCL");
        if (entity.getBlIssueType() == null)
            entity.setBlIssueType("1");
        if (entity.getPortOfDischargePoid() == null)
            entity.setPortOfDischargePoid(800L);
        if (entity.getPlaceOfDeliveryPoid() == null)
            entity.setPlaceOfDeliveryPoid(800L);
        if (entity.getHoldReason() == null)
            entity.setHoldReason("5");
        if (entity.getFreightStatus() == null)
            entity.setFreightStatus("1");
        if (entity.getSalesmanPoid() == null || entity.getSalesmanPoid() == 1)
            entity.setSalesmanPoid(51L);
        if (entity.getComodityPoid() == null)
            entity.setComodityPoid(10L);
        if (entity.getBookedByPp() == null)
            entity.setBookedByPp("Y");
    }

    private boolean checkAddressesExist(ImportManifestBlDto dto) {
        if (CollectionUtils.isEmpty(dto.getAddressDetails()))
            return false;

        return dto.getAddressDetails().stream()
                .anyMatch(address -> "Y".equalsIgnoreCase(address.getSendYesNo()));
    }

    private void saveGeneralCargoDetails(List<GeneralCargoDto> details, Long transactionPoid, List<String> logEntries) {
        if (details == null)
            return;

        Long detRowId = getNextDetRowId(generalDtlRepository.getMaxDetRowId(transactionPoid));

        for (GeneralCargoDto detailDto : details) {
            ShipBlManifestGeneralDtl entity = ImportManifestMapper.mapGeneralEntityFromDto(detailDto, transactionPoid,
                    new ShipBlManifestGeneralDtl());
            entity.getId().setDetRowId(detRowId);
            generalDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on General Cargo Detail with DetRowId: %s", detRowId));
            detRowId++;
        }
    }

    private void saveCargoDescriptions(List<DescriptionAndMarksDto> descriptionsAndMarks, Long transactionPoid,
            List<String> logEntries) {
        if (descriptionsAndMarks == null)
            return;

        Long detRowId = getNextDetRowId(cargoDtlRepository.getMaxDetRowId(transactionPoid));

        for (DescriptionAndMarksDto detailDto : descriptionsAndMarks) {
            ShipBlManifestCargoDtl entity = ImportManifestMapper.mapCargoDescriptionAndMarksFromDto(detailDto,
                    transactionPoid, new ShipBlManifestCargoDtl());
            entity.getId().setDetRowId(detRowId);
            cargoDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on Cargo Description Detail with DetRowId: %s", detRowId));
            detRowId++;
        }
    }

    private List<DescriptionAndMarksDto> buildSimpleCargoDescriptions(ImportManifestBlDto dto) {
        List<DescriptionAndMarksDto> details = new ArrayList<>();
        if (dto.getSimpleCargoDescription() != null) {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_DESCRIPTION)
                    .cargoDescription(dto.getSimpleCargoDescription())
                    .build());
        }
        if (dto.getSimpleCargoMarks() != null) {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_MARKS)
                    .cargoDescription(dto.getSimpleCargoMarks())
                    .build());
        }
        if (details.isEmpty() && dto.getDescriptionsAndMarks() != null) {
            details.addAll(dto.getDescriptionsAndMarks());
        }
        return details;
    }

    private List<DescriptionAndMarksDto> buildSimpleCargoDescriptionsForUpdate(ImportManifestBlDto dto) {
        // During update, if descriptionsAndMarks is explicitly provided, use it directly
        if (dto.getDescriptionsAndMarks() != null && !dto.getDescriptionsAndMarks().isEmpty()) {
            return new ArrayList<>(dto.getDescriptionsAndMarks());
        }
        // Build from simple fields; null value = delete existing rows of that type
        List<DescriptionAndMarksDto> details = new ArrayList<>();
        if (dto.getSimpleCargoDescription() != null) {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_DESCRIPTION)
                    .cargoDescription(dto.getSimpleCargoDescription())
                    .actionType(ACTION_ISUPDATED)
                    .build());
        } else {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_DESCRIPTION)
                    .actionType(ACTION_ISDELETED)
                    .build());
        }
        if (dto.getSimpleCargoMarks() != null) {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_MARKS)
                    .cargoDescription(dto.getSimpleCargoMarks())
                    .actionType(ACTION_ISUPDATED)
                    .build());
        } else {
            details.add(DescriptionAndMarksDto.builder()
                    .descriptionType(CARGO_TYPE_MARKS)
                    .actionType(ACTION_ISDELETED)
                    .build());
        }
        return details;
    }

    private void saveContainers(List<ContainerDto> containers, Long transactionPoid, List<String> logEntries) {
        if (containers == null)
            return;

        Long detRowId = getNextDetRowId(containerDtlRepository.getMaxDetRowId(transactionPoid));

        for (ContainerDto detailDto : containers) {
            ShipBlManifestContainerDtl entity = ImportManifestMapper.mapContainerEntityFromDto(detailDto,
                    transactionPoid, new ShipBlManifestContainerDtl());
            entity.getId().setDetRowId(detRowId);
            containerDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on Container Detail with DetRowId: %s", detRowId));
            detRowId++;
        }
    }

    private void saveOtherChargeDetails(List<ChargeOtherDto> chargeDetails, Long transactionPoid, List<String> logEntries) {
        if (CollectionUtils.isEmpty(chargeDetails))
            return;

        Long nextDetRowId = getNextDetRowId(chargesDtlRepository.getMaxDetRowId(transactionPoid));

        for (ChargeOtherDto detailDto : chargeDetails) {
            Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

            ShipBlManifestChargesDtl entity = ImportManifestMapper.mapChargesDtlFromDto(detailDto, transactionPoid,
                    new ShipBlManifestChargesDtl());

            if (entity.getId() == null) {
                entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
            } else {
                entity.getId().setDetRowId(detRowId);
            }

            chargesDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on Other Charge Detail with DetRowId: %s", detRowId));
        }
    }

    private void saveChargeDetails(List<ChargeDto> chargeDetails, Long transactionPoid, List<String> logEntries) {
        if (CollectionUtils.isEmpty(chargeDetails))
            return;

        Long nextDetRowId = getNextDetRowId(chargesDtlRepository.getMaxDetRowId(transactionPoid));

        for (ChargeDto detailDto : chargeDetails) {
            Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

            populateMissingChargeInfo(detailDto);

            ShipBlManifestChargesDtl entity = ImportManifestMapper.mapChargesDtlFromDto(detailDto, transactionPoid,
                    new ShipBlManifestChargesDtl());

            if (entity.getId() == null) {
                entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
            } else {
                entity.getId().setDetRowId(detRowId);
            }

            chargesDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on Charge Detail with DetRowId: %s", detRowId));
        }
    }

    private void savePartBls(List<PartBlDto> partBls, Long transactionPoid, List<String> logEntries) {
        if (CollectionUtils.isEmpty(partBls))
            return;

        Long nextDetRowId = getNextDetRowId(containerPrtRepository.getMaxDetRowId(transactionPoid));

        for (PartBlDto detailDto : partBls) {
            Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

            ShipBlManifestPartBL entity = ImportManifestMapper.mapPartBlEntityFromDto(detailDto, transactionPoid,
                    new ShipBlManifestPartBL());

            if (entity.getId() == null) {
                entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
            } else {
                entity.getId().setDetRowId(detRowId);
            }

            containerPrtRepository.save(entity);
            logEntries.add(String.format("Row Created on Part BL Detail with DetRowId: %s", detRowId));
        }
    }

    private void saveNotifyParties(List<AddressDetailsDto> notifyParties,
            Long transactionPoid, List<String> logEntries, ImportManifestBlDto headerDto) {
        if (CollectionUtils.isEmpty(notifyParties))
            return;

        Long nextDetRowId = getNextDetRowId(emailFaxDtlRepository.getMaxDetRowId(transactionPoid));

        for (AddressDetailsDto detailDto : notifyParties) {
            Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

            populateMissingAddressInfo(detailDto, headerDto);

            ShipBlManifestEmailFaxDtl entity = ImportManifestMapper.mapEmailFaxEntityFromDto(detailDto,
                    transactionPoid, new ShipBlManifestEmailFaxDtl());

            if (entity.getId() == null) {
                entity.setId(new ShipBlManifestEmailFaxId(transactionPoid, detRowId, detailDto.getAddressType()));
            } else {
                entity.getId().setDetRowId(detRowId);
            }

            emailFaxDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on Notify Party Detail with DetRowId: %s", detRowId));
        }
    }

    private void saveMafiDetails(List<MafiDetailsDto> mafiDetails, Long transactionPoid, List<String> logEntries) {
        if (CollectionUtils.isEmpty(mafiDetails))
            return;

        Long nextDetRowId = getNextDetRowId(mafiDtlRepository.getMaxDetRowId(transactionPoid));

        for (MafiDetailsDto detailDto : mafiDetails) {
            Long detRowId = detailDto.getDetRowId() != null ? detailDto.getDetRowId() : nextDetRowId++;

            ShipBlManifestMafiDtl entity = ImportManifestMapper.mapMafiEntityFromDto(detailDto, transactionPoid,
                    new ShipBlManifestMafiDtl());

            if (entity.getId() == null) {
                entity.setId(new ShipBlManifestDtlId(transactionPoid, detRowId));
            } else {
                entity.getId().setDetRowId(detRowId);
            }

            mafiDtlRepository.save(entity);
            logEntries.add(String.format("Row Created on MAFI Detail with DetRowId: %s", detRowId));
        }
    }

    private boolean hasAnyEdiChange(ImportManifestBlDto dto) {
        return dto.getShipperName() != null
                || dto.getConsigneeName() != null
                || dto.getNotifyName() != null
                || (dto.getOtherNotifies() != null && dto.getOtherNotifies().getNotify2EdiName() != null)
                || (dto.getOtherNotifies() != null && dto.getOtherNotifies().getNotify3EdiName() != null);
    }

    private Long getNextDetRowId(Long maxDetRowId) {
        return (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
    }

    private void updateDetailTables(ImportManifestBlDto dto, Long transactionPoid) {
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        updateGeneralCargo(dto.getGeneralCargoDetails(), transactionPoid, docId, docKeyPoid);
        updateCargoDescriptions(buildSimpleCargoDescriptionsForUpdate(dto), transactionPoid, docId, docKeyPoid);
        updateContainers(dto.getContainers(), transactionPoid, docId, docKeyPoid);
        updateCharges(dto.getCharges(), transactionPoid, docId, docKeyPoid);
        updateOtherCharges(dto.getOtherCharges(), transactionPoid, docId, docKeyPoid);
        updatePartBls(dto.getPartBls(), transactionPoid, docId, docKeyPoid);
        updateNotifyParties(dto.getAddressDetails(), transactionPoid, docId, docKeyPoid, dto);
        updateMafiDetails(dto.getMafiDetails(), transactionPoid, docId, docKeyPoid);
    }

    private void updateGeneralCargo(List<GeneralCargoDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<ShipBlManifestGeneralDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestGeneralDtl>> logRequests = new ArrayList<>();

        for (GeneralCargoDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                case ACTION_ISCREATED -> saveGeneralCargoDetails(List.of(detailDto), transactionPoid, logEntries);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestGeneralDtl existing = generalDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("General Cargo Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestGeneralDtl oldEntity = new ShipBlManifestGeneralDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    ImportManifestMapper.mapGeneralEntityFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestGeneralDtl.class, docId, docKeyPoid, logDetail));
                }
                default -> {}
            }
        }
        processUpdates(generalDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);
        if (!toDelete.isEmpty()) {
            List<ShipBlManifestGeneralDtl> entitiesToDelete = generalDtlRepository.findAllById(toDelete);
            generalDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateCargoDescriptions(List<DescriptionAndMarksDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<ShipBlManifestCargoDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestCargoDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestCargoDtl>> logRequests = new ArrayList<>();

        for (DescriptionAndMarksDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestCargoDtlId(transactionPoid, detailDto.getDetRowId(), detailDto.getDescriptionType()));
                    } else if (detailDto.getDescriptionType() != null) {
                        // delete all rows of this type (used when simple cargo fields are cleared)
                        cargoDtlRepository.deleteByIdTransactionPoidAndIdDescriptionType(transactionPoid, detailDto.getDescriptionType());
                    }
                }
                case ACTION_ISCREATED -> {
                    // Guard against duplicate creates: if rows of this type already exist, update instead
                    List<ShipBlManifestCargoDtl> existingRows = cargoDtlRepository
                            .findByIdTransactionPoidAndIdDescriptionTypeOrderByIdDetRowId(transactionPoid, detailDto.getDescriptionType());
                    if (existingRows.isEmpty()) {
                        saveCargoDescriptions(List.of(detailDto), transactionPoid, logEntries);
                    } else {
                        ShipBlManifestCargoDtl first = existingRows.get(0);
                        ShipBlManifestCargoDtl oldEntity = new ShipBlManifestCargoDtl();
                        BeanUtils.copyProperties(first, oldEntity);
                        first.setCargoDescription(detailDto.getCargoDescription());
                        toUpdate.add(first);
                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s TYPE: %s", transactionPoid, first.getId().getDetRowId(), detailDto.getDescriptionType());
                        logRequests.add(new LogRequestDto<>(oldEntity, first, ShipBlManifestCargoDtl.class, docId, docKeyPoid, logDetail));
                    }
                }
                case ACTION_ISUPDATED -> {
                    // find existing row(s) by type when detRowId is not known
                    if (detailDto.getDetRowId() == null) {
                        List<ShipBlManifestCargoDtl> existing = cargoDtlRepository
                                .findByIdTransactionPoidAndIdDescriptionTypeOrderByIdDetRowId(transactionPoid, detailDto.getDescriptionType());
                        if (existing.isEmpty()) {
                            saveCargoDescriptions(List.of(detailDto), transactionPoid, logEntries);
                        } else {
                            ShipBlManifestCargoDtl first = existing.get(0);
                            ShipBlManifestCargoDtl oldEntity = new ShipBlManifestCargoDtl();
                            BeanUtils.copyProperties(first, oldEntity);
                            first.setCargoDescription(detailDto.getCargoDescription());
                            toUpdate.add(first);
                            String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s TYPE: %s", transactionPoid, first.getId().getDetRowId(), detailDto.getDescriptionType());
                            logRequests.add(new LogRequestDto<>(oldEntity, first, ShipBlManifestCargoDtl.class, docId, docKeyPoid, logDetail));
                        }
                    } else {
                        ShipBlManifestCargoDtlId id = new ShipBlManifestCargoDtlId(transactionPoid, detailDto.getDetRowId(), detailDto.getDescriptionType());
                        ShipBlManifestCargoDtl existing = cargoDtlRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Cargo Description Detail", "detRowId", detailDto.getDetRowId()));
                        ShipBlManifestCargoDtl oldEntity = new ShipBlManifestCargoDtl();
                        BeanUtils.copyProperties(existing, oldEntity);
                        ImportManifestMapper.mapCargoDescriptionAndMarksFromDto(detailDto, transactionPoid, existing);
                        toUpdate.add(existing);
                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s TYPE: %s", transactionPoid, detailDto.getDetRowId(), detailDto.getDescriptionType());
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestCargoDtl.class, docId, docKeyPoid, logDetail));
                    }
                }
                default -> {}
            }
        }
        processUpdates(cargoDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestCargoDtl> entitiesToDelete = cargoDtlRepository.findAllById(toDelete);
            cargoDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateContainers(List<ContainerDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<ShipBlManifestContainerDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestContainerDtl>> logRequests = new ArrayList<>();
        List<String> logEntries = new ArrayList<>();

        for (ContainerDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> saveContainers(List.of(detailDto), transactionPoid, logEntries);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestContainerDtl existing = containerDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Container Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestContainerDtl oldEntity = new ShipBlManifestContainerDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    ImportManifestMapper.mapContainerEntityFromDto(detailDto, transactionPoid, existing);

                    if (existing.getContainerNo() != null && !existing.getContainerNo().trim().isEmpty()) {
                        containerDtlRepository.findByIdTransactionPoidAndContainerNo(transactionPoid, existing.getContainerNo().trim())
                                .ifPresent(conflict -> {
                                    if (!conflict.getId().getDetRowId().equals(existing.getId().getDetRowId())) {
                                        throw new ValidationException(String.format(BlManifestValidationMessages.CONTAINER_DUPLICATE, existing.getContainerNo()));
                                    }
                                });
                    }
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestContainerDtl.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                default -> {}
            }
        }
        processUpdates(containerDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestContainerDtl> entitiesToDelete = containerDtlRepository.findAllById(toDelete);
            containerDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateOtherCharges(List<ChargeOtherDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<ShipBlManifestChargesDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestChargesDtl>> logRequests = new ArrayList<>();

        for (ChargeOtherDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> saveOtherChargeDetails(List.of(detailDto), transactionPoid, logEntries);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestChargesDtl existing = chargesDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Other Charge Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestChargesDtl oldEntity = new ShipBlManifestChargesDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    ImportManifestMapper.mapChargesDtlFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestChargesDtl.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                default -> {}
            }
        }
        processUpdates(chargesDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestChargesDtl> entitiesToDelete = chargesDtlRepository.findAllById(toDelete);
            chargesDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateCharges(List<ChargeDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<ShipBlManifestChargesDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestChargesDtl>> logRequests = new ArrayList<>();

        for (ChargeDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> saveChargeDetails(List.of(detailDto), transactionPoid, logEntries);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestChargesDtl existing = chargesDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestChargesDtl oldEntity = new ShipBlManifestChargesDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    populateMissingChargeInfo(detailDto);

                    ImportManifestMapper.mapChargesDtlFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestChargesDtl.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                default -> {}
            }
        }
        processUpdates(chargesDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestChargesDtl> entitiesToDelete = chargesDtlRepository.findAllById(toDelete);
            chargesDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updatePartBls(List<PartBlDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<PartBlDto> toCreate = new ArrayList<>();
        List<ShipBlManifestPartBL> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestPartBL>> logRequests = new ArrayList<>();

        for (PartBlDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> toCreate.add(detailDto);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestPartBL existing = containerPrtRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Part BL Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestPartBL oldEntity = new ShipBlManifestPartBL();
                    BeanUtils.copyProperties(existing, oldEntity);
                    ImportManifestMapper.mapPartBlEntityFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestPartBL.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                default -> {}
            }
        }
        savePartBls(toCreate, transactionPoid, logEntries);
        processUpdates(containerPrtRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestPartBL> entitiesToDelete = containerPrtRepository.findAllById(toDelete);
            containerPrtRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateNotifyParties(List<AddressDetailsDto> details, Long transactionPoid, String docId, String docKeyPoid, ImportManifestBlDto headerDto) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<AddressDetailsDto> toCreate = new ArrayList<>();
        List<ShipBlManifestEmailFaxDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestEmailFaxId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestEmailFaxDtl>> logRequests = new ArrayList<>();

        for (AddressDetailsDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> toCreate.add(detailDto);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestEmailFaxDtl existing = emailFaxDtlRepository.findById(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(), detailDto.getAddressType()))
                            .orElseThrow(() -> new ResourceNotFoundException("Notify Party Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestEmailFaxDtl oldEntity = new ShipBlManifestEmailFaxDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    populateMissingAddressInfo(detailDto, headerDto);

                    ImportManifestMapper.mapEmailFaxEntityFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestEmailFaxDtl.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestEmailFaxId(transactionPoid, detailDto.getDetRowId(), detailDto.getAddressType()));
                    }
                }
                default -> {}
            }
        }
        saveNotifyParties(toCreate, transactionPoid, logEntries, headerDto);
        processUpdates(emailFaxDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestEmailFaxDtl> entitiesToDelete = emailFaxDtlRepository.findAllById(toDelete);
            emailFaxDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void updateMafiDetails(List<MafiDetailsDto> details, Long transactionPoid, String docId, String docKeyPoid) {
        if (details == null) return;

        List<String> logEntries = new ArrayList<>();
        List<ShipBlManifestMafiDtl> toUpdate = new ArrayList<>();
        List<ShipBlManifestDtlId> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipBlManifestMafiDtl>> logRequests = new ArrayList<>();

        for (MafiDetailsDto detailDto : details) {
            String action = resolveAction(detailDto.getActionType());
            switch (action) {
                case ACTION_ISCREATED -> saveMafiDetails(List.of(detailDto), transactionPoid, logEntries);
                case ACTION_ISUPDATED -> {
                    ShipBlManifestMafiDtl existing = mafiDtlRepository.findById(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException("MAFI Detail", "detRowId", detailDto.getDetRowId()));
                    ShipBlManifestMafiDtl oldEntity = new ShipBlManifestMafiDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    ImportManifestMapper.mapMafiEntityFromDto(detailDto, transactionPoid, existing);
                    toUpdate.add(existing);
                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipBlManifestMafiDtl.class, docId, docKeyPoid, logDetail));
                }
                case ACTION_ISDELETED -> {
                    if (detailDto.getDetRowId() != null) {
                        toDelete.add(new ShipBlManifestDtlId(transactionPoid, detailDto.getDetRowId()));
                    }
                }
                default -> {}
            }
        }
        processUpdates(mafiDtlRepository, toUpdate, logRequests);
        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipBlManifestMafiDtl> entitiesToDelete = mafiDtlRepository.findAllById(toDelete);
            mafiDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private String getCargoDescriptionByType(List<CargoDescriptionRequestDto> cargoDescriptions, String... types) {
        if (cargoDescriptions == null) {
            return null;
        }
        return cargoDescriptions.stream()
                .filter(dto -> {
                    if (dto.getDescriptionType() == null) {
                        return false;
                    }
                    for (String type : types) {
                        if (type.equalsIgnoreCase(dto.getDescriptionType())) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(CargoDescriptionRequestDto::getCargoDescription)
                .findFirst()
                .orElse(null);
    }

    private <T, ID> void processUpdates(JpaRepository<T, ID> repository, List<T> entities, List<LogRequestDto<T>> logRequests) {
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private String resolveAction(String rawAction) {
        String action = (rawAction == null || rawAction.trim().isEmpty()) ? ACTION_NOCHANGES : rawAction.trim().toUpperCase();
        return switch (action) {
            case ACTION_ISCREATED, "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case ACTION_ISUPDATED, "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
            case ACTION_ISDELETED, "ISDELETED", "DELETED" -> ACTION_ISDELETED;
            default -> ACTION_NOCHANGES;
        };
    }

    private void populateMissingChargeInfo(ChargeDto dto) {
        if (dto.getChargePoid() != null && (dto.getChargeType() == null || dto.getChargeType().trim().isEmpty())) {
            chargeMasterRepository.findByChargePoid(dto.getChargePoid()).ifPresent(master -> {
                dto.setChargeType(master.getChargeType());
                if (dto.getChargeDescription() == null || dto.getChargeDescription().trim().isEmpty()) {
                    dto.setChargeDescription(master.getChargeName());
                }
            });
        }
    }

    private static BigDecimal toBigDecimal(Long value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private void populateMissingAddressInfo(AddressDetailsDto detailDto, ImportManifestBlDto headerDto) {
        if (detailDto.getAddressPoid() == null) {
            String type = detailDto.getAddressType();
            if ("CONSIGNEE".equalsIgnoreCase(type)) {
                detailDto.setAddressPoid(toBigDecimal(headerDto.getConsigneePoid()));
            } else if ("NOTIFY1".equalsIgnoreCase(type) || "N1".equalsIgnoreCase(type)) {
                detailDto.setAddressPoid(toBigDecimal(headerDto.getNotify1Poid()));
            } else if (("NOTIFY2".equalsIgnoreCase(type) || "N2".equalsIgnoreCase(type)) && headerDto.getOtherNotifies() != null) {
                detailDto.setAddressPoid(toBigDecimal(headerDto.getOtherNotifies().getNotify2Poid()));
            } else if (("NOTIFY3".equalsIgnoreCase(type) || "N3".equalsIgnoreCase(type)) && headerDto.getOtherNotifies() != null) {
                detailDto.setAddressPoid(toBigDecimal(headerDto.getOtherNotifies().getNotify3Poid()));
            }
        }
    }


    @Override
    public List<ConsigneeEmailDto> getConsigneeEmails(Long transactionPoId) {
        List<ShipBlManifestEmailFaxDtl> rows = emailFaxDtlRepository
                .findByIdTransactionPoidOrderByIdDetRowId(transactionPoId);

        List<String> addressPoidIds = rows.stream()
                .map(ShipBlManifestEmailFaxDtl::getAddressPoid)
                .filter(Objects::nonNull)
                .map(p -> p.stripTrailingZeros().toPlainString())
                .distinct()
                .collect(Collectors.toList());

        Map<String, AddressDetails> masterByAddressPoid = new HashMap<>();
        if (!addressPoidIds.isEmpty()) {
            addressDetailsRepository.findAllById(addressPoidIds)
                    .forEach(ad -> masterByAddressPoid.put(ad.getAddressPoid(), ad));
        }

        List<ConsigneeEmailDto> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ShipBlManifestEmailFaxDtl r = rows.get(i);
            AddressDetails master = r.getAddressPoid() != null
                    ? masterByAddressPoid.get(r.getAddressPoid().stripTrailingZeros().toPlainString())
                    : null;
            boolean fromMaster = master != null
                    && normalizeEmail(r.getEmail1()).equals(normalizeEmail(master.getEmail()))
                    && normalizeEmail(r.getEmail2()).equals(normalizeEmail(master.getEmail2()));

            result.add(ConsigneeEmailDto.builder()
                    .sn((long) (i + 1))
                    .send("Y".equalsIgnoreCase(r.getSendYesNo()))
                    .select(false)
                    .detRowId(r.getId().getDetRowId())
                    .addressPoid(r.getAddressPoid())
                    .email1(r.getEmail1())
                    .email2(r.getEmail2())
                    .addressType(r.getId().getAddressType())
                    .sendYesNo(r.getSendYesNo())
                    .sendEmailFax(r.getSendEmailFax())
                    .actionType(null)
                    .fromMaster(fromMaster)
                    .build());
        }
        return result;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Override
    public ChargeDefaultsResponseDto getChargeDefaults(ChargeDefaultsRequestDto request) {
        log.info("Fetching charge defaults for chargePoid: {}", request.getChargePoid());

        Long companyPoid = UserContext.getCompanyPoid();
        Object[] taxData = procRepository.getTaxRate(request.getChargePoid(), companyPoid, request.getTransactionDate());
        Long taxPoid = (Long) taxData[0];
        BigDecimal taxPercentage = (BigDecimal) taxData[1];

        ChargeDefaultsResponseDto.ChargeDefaultsResponseDtoBuilder builder = ChargeDefaultsResponseDto.builder()
                .taxPoid(taxPoid)
                .taxPercentage(taxPercentage != null ? taxPercentage : BigDecimal.ZERO);

        if (taxPoid != null) {
            try {
                builder.taxDet(lovService.getLovItemByPoid(taxPoid, "TAX_MASTER",
                        UserContext.getGroupPoid(), companyPoid, UserContext.getUserPoid()));
            } catch (Exception e) {
                log.warn("Failed to fetch TAX_MASTER LOV for taxPoid: {}", taxPoid, e);
            }
        }

        return builder.build();
    }
}
