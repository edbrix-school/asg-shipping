package com.asg.shipping.importmanifestupdate.service;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.importmanifestupdate.constants.BlManifestValidationMessages;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
public class BlManifestValidationService {

    private static final Set<String> HOLD_REASONS_REQUIRING_REMARKS = Set.of("1", "2", "3");
    private static final Set<Long> FREIGHT_CHARGE_POIDS = Set.of(65L, 748L, 928L);
    private static final Long DEMURRAGE_CHARGE_POID = 94L;


    public void validateMandatoryFields(Long voyageTransactionPoid, String cargoType, String blNumber, String blType) {
        if (voyageTransactionPoid == null) {
            throw new ValidationException(BlManifestValidationMessages.VOYAGE_REQUIRED);
        }
        if (cargoType == null) {
            throw new ValidationException(BlManifestValidationMessages.CARGO_TYPE_REQUIRED);
        }
        if (blNumber == null) {
            throw new ValidationException(BlManifestValidationMessages.BL_NUMBER_REQUIRED);
        }
        if (blType == null) {
            throw new ValidationException(BlManifestValidationMessages.BL_TYPE_REQUIRED);
        }
    }


    public void validateHoldReasons(String holdReason, String holdCanDo, String holdRemarks) {
        if (holdReason != null
                && HOLD_REASONS_REQUIRING_REMARKS.contains(holdReason)
                && isBlank(holdRemarks)) {
            log.error("Validation failed: Hold reason {} requires remarks", holdReason);
            throw new ValidationException(BlManifestValidationMessages.HOLD_REMARKS_REQUIRED);
        }

        if ("Y".equalsIgnoreCase(holdCanDo) && isBlank(holdRemarks)) {
            log.error("Validation failed: Hold CAN/DO requires remarks");
            throw new ValidationException(BlManifestValidationMessages.HOLD_CAN_DO_REMARKS_REQUIRED);
        }
    }


    public void validateAddressesForCan(String holdReason, Long consigneePoid, Long notifyPoid,
                                         String manuallyCanSend, boolean addressFound) {
        String reason = holdReason != null ? holdReason : "5";

        if ("5".equalsIgnoreCase(reason)) {
            return;
        }

        if (consigneePoid == null || consigneePoid.equals(1L)) {
            log.error("Validation failed: Invalid Consignee POID: {}", consigneePoid);
            throw new ValidationException(BlManifestValidationMessages.CONSIGNEE_REQUIRED);
        }

        if (notifyPoid == null || notifyPoid.equals(1L)) {
            log.error("Validation failed: Invalid Notify POID: {}", notifyPoid);
            throw new ValidationException(BlManifestValidationMessages.NOTIFY_PARTY_REQUIRED);
        }

        if (!addressFound && (manuallyCanSend == null || "N".equalsIgnoreCase(manuallyCanSend))) {
            log.error("Validation failed: No CAN address available and manual send is disabled");
            throw new ValidationException(BlManifestValidationMessages.NO_CAN_ADDRESS);
        }
    }


    public void validateContainerFields(List<? extends ContainerValidatable> containers) {
        if (containers == null || containers.isEmpty()) {
            return;
        }
        for (ContainerValidatable container : containers) {
            if (isBlank(container.getContainerNoValue()) || isBlank(container.getEquipmentIsoTypeValue())) {
                log.error("Validation failed: Container missing required fields - containerNo: {}, isoType: {}",
                        container.getContainerNoValue(), container.getEquipmentIsoTypeValue());
                throw new ValidationException(BlManifestValidationMessages.CONTAINER_FIELDS_REQUIRED);
            }
        }
    }


