package com.asg.shipping.exceptions;

public class ResourceAlreadyExistsException extends RuntimeException{

    private final String fieldName;
    private final String fieldValue;

    public ResourceAlreadyExistsException(String message) {
        super(message);
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ResourceAlreadyExistsException(String fieldName, String fieldValue) {
        super(String.format("%s already exists with value: %s", fieldName, fieldValue));
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}