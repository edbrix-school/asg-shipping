package com.asg.shipping.receipts.util;

import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.receipts.dto.ReceiptCharges;
import com.asg.shipping.receipts.dto.ReceiptContainerDto;
import com.asg.shipping.receipts.dto.ReceiptPaymentDetailDto;
import com.asg.shipping.receipts.dto.ReceiptsBlDetailsDto;
import com.asg.shipping.receipts.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReceiptsMapper {


	public ReceiptContainerDto mapContainerDtlToDto(ArShReceiptContainerDtl entity) {
		if (entity == null) return null;
		
		return ReceiptContainerDto.builder()
				.detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
				.containerSocYn(entity.getContainerSocYn())
				.blPoid(entity.getBlPoid())
				.containerNo(entity.getContainerNo())
				.equipmentIsoType(entity.getEquipmentIsoType())
				.freeDays(entity.getFreeDays())
				.dmFrmDate(entity.getDmFrmDate())
				.dmToDate(entity.getDmToDate())
				.dmDays(entity.getDmDays())
				.dmChargeAmt(entity.getDmChargeAmt())
				.cntTaxPercentage(entity.getCntTaxPercentage())
				.cntTaxAmount(entity.getCntTaxAmount())
				.emptyIn(entity.getEmptyIn())
				.cntTaxPoid(entity.getCntTaxPoid())
				.build();
	}

	public ArShReceiptContainerDtl mapContainerDtoToEntity(ReceiptContainerDto dto, Long transactionPoid, Long detRowId) {
		if (dto == null) return null;
		
		return ArShReceiptContainerDtl.builder()
				.id(new TransactionDtlId(transactionPoid, detRowId))
				.containerSocYn(dto.getContainerSocYn())
				.blPoid(dto.getBlPoid())
				.containerNo(dto.getContainerNo())
				.equipmentIsoType(dto.getEquipmentIsoType())
				.freeDays(dto.getFreeDays())
				.dmFrmDate(dto.getDmFrmDate())
				.dmToDate(dto.getDmToDate())
				.dmDays(dto.getDmDays())
				.dmChargeAmt(dto.getDmChargeAmt())
				.cntTaxPercentage(dto.getCntTaxPercentage())
				.cntTaxAmount(dto.getCntTaxAmount())
				.emptyIn(dto.getEmptyIn())
				.cntTaxPoid(dto.getCntTaxPoid())
				.build();
	}

	public void updateContainerEntity(ReceiptContainerDto dto, ArShReceiptContainerDtl entity) {
		if (dto == null) return;
		entity.setContainerSocYn(dto.getContainerSocYn());
		entity.setBlPoid(dto.getBlPoid());
		entity.setContainerNo(dto.getContainerNo());
		entity.setEquipmentIsoType(dto.getEquipmentIsoType());
		entity.setFreeDays(dto.getFreeDays());
		entity.setDmFrmDate(dto.getDmFrmDate());
		entity.setDmToDate(dto.getDmToDate());
		entity.setDmDays(dto.getDmDays());
		entity.setDmChargeAmt(dto.getDmChargeAmt());
		entity.setCntTaxPercentage(dto.getCntTaxPercentage());
		entity.setCntTaxAmount(dto.getCntTaxAmount());
		entity.setEmptyIn(dto.getEmptyIn());
		entity.setCntTaxPoid(dto.getCntTaxPoid());
	}

	public List<ReceiptContainerDto> mapContainerDtlListToDto(List<ArShReceiptContainerDtl> entities) {
		if (entities == null) return null;
		return entities.stream().map(this::mapContainerDtlToDto).collect(Collectors.toList());
	}


	public ReceiptCharges mapChargesDtlToDto(ArShReceiptChargesDtl entity) {
		if (entity == null) return null;
		
		return ReceiptCharges.builder()
				.detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
				.blPoid(entity.getBlPoid())
				.chargePoid(entity.getChargePoid())
				.amount(entity.getAmount())
				.taxPercentage(entity.getTaxPercentage())
				.taxAmount(entity.getTaxAmount())
				.taxPoid(entity.getTaxPoid())
				.amountSelect(entity.getAmountSelect())
				.build();
	}

	public ArShReceiptChargesDtl mapChargesDtoToEntity(ReceiptCharges dto, Long transactionPoid, Long detRowId) {
		if (dto == null) return null;
		
		return ArShReceiptChargesDtl.builder()
				.id(new TransactionDtlId(transactionPoid, detRowId))
				.blPoid(dto.getBlPoid())
				.chargePoid(dto.getChargePoid())
				.amount(dto.getAmount())
				.taxPercentage(dto.getTaxPercentage())
				.taxAmount(dto.getTaxAmount())
				.taxPoid(dto.getTaxPoid())
				.amountSelect(dto.getAmountSelect())
				.build();
	}

	public void updateChargesEntity(ReceiptCharges dto, ArShReceiptChargesDtl entity) {
		if (dto == null) return;
		entity.setBlPoid(dto.getBlPoid());
		entity.setChargePoid(dto.getChargePoid());
		entity.setAmount(dto.getAmount());
		entity.setTaxPercentage(dto.getTaxPercentage());
		entity.setTaxAmount(dto.getTaxAmount());
		entity.setTaxPoid(dto.getTaxPoid());
		entity.setAmountSelect(dto.getAmountSelect());
	}

	public List<ReceiptCharges> mapChargesDtlListToDto(List<ArShReceiptChargesDtl> entities) {
		if (entities == null) return null;
		return entities.stream().map(this::mapChargesDtlToDto).collect(Collectors.toList());
	}


	public ReceiptPaymentDetailDto mapPaymentDtlToDto(ArShReceiptPymtDetails entity) {
		if (entity == null) return null;
		
		return ReceiptPaymentDetailDto.builder()
				.detRowId(entity.getId() != null ? entity.getId().getDetRowId() : null)
				.pymtType(entity.getPymtType())
				.amount(entity.getAmount())
				.ttBankPoid(entity.getTtBankPoid())
				.chqCardno(entity.getChqCardno())
				.chqDate(entity.getChqDate())
				.accountName(entity.getAccountName())
				.accountNo(entity.getAccountNo())
				.bankPoid(entity.getBankPoid())
				.build();
	}

	public ArShReceiptPymtDetails mapPaymentDtoToEntity(ReceiptPaymentDetailDto dto, Long transactionPoid, Long detRowId) {
		if (dto == null) return null;
		
		return ArShReceiptPymtDetails.builder()
				.id(new TransactionDtlId(transactionPoid, detRowId))
				.pymtType(dto.getPymtType())
				.amount(dto.getAmount())
				.ttBankPoid(dto.getTtBankPoid())
				.chqCardno(dto.getChqCardno())
				.chqDate(dto.getChqDate())
				.accountName(dto.getAccountName())
				.accountNo(dto.getAccountNo())
				.bankPoid(dto.getBankPoid())
				.build();
	}

	public void updatePaymentEntity(ReceiptPaymentDetailDto dto, ArShReceiptPymtDetails entity) {
		if (dto == null) return;
		entity.setPymtType(dto.getPymtType());
		entity.setAmount(dto.getAmount());
		entity.setTtBankPoid(dto.getTtBankPoid());
		entity.setChqCardno(dto.getChqCardno());
		entity.setChqDate(dto.getChqDate());
		entity.setAccountName(dto.getAccountName());
		entity.setAccountNo(dto.getAccountNo());
		entity.setBankPoid(dto.getBankPoid());
	}

	public List<ReceiptPaymentDetailDto> mapPaymentDtlListToDto(List<ArShReceiptPymtDetails> entities) {
		if (entities == null) return null;
		return entities.stream().map(this::mapPaymentDtlToDto).collect(Collectors.toList());
	}


	public ReceiptsBlDetailsDto mapBlDetailsEntityToDto(ArShReceiptHdr entity, 
			List<ArShReceiptContainerDtl> containers,
			List<ArShReceiptChargesDtl> charges,
			List<ArShReceiptPymtDetails> payments) {
		if (entity == null) return null;

		return ReceiptsBlDetailsDto.builder()
				.docRef(entity.getDocRef())
				.date(entity.getTransactionDate())
				.blPoid(entity.getBlPoid())
				.companyPoid(entity.getCompanyPoid())
				.releaseType(entity.getOrignalBlReleaseType())
				.originalReleaseType(entity.getBlReleaseTypeOffice())
				.printDoCustomerPoid(entity.getPrintCustomerPoid())
				.chequeCompany(entity.getDocumentCmpPoid())
				.cpr(entity.getDoReleasedIdPerson())
				.name(entity.getDoReleasedToPerson())
				.contact(entity.getDoReleasedAddrsPerson())
				.paymentReference(entity.getPaymentRef())
				.remarks(entity.getRemarks())
				.token(entity.getTokenNumber())
				.container(mapContainerDtlListToDto(containers))
				.charges(mapChargesDtlListToDto(charges))
				.paymentDetail(mapPaymentDtlListToDto(payments))
				.build();
	}

	public ArShReceiptHdr mapBlDetailsDtoToEntity(ReceiptsBlDetailsDto dto) {
		if (dto == null) return null;

		return ArShReceiptHdr.builder()
				.docRef(dto.getDocRef())
				.transactionDate(dto.getDate())
				.blPoid(dto.getBlPoid())
				.companyPoid(dto.getCompanyPoid())
				.blReleaseTypeOffice(dto.getOriginalReleaseType())
				.printCustomerPoid(dto.getPrintDoCustomerPoid())
				.documentCmpPoid(dto.getChequeCompany())
				.doReleasedIdPerson(dto.getCpr())
				.doReleasedToPerson(dto.getName())
				.doReleasedAddrsPerson(dto.getContact())
				.paymentRef(dto.getPaymentReference())
				.remarks(dto.getRemarks())
				.tokenNumber(dto.getToken())
				.rcptAmount(BigDecimal.valueOf(dto.getAmount()))
				.build();
	}
}
