package com.asg.shipping.vvc.exceptions;

import com.asg.shipping.vvc.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        log.error("CustomException: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<?> handleExists(ResourceAlreadyExistsException ex) {
        return ApiResponse.conflict(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.notFound(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.info("Validation errors at {}", request.getRequestURI());
        return ApiResponse.error("Validation error occurred", HttpStatus.BAD_REQUEST.value(), errors);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<?> handleMissingPathVariable(MissingPathVariableException ex) {
        return ApiResponse.badRequest("Missing path variable: '" + ex.getVariableName() + "'");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException ex) {
        return ApiResponse.badRequest("Missing Header variable: '" + ex.getHeaderName() + "'");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == LocalDate.class) {
            return ApiResponse.badRequest("Invalid date format. Please use YYYY-MM-DD.");
        }
        return ApiResponse.badRequest(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParse(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : "Invalid request payload";
        return ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
        return ApiResponse.badRequest("File exceeds the maximum allowed upload size. Please upload a smaller file.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return ApiResponse.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}


