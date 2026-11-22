package ua.edu.nupp.kpp.client;

import javafx.concurrent.Task;

import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Фоновий імпорт каталогу видань із CSV через JavaFX Task.
 * Читання файла й надсилання записів на сервер виконуються поза потоком JavaFX,
 * тому інтерфейс не блокується. Прогрес передається через updateProgress/updateMessage.
 */
public class ImportEditionsTask extends Task<ImportEditionsTask.Result> {
    private static final Logger LOG = Logger.getLogger(ImportEditionsTask.class.getName());

    private final Path file;
    private final LibraryApiClient api;

    /** Підсумок імпорту. */
    public record Result(int total, int imported, List<String> errors) {
        public int skipped() { return total - imported; }
    }

    public ImportEditionsTask(Path file, LibraryApiClient api) {
        this.file = file;
        this.api = api;
    }

    @Override
    protected Result call() throws Exception {
        LOG.info("Початок CSV-імпорту видань: " + file.getFileName());
        updateMessage("Читання файла " + file.getFileName() + "…");
        updateProgress(0, 1);

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            LOG.warning("CSV-файл не знайдено: " + file);
            throw new IllegalStateException("CSV-файл не знайдено: " + file);
        } catch (MalformedInputException e) {
            LOG.warning("Файл не в кодуванні UTF-8: " + file.getFileName());
            throw new IllegalStateException("Файл має бути збережений у кодуванні UTF-8");
        }

        List<EditionCsvParser.Row> rows;
        try {
            rows = EditionCsvParser.parse(lines);
        } catch (IllegalArgumentException e) {
            LOG.warning("CSV відхилено: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }

        int total = rows.size();
        int imported = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (isCancelled()) {
                LOG.info("Імпорт скасовано користувачем");
                break;
            }
            EditionCsvParser.Row row = rows.get(i);
            if (!row.ok()) {
                skip(errors, row.lineNumber(), row.error());
            } else {
                try {
                    api.addEdition(row.edition());
                    imported++;
                } catch (ApiException e) {
                    if (e.isServerUnavailable()) {
                        LOG.severe("Імпорт перервано — сервер недоступний. Імпортовано " + imported + " із " + total);
                        throw new IllegalStateException(e.getMessage()
                                + "\nДо переривання імпортовано записів: " + imported);
                    }
                    skip(errors, row.lineNumber(), e.getMessage());
                }
            }
            updateProgress(i + 1, total);
            updateMessage("Опрацьовано " + (i + 1) + " із " + total + "; імпортовано: " + imported);
        }

        LOG.info("Завершено CSV-імпорт: рядків " + total + ", імпортовано " + imported
                + ", пропущено " + (total - imported));
        return new Result(total, imported, errors);
    }

    private void skip(List<String> errors, int line, String reason) {
        String msg = "Рядок " + line + ": " + reason;
        errors.add(msg);
        LOG.warning("Пропущено. " + msg);
    }
}
