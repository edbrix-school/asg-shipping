package com.asg.shipping.vesselvoyagecreation.util;

import java.time.LocalDateTime;

public class DateValidationUtil {

    private DateValidationUtil() {}

    public static void requireGte(LocalDateTime a, LocalDateTime b, String message) {
        if (a == null || b == null) return;
        if (a.isBefore(b)) {
            throw new IllegalArgumentException(message);
        }
    }
}










