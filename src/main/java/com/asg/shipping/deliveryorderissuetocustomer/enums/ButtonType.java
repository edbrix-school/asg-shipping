package com.asg.shipping.deliveryorderissuetocustomer.enums;

import lombok.Getter;

@Getter
public enum ButtonType {
    DeliveryOrderPrint,
    ContainerFormPrint,
    ReturnFormPrint;

    public static ButtonType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ButtonType type : ButtonType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
