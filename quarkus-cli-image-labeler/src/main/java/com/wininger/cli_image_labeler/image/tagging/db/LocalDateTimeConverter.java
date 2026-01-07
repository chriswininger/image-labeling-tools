package com.wininger.cli_image_labeler.image.tagging.db;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converts LocalDateTime to/from TEXT (ISO format) for SQLite storage.
 * TEXT is the recommended format for dates in SQLite as it's human-readable
 * and works well with SQLite's date/time functions.
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // Handle both ISO format and Unix timestamp (for existing data migration)
        try {
            // Try parsing as ISO format first
            if (dbData.contains("-") && dbData.contains(":")) {
                return LocalDateTime.parse(dbData, FORMATTER);
            }
            // If it's a Unix timestamp (milliseconds), convert it
            long timestamp = Long.parseLong(dbData);
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse date: " + dbData, e);
        }
    }
}

