package ua.edu.nupp.kpp.server.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Допоміжні перевірки, спільні для Service-класів. */
final class Validation {
    private Validation() {}

    static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    static LocalDate date(String value, String fieldName) {
        require(!blank(value), "Поле «" + fieldName + "» є обов'язковим");
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» має бути датою у форматі РРРР-ММ-ДД");
        }
    }
}
