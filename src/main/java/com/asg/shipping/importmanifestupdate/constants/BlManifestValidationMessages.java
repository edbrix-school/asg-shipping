package com.asg.shipping.importManifestUpdate.constants;


public final class BlManifestValidationMessages {

    private BlManifestValidationMessages() {
    }

    public static final String VOYAGE_REQUIRED =
            "Voyage number is required. Please select a valid vessel/voyage.";
    public static final String CARGO_TYPE_REQUIRED =
            "Cargo type is required. Please select a cargo type.";
    public static final String BL_NUMBER_REQUIRED =
            "BL number is required. Please enter a valid BL number.";
    public static final String BL_TYPE_REQUIRED =
            "BL type is required. Please select a BL type.";

    public static final String HOLD_REMARKS_REQUIRED =
            "Hold remarks are required when a hold reason is selected. Please provide remarks.";
    public static final String HOLD_CAN_DO_REMARKS_REQUIRED =
            "Hold remarks are required when 'Hold CAN Auto' is enabled. Please provide remarks.";

    public static final String CONSIGNEE_REQUIRED =
            "A valid consignee must be selected for Cargo Arrival Notice (CAN) generation.";
    public static final String NOTIFY_PARTY_REQUIRED =
            "A valid notify party must be selected for Cargo Arrival Notice (CAN) generation.";
    public static final String NO_CAN_ADDRESS =
            "No email or fax address is available for CAN notification. Please add an address or enable 'Manual CAN Send'.";

    public static final String CONTAINER_FIELDS_REQUIRED =
            "Container number and equipment ISO type are mandatory for each container entry.";
    public static final String CONTAINER_DUPLICATE =
            "Duplicate container number '%s' found. Each container number must be unique within a BL.";

    public static final String NEGATIVE_GAIN =
            "Total financial gain cannot be negative. Current gain: %s. Please review the charge amounts.";

    public static final String FREIGHT_TYPE_NOT_ENTERED =
            "Freight type is not specified in the charge details. Please assign a freight type to the applicable charges.";
    public static final String FREIGHT_STATUS_MISMATCH_PREPAID =
            "Freight status mismatch: BL freight is set to 'Prepaid' but charge details contain a non-prepaid freight type.";
    public static final String FREIGHT_STATUS_MISMATCH_COLLECT =
            "Freight status mismatch: BL freight is set to 'Collect' but charge details contain a non-collect freight type.";
    public static final String FREIGHT_STATUS_MISMATCH_ELSEWHERE =
            "Freight status mismatch: BL freight is set to 'Elsewhere' but charge details contain a non-elsewhere freight type.";

    public static final String DEMURRAGE_CODE_94_NOT_ALLOWED =
            "Charge code 94 (Demurrage) is not permitted. Please use the 'Manifested Demurrage' code (DEMMF) instead.";

    public static final String QUOTATION_MAPPING_REQUIRED =
            "A quotation must be mapped to this manifest. The BL has collect freight and is not booked by principal.";

    public static final String BL_NUMBER_EXISTS_FOR_VOYAGE =
            "BL number '%s' already exists for this voyage. Please use a unique BL number.";
    public static final String BL_NUMBER_EXISTS =
            "BL number '%s' already exists in the system. Please use a unique BL number.";

    public static final String PORT_OF_LOADING_REQUIRED =
            "Port of loading is required for import BL type. Please select a valid port.";

    public static final String VOYAGE_COMPANY_MISMATCH =
            "The selected voyage does not belong to your company. Please select a valid voyage.";
}
