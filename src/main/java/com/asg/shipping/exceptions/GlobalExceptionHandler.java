package com.asg.shipping.exceptions;

import com.asg.shipping.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleAsgException(CustomException ex) {
        log.error("CustomException: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException ex) {
        return ApiResponse.badRequest(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleDateFormatException(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == LocalDate.class) {
            String value = ex.getValue() != null ? ex.getValue().toString() : null;
            String parameterName = ex.getPropertyName();

            String message = "Invalid date format. Please use YYYY-MM-DD.";
            if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(parameterName)) {
                message = String.format("Invalid value '%s' for parameter '%s'. Expected format: YYYY-MM-DD.", value, parameterName);
            }
            return ApiResponse.badRequest(message);
        }
        return ApiResponse.badRequest(ex.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.info("Validation errors at {}", request.getRequestURI());
        return ApiResponse.error("Validation error occurred", HttpStatus.BAD_REQUEST.value(), errors);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<?> handleMissingPathVariable(MissingPathVariableException ex) {
        String msg = String.format("Missing path variable: '%s'", ex.getVariableName());
        return ApiResponse.badRequest(msg);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException ex) {
        String msg = String.format("Missing Header variable: '%s'", ex.getHeaderName());
        return ApiResponse.badRequest(msg);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<?> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        return ApiResponse.conflict(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        return ApiResponse.notFound(ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ApiResponse.notFound(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseErrors(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();

        // extract root cause if it’s IllegalArgumentException from enum
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : "Invalid request payload";
        return ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        String msg = "File exceeds the maximum allowed upload size. Please upload a smaller file.";
        log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
        return ApiResponse.badRequest(msg);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("DataIntegrityViolation at {}", request.getRequestURI(), ex);
        String msg = ex.getMostSpecificCause().getMessage();
        // ORA-20001: financial year violation (from SHIP_VOYAGE_HDR_GTTRG)
        if (msg != null && msg.contains("ORA-20001")) {
            return ApiResponse.badRequest(extractOraMessage(msg, "Changes allowed only within current Financial Period"));
        }
        // ORA-20002: business rule violations (from SHIP_VOYAGE_HDR_TRG and other triggers)
        if (msg != null && msg.contains("ORA-20002")) {
            return ApiResponse.badRequest(extractOraMessage(msg, "Business rule violation"));
        }
        // FK constraint violation — e.g. invalid container type, line, vessel poid
        if (msg != null && msg.contains("ORA-02291")) {
            return ApiResponse.badRequest("Invalid reference: a required parent record does not exist. Please check your input values.");
        }
        // Unique constraint violation
        if (msg != null && msg.contains("ORA-00001")) {
            return ApiResponse.conflict("Duplicate record: a record with the same unique key already exists.");
        }
        return ApiResponse.error("Data integrity error: " + cleanOraMessage(msg), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<?> handleJpaSystemException(JpaSystemException ex, HttpServletRequest request) {
        log.error("JpaSystemException at {}", request.getRequestURI(), ex);
        String msg = ex.getMostSpecificCause().getMessage();
        // ORA-20001: financial year violation (from SHIP_VOYAGE_HDR_GTTRG)
        if (msg != null && msg.contains("ORA-20001")) {
            return ApiResponse.badRequest(extractOraMessage(msg, "Changes allowed only within current Financial Period"));
        }
        // ORA-20002: business rule violations (from triggers)
        if (msg != null && msg.contains("ORA-20002")) {
            return ApiResponse.badRequest(extractOraMessage(msg, "Business rule violation"));
        }
        // FK constraint violation
        if (msg != null && msg.contains("ORA-02291")) {
            return ApiResponse.badRequest("Invalid reference: a required parent record does not exist. Please check your input values.");
        }
        // Unique constraint violation
        if (msg != null && msg.contains("ORA-00001")) {
            return ApiResponse.conflict("Duplicate record: a record with the same unique key already exists.");
        }
        return ApiResponse.error(cleanOraMessage(msg), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * Extracts the human-readable message from an Oracle trigger error.
     * Oracle format: "ORA-20001: <message>\nORA-06512: at ..."
     * Returns the text after the error code, trimmed, up to the first newline.
     */
    private String extractOraMessage(String raw, String fallback) {
        if (raw == null) return fallback;
        // Find the colon after ORA-XXXXX and take text until next newline
        int colonIdx = raw.indexOf(':');
        if (colonIdx != -1 && colonIdx < raw.length() - 1) {
            String after = raw.substring(colonIdx + 1).trim();
            int newline = after.indexOf('\n');
            String extracted = newline != -1 ? after.substring(0, newline).trim() : after.trim();
            return extracted.isEmpty() ? fallback : extracted;
        }
        return fallback;
    }

    /**
     * Strips Oracle stack trace lines (ORA-06512 etc.) leaving only the first line.
     */
    private String cleanOraMessage(String raw) {
        if (raw == null) return "Unexpected database error";
        int newline = raw.indexOf('\n');
        return newline != -1 ? raw.substring(0, newline).trim() : raw.trim();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {} ", request.getRequestURI(), ex);
        return ApiResponse.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {} ", request.getRequestURI(), ex);
        return ApiResponse.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(com.asg.common.lib.exception.ResourceAlreadyExistsException.class)
    public ResponseEntity<?> handleResourceAlreadyExists(com.asg.common.lib.exception.ResourceAlreadyExistsException ex) {
        return ApiResponse.conflict(ex.getMessage());
    }

    @ExceptionHandler(com.asg.common.lib.exception.ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(com.asg.common.lib.exception.ResourceNotFoundException ex) {
        return ApiResponse.notFound(ex.getMessage());
    }

    @ExceptionHandler(com.asg.shipping.exceptions.ValidationException.class)
    public ResponseEntity<?> handleShippingValidationException(com.asg.shipping.exceptions.ValidationException ex) {
        Map<String, Object> errors = new HashMap<>();
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            ex.getFieldErrors().forEach(error ->
                    errors.put(error.getField() != null ? error.getField() : "general", error.getMessage())
            );
            return ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), errors);
        }
        return ApiResponse.badRequest(ex.getMessage());
    }

    @ExceptionHandler(com.asg.common.lib.exception.ValidationException.class)
    public ResponseEntity<?> handleValidationException(com.asg.common.lib.exception.ValidationException ex) {
        return ApiResponse.badRequest(ex.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpServletRequest request) {

        Map<String, Object> errors = new HashMap<>();

        ex.getAllErrors().forEach(error -> {
            String fieldName = "unknown";

            if (error instanceof org.springframework.validation.FieldError fieldError) {
                fieldName = fieldError.getField();
            } else if (error.getCodes() != null && error.getCodes().length > 0) {
                // fallback: extract parameter name from validation codes
                fieldName = error.getCodes()[0];
            }

            errors.put(fieldName, error.getDefaultMessage());
        });

        log.info("Validation errors at {}", request.getRequestURI());

        return ApiResponse.error("Validation error occurred", HttpStatus.BAD_REQUEST.value(), errors);
    }
}
