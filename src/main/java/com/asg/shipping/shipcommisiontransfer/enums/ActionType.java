package com.asg.shipping.shipcommisiontransfer.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ActionType {
    isCreated,
    isUpdated,
    isDeleted,
    noChange;

    @JsonCreator
    public static ActionType fromString(String value) {
        if (value == null) {
            return null;
        }

        for (ActionType type : ActionType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid ActionType: " + value);
    }
}
