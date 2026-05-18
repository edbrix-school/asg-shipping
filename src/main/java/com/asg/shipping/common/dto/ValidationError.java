package com.asg.shipping.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValidationError {
    private Integer recordIndex; // For array validation errors
    private String field;
    private String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
