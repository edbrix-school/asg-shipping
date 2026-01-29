package com.asg.shipping.collectionhandover.util;

import com.asg.shipping.collectionhandover.dto.*;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class CollectionHandoverMapper {

    /**
     * Convert Header Entity to DTO
     */
    public CollectionHandoverDto mapToDto(ArShDayEndCloseHdr entity, List<ArShDayEndCloseDtl> detailList) {
        if (entity == null) {
            return null;
        }

        CollectionHandoverDto dto = CollectionHandoverDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .transactionDate(entity.getTransactionDate())
                .groupPoid(entity.getGroupPoid())
                .companyPoid(entity.getCompanyPoid())
                .docRef(entity.getDocRef())
                .locationCode(entity.getLocationCode())
                .cashAmount(entity.getCashAmount())
                .chequeAmount(entity.getChequeAmount())
                .outstandingAmount(entity.getOutstandingAmount())
                .totalAmount(entity.getTotalAmount())
                .noofChqs(entity.getNoofChqs())
                .locRemarks(entity.getLocRemarks())
                .verifiedRcvd(entity.getVerifiedRcvd())
                .mainOfcRemarks(entity.getMainOfcRemarks())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();

        // Map detail list
        if (detailList != null) {
            dto.setDetails(detailList.stream()
                    .map(this::mapDetailToDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Map Detail Entity to DTO
     */
    public CollectionHandoverDetailDto mapDetailToDto(ArShDayEndCloseDtl entity) {
        if (entity == null) {
            return null;
        }
        return CollectionHandoverDetailDto.builder()
                .detRowId(entity.getDetRowId())
                .currencyAmount(entity.getCurrencyAmount())
                .currencyType(entity.getCurrencyType())
                .noOfTran(entity.getNoOfTran())
                .cashAmount(entity.getCashAmount())
                .build();
    }

    /**
     * Map CreateDTO to Header Entity
     */
    public void mapCreateDTOToEntity(CollectionHandoverCreateDTO dto, ArShDayEndCloseHdr entity, Long groupPoid, Long userPoid) {
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setCompanyPoid(dto.getCompanyPoid());
        entity.setGroupPoid(groupPoid);
        entity.setDocRef(dto.getDocRef());
        entity.setLocationCode(dto.getLocationCode());
        entity.setCashAmount(dto.getCashAmount());
        entity.setChequeAmount(dto.getChequeAmount());
        entity.setOutstandingAmount(dto.getOutstandingAmount());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setNoofChqs(dto.getNoofChqs());
        entity.setLocRemarks(dto.getLocRemarks());
        entity.setVerifiedRcvd(dto.getVerifiedRcvd() != null ? dto.getVerifiedRcvd() : "N");
        entity.setMainOfcRemarks(dto.getMainOfcRemarks());

        // Set audit fields
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        // Set deleted flag
        entity.setDeleted("N");
    }

    /**
     * Map UpdateDTO to Header Entity
     */
    public void mapUpdateDTOToEntity(CollectionHandoverUpdateDTO dto, ArShDayEndCloseHdr entity, Long groupPoid, Long userPoid) {
        if (dto.getDocRef() != null) {
            entity.setDocRef(dto.getDocRef());
        }
        if (dto.getLocationCode() != null) {
            entity.setLocationCode(dto.getLocationCode());
        }
        if (dto.getCashAmount() != null) {
            entity.setCashAmount(dto.getCashAmount());
        }
        if (dto.getChequeAmount() != null) {
            entity.setChequeAmount(dto.getChequeAmount());
        }
        if (dto.getOutstandingAmount() != null) {
            entity.setOutstandingAmount(dto.getOutstandingAmount());
        }
        if (dto.getTotalAmount() != null) {
            entity.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getNoofChqs() != null) {
            entity.setNoofChqs(dto.getNoofChqs());
        }
        if (dto.getLocRemarks() != null) {
            entity.setLocRemarks(dto.getLocRemarks());
        }
        if (dto.getVerifiedRcvd() != null) {
            entity.setVerifiedRcvd(dto.getVerifiedRcvd());
        }
        if (dto.getMainOfcRemarks() != null) {
            entity.setMainOfcRemarks(dto.getMainOfcRemarks());
        }

        // Update audit fields
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }

    /**
     * Map CreateDTO to Detail Entity
     */
    public void mapDetailCreateDTOToEntity(CollectionHandoverDetailCreateDTO dto, ArShDayEndCloseDtl entity) {
        entity.setCurrencyAmount(dto.getCurrencyAmount());
        entity.setCurrencyType(dto.getCurrencyType());
        entity.setNoOfTran(dto.getNoOfTran());
        entity.setCashAmount(dto.getCashAmount());
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
    }

    /**
     * Map UpdateDTO to Detail Entity
     */
    public void mapDetailUpdateDTOToEntity(CollectionHandoverDetailUpdateDTO dto, ArShDayEndCloseDtl entity) {
        entity.setCurrencyAmount(dto.getCurrencyAmount());
        entity.setCurrencyType(dto.getCurrencyType());
        entity.setNoOfTran(dto.getNoOfTran());
        entity.setCashAmount(dto.getCashAmount());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}

