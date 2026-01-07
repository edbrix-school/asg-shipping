package com.asg.shipping.containerinventorymovementupdate.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeUtil {
    private DateTimeUtil() {}

    /**
     * Parses flexible datetime string (supports both ISO and UI formats) and converts to Oracle format.
     * Supports:
     * - ISO format: "2025-06-16T09:39:15" or "2025-06-16 09:39:15"
     * - UI format: "16-Jun-2025 09:39:15" (legacy display format)
     * - Date only: "2025-06-16" or "16-Jun-2025" (time defaults to 00:00:00)
     *
     * @param dateTimeStr Date/datetime string in various formats
     * @return Oracle-friendly string: "yyyy-MM-dd HH:mm:ss" (19 chars) for use in TO_DATE(SUBSTR('...',1,19),'RRRR-MM-DD HH24:MI:SS')
     * @throws IllegalArgumentException if format is invalid
     */
    public static String parseFlexibleDateTimeToOracle(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return null;
        String s = dateTimeStr.trim();

        // Try ISO datetime formats first
        try {
            if (s.contains("T")) {
                LocalDateTime ldt = LocalDateTime.parse(s);
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (DateTimeParseException ignored) {
            // Try other formats
        }

        try {
            if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                // "2025-06-16 09:39:15" format
                LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (DateTimeParseException ignored) {
            // Try other formats
        }

        // Try UI format: "16-Jun-2025 09:39:15"
        try {
            if (s.matches("\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
                LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (DateTimeParseException ignored) {
            // Try other formats
        }

        // Try date-only formats
        try {
            // ISO date: "2025-06-16"
            LocalDate date = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            // Try UI date format
        }

        try {
            // UI date: "16-Jun-2025"
            LocalDate date = LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            return date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            // All formats failed
        }

        throw new IllegalArgumentException("Invalid date/datetime format: " + dateTimeStr +
                ". Supported formats: yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd HH:mm:ss, dd-MMM-yyyy HH:mm:ss, yyyy-MM-dd, dd-MMM-yyyy");
    }

    /**
     * Parses flexible date string (supports both ISO and UI formats) and returns LocalDate.
     * Supports:
     * - ISO format: "2025-06-16"
     * - UI format: "16-Jun-2025"
     *
     * @param dateStr Date string in various formats
     * @return LocalDate parsed from the string
     * @throws IllegalArgumentException if format is invalid
     */
    public static LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String s = dateStr.trim();

        // Extract first 10 chars if longer (for datetime strings)
        if (s.length() > 10) {
            s = s.substring(0, 10);
        }

        // Try ISO format first
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // Try UI format
        }

        // Try UI format: "16-Jun-2025"
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        } catch (DateTimeParseException ignored) {
            // All formats failed
        }

        throw new IllegalArgumentException("Invalid date format: " + dateStr +
                ". Supported formats: yyyy-MM-dd or dd-MMM-yyyy");
    }
}