    public void validateFinancialGain(List<? extends ChargeValidatable> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }
        BigDecimal totalSell = BigDecimal.ZERO;
        BigDecimal totalBuy = BigDecimal.ZERO;
        for (ChargeValidatable charge : charges) {
            BigDecimal qty = charge.getQuantityValue() != null ? charge.getQuantityValue() : BigDecimal.ONE;
            BigDecimal sell = charge.getSellValue() != null ? charge.getSellValue() : BigDecimal.ZERO;
            BigDecimal buy = charge.getBuyValue() != null ? charge.getBuyValue() : BigDecimal.ZERO;
            totalSell = totalSell.add(sell.multiply(qty));
            totalBuy = totalBuy.add(buy.multiply(qty));
        }
        BigDecimal totalGain = totalSell.subtract(totalBuy);
        if (totalGain.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Validation failed: Total financial gain is negative: {}", totalGain);
            throw new ValidationException(
                    String.format(BlManifestValidationMessages.NEGATIVE_GAIN, totalGain));
        }
    }


    public void validateFreightType(String freightStatus, String holdReason,
                                     List<? extends ChargeValidatable> charges) {
        validateFreightType(freightStatus, holdReason, charges, List.of());
    }


    public void validateFreightType(String freightStatus, String holdReason,
                                     List<? extends ChargeValidatable> charges,
                                     List<? extends ChargeValidatable> otherCharges) {
        if (StringUtils.isBlank(freightStatus)) {
            return;
        }

        String reason = holdReason != null ? holdReason : "5";
        String globalFreightType = determineGlobalFreightType(otherCharges);
        if ("XX".equals(globalFreightType)) {
            globalFreightType = determineGlobalFreightType(charges);
        }

        if ("XX".equals(globalFreightType) && !"5".equalsIgnoreCase(reason)) {
            throw new ValidationException(BlManifestValidationMessages.FREIGHT_TYPE_NOT_ENTERED);
        }

        if ("1".equalsIgnoreCase(freightStatus)
                && !"P".equals(globalFreightType)
                && !"XX".equals(globalFreightType)
                && !"5".equalsIgnoreCase(reason)) {
            throw new ValidationException(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_PREPAID);
        }

        if ("2".equalsIgnoreCase(freightStatus)
                && !"C".equals(globalFreightType)
                && !"XX".equals(globalFreightType)
                && !"5".equalsIgnoreCase(reason)) {
            throw new ValidationException(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_COLLECT);
        }

        if ("3".equalsIgnoreCase(freightStatus)
                && !"E".equals(globalFreightType)
                && !"XX".equals(globalFreightType)
                && !"5".equalsIgnoreCase(reason)) {
            throw new ValidationException(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_ELSEWHERE);
        }
    }


    public void validateDemurrageChargeCode(List<? extends ChargeValidatable> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }
        for (ChargeValidatable charge : charges) {
            if (charge.getChargePoidValue() != null && charge.getChargePoidValue().equals(DEMURRAGE_CHARGE_POID)) {
                log.error("Validation failed: Demurrage charge code 94 is not allowed");
                throw new ValidationException(BlManifestValidationMessages.DEMURRAGE_CODE_94_NOT_ALLOWED);
            }
        }
    }


    private String determineGlobalFreightType(List<? extends ChargeValidatable> charges) {
        if (charges == null || charges.isEmpty()) {
            return "XX";
        }

        String freightType = "XX";
        for (ChargeValidatable charge : charges) {
            if (charge.getFreightTypeValue() != null && !charge.getFreightTypeValue().trim().isEmpty()) {
                Long chargePoid = charge.getChargePoidValue();
                if (chargePoid != null && FREIGHT_CHARGE_POIDS.contains(chargePoid)) {
                    freightType = charge.getFreightTypeValue();
                }
            }
        }
        return freightType;
    }

    private boolean isBlank(String value) {
        return StringUtils.isBlank(value);
    }


    public interface ContainerValidatable {
        String getContainerNoValue();
        String getEquipmentIsoTypeValue();
    }


    public interface ChargeValidatable {
        Long getChargePoidValue();
        String getFreightTypeValue();
        BigDecimal getQuantityValue();
        BigDecimal getSellValue();
        BigDecimal getBuyValue();
    }
}
