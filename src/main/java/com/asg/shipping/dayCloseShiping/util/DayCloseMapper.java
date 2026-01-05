package com.asg.shipping.dayCloseShiping.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.asg.shipping.dayCloseShiping.dto.DayCloseDenominationDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseHdrDto;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseHdr;

@Component
public class DayCloseMapper {

	public void mapCreateDTOToEntity(DayCloseHdrDto dto, ArShDayEndCloseHdr entity, Long groupPoid, Long companyPoid) {

		entity.setGroupPoid(groupPoid);
		entity.setCompanyPoid(companyPoid);
		entity.setTransactionDate(dto.getTransactionDate());
		entity.setLocationCode(dto.getLocationCode() != null ? dto.getLocationCode() : "PORT");
		entity.setCashAmount(dto.getCashAmount());
		entity.setChequeAmount(dto.getChequeAmount());
		entity.setOutstandingAmount(dto.getOutstandingAmount());
		entity.setTotalAmount(dto.getTotalAmount());
		entity.setNoOfCheques(dto.getNoOfCheques());
		entity.setLocRemarks(dto.getLocRemarks());
		entity.setVerifiedRcvd(dto.getVerifiedRcvd() != null ? dto.getVerifiedRcvd() : "N");
		entity.setMainOfcRemarks(dto.getMainOfcRemarks() != null ? dto.getMainOfcRemarks() : ".");
		entity.setDocRef(dto.getDocRef());
		entity.setDeleted("N");
	}

	public DayCloseDto mapToDto(ArShDayEndCloseHdr entity) {
		if (entity == null) {
			return null;
		}

		DayCloseHdrDto header = DayCloseHdrDto.builder().transactionPoid(entity.getTransactionPoid())
				.groupPoid(entity.getGroupPoid()).companyPoid(entity.getCompanyPoid()).docRef(entity.getDocRef())
				.transactionDate(entity.getTransactionDate()).locationCode(entity.getLocationCode())
				.cashAmount(entity.getCashAmount()).chequeAmount(entity.getChequeAmount())
				.outstandingAmount(entity.getOutstandingAmount()).totalAmount(entity.getTotalAmount())
				.noOfCheques(entity.getNoOfCheques()).locRemarks(entity.getLocRemarks())
				.verifiedRcvd(entity.getVerifiedRcvd()).mainOfcRemarks(entity.getMainOfcRemarks()).build();
		return DayCloseDto.builder().header(header).build();
	}

	public DayCloseDenominationDto mapDtlToDto(ArShDayEndCloseDtl entity) {
		if (entity == null)
			return null;
		return DayCloseDenominationDto.builder().detRowId(entity.getDetRowId()).denomination(entity.getCurrencyAmount())
				.currencyType(entity.getCurrencyType()).noOfTran(entity.getNoOfTran())
				.cashAmount(entity.getCashAmount()).build();
	}

	public ArShDayEndCloseDtl mapDtlFromDto(DayCloseDenominationDto dto, Long transactionPoid, Long detRowId) {
		if (dto == null)
			return null;
		ArShDayEndCloseDtl entity = ArShDayEndCloseDtl.builder().transactionPoid(transactionPoid).detRowId(detRowId)
				.currencyAmount(dto.getDenomination()).currencyType(dto.getCurrencyType()).noOfTran(dto.getNoOfTran())
				.build();

		if (dto.getCashAmount() == null && entity.getCurrencyAmount() != null && entity.getNoOfTran() != null) {
			entity.setCashAmount(entity.getCurrencyAmount().multiply(BigDecimal.valueOf(entity.getNoOfTran())));
		} else {
			entity.setCashAmount(dto.getCashAmount());
		}

		return entity;
	}

	public List<DayCloseDenominationDto> mapDtlListToDto(List<ArShDayEndCloseDtl> entities) {
		if (entities == null)
			return null;
		return entities.stream().map(this::mapDtlToDto).collect(Collectors.toList());
	}
}
