package com.asg.shipping.vvc.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class ApiResponse {

    public static ResponseEntity<?> success(String message, Object data) {
        return ResponseEntity.ok(Map.of(
                "statusCode", HttpStatus.OK.value(),
                "success", true,
                "message", message,
                "result", data != null ? Map.of("data", data) : ""
        ));
    }

    public static ResponseEntity<?> success(String message) {
        return ResponseEntity.ok(Map.of(
                "statusCode", HttpStatus.OK.value(),
                "success", true,
                "message", message
        ));
    }

    public static ResponseEntity<?> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST.value());
    }

    public static ResponseEntity<?> internalServerError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static ResponseEntity<?> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND.value());
    }

    public static ResponseEntity<?> conflict(String message) {
        return error(message, HttpStatus.CONFLICT.value());
    }

    public static ResponseEntity<?> error(String message, int statusCode) {
        return ResponseEntity.status(statusCode).body(Map.of(
                "success", false,
                "statusCode", String.valueOf(statusCode),
                "message", message
        ));
    }

    public static ResponseEntity<?> error(String message, int statusCode, Object errors) {
        return ResponseEntity.status(statusCode).body(Map.of(
                "success", false,
                "statusCode", String.valueOf(statusCode),
                "message", message,
                "errors", errors
        ));
    }
}


