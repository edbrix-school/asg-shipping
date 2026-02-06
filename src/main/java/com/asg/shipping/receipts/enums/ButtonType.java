package com.asg.shipping.receipts.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Getter
public enum ButtonType {
    Receipts,
    Invoice;

    @JsonCreator
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
    
    @Component
    public static class StringToButtonTypeConverter implements Converter<String, ButtonType> {
        @Override
        public ButtonType convert(String source) {
            return ButtonType.fromString(source);
        }
    }
}