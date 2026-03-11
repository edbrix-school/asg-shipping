package com.asg.shipping.salesinvoice.util;

import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.salesinvoice.dto.*;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceChargDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceContnrDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceHdr;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

public class SalesInvoiceMapper {

    /**
     * Map Create DTO to Entity
     */
    public static void mapCreateDTOToEntity(SalesInvoiceShippingCreateDTO dto, ArShSalesInvoiceHdr entity, Long groupPoid, Long companyPoid) {
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : DateUtil.getCurrentDateInUserTimeZone());
        entity.setInvDate(dto.getInvDate());
        entity.setJobnoPoid(dto.getJobnoPoid());
        entity.setCustomerAddrPoid(dto.getCustomerAddrPoid());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setInvoiceAgainst(dto.getInvoiceAgainst());
        entity.setLpoSrnNo(dto.getLpoSrnNo());
        entity.setLpoSrnDate(dto.getLpoSrnDate());
        entity.setAuthorizedId(dto.getAuthorizedId());
        entity.setZeroValueInvoice(dto.getZeroValueInvoice() != null ? dto.getZeroValueInvoice() : "N");
        entity.setInvoiceType(dto.getInvoiceType() != null ? dto.getInvoiceType() : "MANUAL");
        entity.setPrintCustomerPoid(dto.getPrintCustomerPoid());
        entity.setBlReleaseTypeOffice(dto.getBlReleaseTypeOffice());
        entity.setCcRef(dto.getCcRef());
        entity.setPrintInvoiceBankPoid(dto.getPrintInvoiceBankPoid());
        entity.setBookingPartyPoid(dto.getBookingPartyPoid());
        entity.setOwnInvoiceNo(dto.getOwnInvoiceNo() != null ? dto.getOwnInvoiceNo() : "Y");
        entity.setInvoiceTo(dto.getInvoiceTo() != null ? dto.getInvoiceTo() : "C");
        entity.setInvoiceDeliveryDate(dto.getInvoiceDeliveryDate());
        entity.setCreditDays(dto.getCreditDays() != null ? dto.getCreditDays() : 0);
        entity.setCurrencyRate(dto.getCurrencyRate() != null ? dto.getCurrencyRate() : BigDecimal.ONE);
        entity.setBlPoid(dto.getBlPoid() != null ? dto.getBlPoid() : 0L);
        entity.setCustomerPoid(dto.getCustomerPoid() != null ? dto.getCustomerPoid() : 0L);
    }

    /**
     * Map Update DTO to Entity
     */
    public static void mapUpdateDTOToEntity(SalesInvoiceShippingUpdateDTO dto, ArShSalesInvoiceHdr entity) {
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getInvDate() != null) {
            entity.setInvDate(dto.getInvDate());
        }
        if (dto.getJobnoPoid() != null) {
            entity.setJobnoPoid(dto.getJobnoPoid());
        }
        if (dto.getCustomerPoid() != null) {
            entity.setCustomerPoid(dto.getCustomerPoid());
        }
        if (dto.getCustomerAddrPoid() != null) {
            entity.setCustomerAddrPoid(dto.getCustomerAddrPoid());
        }
        if (dto.getCurrencyCode() != null) {
            entity.setCurrencyCode(dto.getCurrencyCode());
        }
        if (dto.getCurrencyRate() != null) {
            entity.setCurrencyRate(dto.getCurrencyRate());
        }
        if (dto.getCreditDays() != null) {
            entity.setCreditDays(dto.getCreditDays());
        }
        if (dto.getInvoiceAgainst() != null) {
            entity.setInvoiceAgainst(dto.getInvoiceAgainst());
        }
        if (dto.getLpoSrnNo() != null) {
            entity.setLpoSrnNo(dto.getLpoSrnNo());
        }
        if (dto.getLpoSrnDate() != null) {
            entity.setLpoSrnDate(dto.getLpoSrnDate());
        }
        if (dto.getAuthorizedId() != null) {
            entity.setAuthorizedId(dto.getAuthorizedId());
        }
        if (dto.getZeroValueInvoice() != null) {
            entity.setZeroValueInvoice(dto.getZeroValueInvoice());
        }
        if (dto.getInvoiceType() != null) {
            entity.setInvoiceType(dto.getInvoiceType());
        }
        if (dto.getPrintCustomerPoid() != null) {
            entity.setPrintCustomerPoid(dto.getPrintCustomerPoid());
        }
        if (dto.getBlReleaseTypeOffice() != null) {
            entity.setBlReleaseTypeOffice(dto.getBlReleaseTypeOffice());
        }
        if (dto.getCcRef() != null) {
            entity.setCcRef(dto.getCcRef());
        }
        if (dto.getPrintInvoiceBankPoid() != null) {
            entity.setPrintInvoiceBankPoid(dto.getPrintInvoiceBankPoid());
        }
        if (dto.getBookingPartyPoid() != null) {
            entity.setBookingPartyPoid(dto.getBookingPartyPoid());
        }
        if (dto.getOwnInvoiceNo() != null) {
            entity.setOwnInvoiceNo(dto.getOwnInvoiceNo());
        }
        if (dto.getInvoiceTo() != null) {
            entity.setInvoiceTo(dto.getInvoiceTo());
        }
        if (dto.getInvoiceDeliveryDate() != null) {
            entity.setInvoiceDeliveryDate(dto.getInvoiceDeliveryDate());
        }
    }

    /**
     * Map entity to DTO
     */
    public static SalesInvoiceShippingDto mapToDto(ArShSalesInvoiceHdr entity) {
        return SalesInvoiceShippingDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate())
                .invDate(entity.getInvDate())
                .jobnoPoid(entity.getJobnoPoid())
                .customerPoid(entity.getCustomerPoid())
                .customerAddrPoid(entity.getCustomerAddrPoid())
                .currencyCode(entity.getCurrencyCode())
                .invAmount(entity.getInvAmount())
                .creditDays(entity.getCreditDays())
                .dueDate(entity.getDueDate())
                .relasedIdPerson(entity.getRelasedIdPerson())
                .relasedToPerson(entity.getRelasedToPerson())
                .relasedAddrsPerson(entity.getRelasedAddrsPerson())
                .blPoid(entity.getBlPoid())
                .printCustomerPoid(entity.getPrintCustomerPoid())
                .blReleaseTypeOffice(entity.getBlReleaseTypeOffice())
                .orignalBlReleaseType(entity.getOrignalBlReleaseType())
                .blTypeInvoice(entity.getBlTypeInvoice())
                .lpoSrnNo(entity.getLpoSrnNo())
                .lpoSrnDate(entity.getLpoSrnDate())
                .currencyRate(entity.getCurrencyRate())
                .invoiceAgainst(entity.getInvoiceAgainst())
                .ffJobNo(entity.getFfJobNo())
                .ffPjNo(entity.getFfPjNo())
                .bookingPartyPoid(entity.getBookingPartyPoid())
                .ownInvoiceNo(entity.getOwnInvoiceNo())
                .invoiceTo(entity.getInvoiceTo())
                .invoiceType(entity.getInvoiceType())
                .authorizedId(entity.getAuthorizedId())
                .zeroValueInvoice(entity.getZeroValueInvoice())
                .ccRef(entity.getCcRef())
                .printInvoiceBankPoid(entity.getPrintInvoiceBankPoid())
                .tinNumber(entity.getTinNumber())
                .verifiedByAccount(entity.getVerifiedByAccount())
                .changePosting(entity.getChangePosting())
                .emailSent(entity.getEmailSent())
                .reportGenerated(entity.getReportGenerated())
                .invoiceDeliveryDate(entity.getInvoiceDeliveryDate())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Map Container Detail Entity to DTO
     */
    public static SalesInvoiceContainerDtlDto mapContainerDtlToDto(ArShSalesInvoiceContnrDtl entity) {
        return SalesInvoiceContainerDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .blPoid(entity.getBlPoid())
                .containerSocYn(entity.getContainerSocYn())
                .containerNo(entity.getContainerNo())
                .dmFrmDate(entity.getDmFrmDate())
                .dmToDate(entity.getDmToDate())
                .dmDays(entity.getDmDays())
                .dmChargeAmt(entity.getDmChargeAmt())
                .freeDays(entity.getFreeDays())
                .equipmentIsoType(entity.getEquipmentIsoType())
                .dlvFormPrinted(entity.getDlvFormPrinted())
                .rtnFormPrinted(entity.getRtnFormPrinted())
                .emptyIn(entity.getEmptyIn())
                .cntTaxPoid(entity.getCntTaxPoid())
                .cntTaxPercentage(entity.getCntTaxPercentage())
                .cntTaxAmount(entity.getCntTaxAmount())
                .build();
    }

    /**
     * Map Container Detail DTO to Entity
     */
    public static ArShSalesInvoiceContnrDtl mapContainerDtlFromDto(SalesInvoiceContainerDtlDto dto, Long transactionPoid) {
        return ArShSalesInvoiceContnrDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(dto.getDetRowId())
                .blPoid(dto.getBlPoid())
                .containerSocYn(dto.getContainerSocYn())
                .containerNo(dto.getContainerNo())
                .dmFrmDate(dto.getDmFrmDate())
                .dmToDate(dto.getDmToDate())
                .dmDays(dto.getDmDays())
                .dmChargeAmt(dto.getDmChargeAmt())
                .freeDays(dto.getFreeDays())
                .equipmentIsoType(dto.getEquipmentIsoType())
                .dlvFormPrinted(dto.getDlvFormPrinted() != null ? dto.getDlvFormPrinted() : "N")
                .rtnFormPrinted(dto.getRtnFormPrinted() != null ? dto.getRtnFormPrinted() : "N")
                .emptyIn(dto.getEmptyIn())
                .cntTaxPoid(dto.getCntTaxPoid())
                .cntTaxPercentage(dto.getCntTaxPercentage())
                .cntTaxAmount(dto.getCntTaxAmount())
                .build();
    }

    /**
     * Map Charges Detail Entity to DTO
     */
    public static SalesInvoiceChargesDtlDto mapChargesDtlToDto(ArShSalesInvoiceChargDtl entity) {
        return SalesInvoiceChargesDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .blPoid(entity.getBlPoid())
                .chargesDetRowId(entity.getChargesDetRowId())
                .chargePoid(entity.getChargePoid())
                .amount(entity.getAmount())
                .amountSelect(entity.getAmountSelect())
                .buyAmount(entity.getBuyAmount())
                .currencyCode(entity.getCurrencyCode())
                .currencyExchange(entity.getCurrencyExchange())
                .quantity(entity.getQuantity())
                .lpoSrnDate(entity.getLpoSrnDate())
                .lpoSrnNo(entity.getLpoSrnNo())
                .perQtyBuyAmt(entity.getPerQtyBuyAmt())
                .perQtySellAmt(entity.getPerQtySellAmt())
                .printGroupTemp(entity.getPrintGroupTemp())
                .chargeType(entity.getChargeType())
                .chargeNewRecord(entity.getChargeNewRecord())
                .taxPoid(entity.getTaxPoid())
                .taxPercentage(entity.getTaxPercentage())
                .taxAmount(entity.getTaxAmount())
                .cnRefDocId(entity.getCnRefDocId())
                .cnRefDocPoid(entity.getCnRefDocPoid())
                .cnRefDetRowId(entity.getCnRefDetRowId())
                .printCurrencyCode(entity.getPrintCurrencyCode())
                .printCurrencyExchange(entity.getPrintCurrencyExchange())
                .printRateAmt(entity.getPrintRateAmt())
                .build();
    }

    /**
     * Map Charges Detail DTO to Entity
     */
    public static ArShSalesInvoiceChargDtl mapChargesDtlFromDto(SalesInvoiceChargesDtlDto dto, Long transactionPoid) {
        return ArShSalesInvoiceChargDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(dto.getDetRowId())
                .blPoid(dto.getBlPoid())
                .chargesDetRowId(dto.getChargesDetRowId())
                .chargePoid(dto.getChargePoid())
                .amount(dto.getAmount())
                .amountSelect(dto.getAmountSelect())
                .buyAmount(dto.getBuyAmount())
                .currencyCode(dto.getCurrencyCode())
                .currencyExchange(dto.getCurrencyExchange())
                .quantity(dto.getQuantity())
                .lpoSrnDate(dto.getLpoSrnDate())
                .lpoSrnNo(dto.getLpoSrnNo())
                .perQtyBuyAmt(dto.getPerQtyBuyAmt())
                .perQtySellAmt(dto.getPerQtySellAmt())
                .printGroupTemp(dto.getPrintGroupTemp())
                .chargeType(dto.getChargeType())
                .chargeNewRecord(dto.getChargeNewRecord() != null ? dto.getChargeNewRecord() : "N")
                .taxPoid(dto.getTaxPoid())
                .taxPercentage(dto.getTaxPercentage())
                .taxAmount(dto.getTaxAmount())
                .cnRefDocId(dto.getCnRefDocId())
                .cnRefDocPoid(dto.getCnRefDocPoid())
                .cnRefDetRowId(dto.getCnRefDetRowId())
                .printCurrencyCode(dto.getPrintCurrencyCode())
                .printCurrencyExchange(dto.getPrintCurrencyExchange())
                .printRateAmt(dto.getPrintRateAmt())
                .build();
    }
}

