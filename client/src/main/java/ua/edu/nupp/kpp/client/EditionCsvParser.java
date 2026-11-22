package ua.edu.nupp.kpp.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Розбір CSV-каталогу видань (без JavaFX і мережі).
 * Формат: роздільник «;», кодування UTF-8, перший рядок — заголовок
 * isbn;title;author;publish_year;total_copies;available_copies;request_count
 * Предметна валідація (діапазони, унікальність ISBN) виконується на сервері.
 */
public final class EditionCsvParser {
    public static final String SEPARATOR = ";";
    public static final String[] HEADER = {
            "isbn", "title", "author", "publish_year", "total_copies", "available_copies", "request_count"
    };

    private EditionCsvParser() {}

    /** Результат розбору одного рядка: або edition, або error. */
    public record Row(int lineNumber, Edition edition, String error) {
        public boolean ok() { return error == null; }
    }

    public static void checkHeader(String headerLine) {
        if (headerLine == null) throw new IllegalArgumentException("CSV-файл порожній");
        String h = headerLine.startsWith("﻿") ? headerLine.substring(1) : headerLine;
        String[] parts = h.trim().split(SEPARATOR, -1);
        boolean same = parts.length == HEADER.length;
        for (int i = 0; same && i < HEADER.length; i++) {
            same = HEADER[i].equalsIgnoreCase(parts[i].trim());
        }
        if (!same) {
            throw new IllegalArgumentException("Неправильний заголовок CSV. Очікується: " + String.join(SEPARATOR, HEADER));
        }
    }

    /** Розбирає всі рядки даних (після заголовка). Порожні рядки пропускаються. */
    public static List<Row> parse(List<String> lines) {
        if (lines.isEmpty()) throw new IllegalArgumentException("CSV-файл порожній");
        checkHeader(lines.get(0));
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) continue;
            rows.add(parseLine(i + 1, line));
        }
        return rows;
    }

    public static Row parseLine(int lineNumber, String line) {
        String[] p = line.split(SEPARATOR, -1);
        if (p.length != HEADER.length) {
            return new Row(lineNumber, null,
                    "очікується " + HEADER.length + " полів, отримано " + p.length);
        }
        for (int i = 0; i < 3; i++) {
            if (p[i].isBlank()) return new Row(lineNumber, null, "порожнє обов'язкове поле «" + HEADER[i] + "»");
        }
        try {
            Edition e = new Edition(null, p[0].trim(), p[1].trim(), p[2].trim(),
                    integer(p[3], HEADER[3]), integer(p[4], HEADER[4]),
                    integer(p[5], HEADER[5]), integer(p[6], HEADER[6]));
            return new Row(lineNumber, e, null);
        } catch (IllegalArgumentException ex) {
            return new Row(lineNumber, null, ex.getMessage());
        }
    }

    private static int integer(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("поле «" + field + "» має бути цілим числом, отримано «" + value.trim() + "»");
        }
    }
}
