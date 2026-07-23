package com.asg.shipping.exportManifestUpdate.util;

import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.address.entity.AddressDetailsRepository;

import java.util.Objects;
import java.util.Optional;


public final class ExportManifestAddressTypeAudit {

    private static final String LOG_TABLE_HDR = "SHIP_BL_MANIFEST_HDR";
    private static final String FIELD_SHIPPER_ADDRESS_TYPE = "ShipperAddressType";

    private ExportManifestAddressTypeAudit() {
    }

  
    public static void logShipperAddressTypeChange(
            LoggingService loggingService,
            AddressDetailsRepository addressDetailsRepository,
            String documentId,
            String docKeyPoid,
            Long beforeShipperAddressPoid,
            String shipperAddressTypeFromRequest) {
        if (loggingService == null || addressDetailsRepository == null || !hasText(shipperAddressTypeFromRequest)) {
            return;
        }
        String beforeType = resolveAddressType(addressDetailsRepository, beforeShipperAddressPoid);
        String afterType = shipperAddressTypeFromRequest.trim();
        if (equalsNormalized(beforeType, afterType)) {
            return;
        }
        String keyDetail = "KeyId = TRANSACTION_POID:" + docKeyPoid;
        loggingService.createLogDetailsEntry(
                documentId,
                docKeyPoid,
                FIELD_SHIPPER_ADDRESS_TYPE,
                beforeType,
                afterType,
                keyDetail,
                LOG_TABLE_HDR);
    }

    static String resolveAddressType(AddressDetailsRepository repository, Long addressPoid) {
        if (addressPoid == null) {
            return "";
        }
        Optional<AddressDetails> byId = repository.findById(String.valueOf(addressPoid));
        if (byId.isPresent()) {
            return normalizeType(byId.get().getAddressType());
        }
        Optional<AddressDetails> withDecimalSuffix = repository.findById(addressPoid + ".0");
        return withDecimalSuffix.map(d -> normalizeType(d.getAddressType())).orElse("");
    }

    private static String normalizeType(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean equalsNormalized(String a, String b) {
        return Objects.equals(normalizeType(a).toUpperCase(), normalizeType(b).toUpperCase());
    }
}
