package com.asg.shipping.exportManifestUpdate.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    ISCREATED, ISUPDATED, ISDELETED, NOCHANGES;

    @JsonCreator
    public static ActionType from(String value) {
        if (value == null || value.isBlank()) return NOCHANGES;
        switch (value.trim().toUpperCase()) {
            case "INSERT": case "CREATE": case "ISCREATED": case "ACTION_ISCREATED":
                return ISCREATED;
            case "UPDATE": case "EDIT": case "ISUPDATED": case "ACTION_ISUPDATED":
                return ISUPDATED;
            case "DELETE": case "ISDELETED": case "ACTION_ISDELETED":
                return ISDELETED;
            default:
                return NOCHANGES;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
