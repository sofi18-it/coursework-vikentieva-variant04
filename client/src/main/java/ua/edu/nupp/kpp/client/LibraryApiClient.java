package ua.edu.nupp.kpp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP-клієнт JavaFX-застосунку: надсилає запити до Servlet, отримує JSON
 * і перетворює його на клієнтські Java-об'єкти за допомогою ObjectMapper.
 * Не містить SQL і не працює з MySQL безпосередньо.
 *
 * Адресу сервера можна змінити параметром JVM -Dlibrary.server=http://host:port/library-server
 */
public class LibraryApiClient {
    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public LibraryApiClient() {
        this(System.getProperty("library.server", "http://localhost:8080/library-server"));
    }

    public LibraryApiClient(String baseUrl) {
        this.base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getBaseUrl() { return base; }

    // ---------- Видання ----------
    public List<Edition> getEditions() { return list("/editions", Edition[].class); }

    public long addEdition(Edition e) { return post("/editions", editionForm("add", e)); }

    public void updateEdition(Edition e) {
        Map<String, Object> f = editionForm("update", e);
        f.put("id", e.getId());
        post("/editions", f);
    }

    public void deleteEdition(long id) { post("/editions", form("action", "delete", "id", id)); }

    // ---------- Читачі ----------
    public List<Reader> getReaders() { return list("/readers", Reader[].class); }

    public long addReader(Reader r) { return post("/readers", readerForm("add", r)); }

    public void updateReader(Reader r) {
        Map<String, Object> f = readerForm("update", r);
        f.put("id", r.getId());
        post("/readers", f);
    }

    public void deleteReader(long id) { post("/readers", form("action", "delete", "id", id)); }

    // ---------- Бронювання ----------
    public List<Reservation> getReservations() { return list("/reservations", Reservation[].class); }

    public long addReservation(long readerId, long editionId, String date, int waitDays) {
        return post("/reservations", form("action", "add", "readerId", readerId, "editionId", editionId,
                "reservationDate", date, "waitDays", waitDays));
    }

    public void updateReservation(long id, long readerId, long editionId, String date, int waitDays) {
        post("/reservations", form("action", "update", "id", id, "readerId", readerId, "editionId", editionId,
                "reservationDate", date, "waitDays", waitDays));
    }

    public void changeReservationState(long id, ReservationState state) {
        post("/reservations", form("action", "transition", "id", id, "state", state.name()));
    }

    public void deleteReservation(long id) { post("/reservations", form("action", "delete", "id", id)); }

    // ---------- Видачі ----------
    public List<Loan> getLoans() { return list("/loans", Loan[].class); }

    public long addLoan(Loan l) { return post("/loans", loanForm("add", l)); }

    public void updateLoan(Loan l) {
        Map<String, Object> f = loanForm("update", l);
        f.put("id", l.getId());
        post("/loans", f);
    }

    public void deleteLoan(long id) { post("/loans", form("action", "delete", "id", id)); }

    // ---------- Формування параметрів ----------
    private Map<String, Object> editionForm(String action, Edition e) {
        return form("action", action, "isbn", e.getIsbn(), "title", e.getTitle(), "author", e.getAuthor(),
                "publishYear", e.getPublishYear(), "totalCopies", e.getTotalCopies(),
                "availableCopies", e.getAvailableCopies(), "requestCount", e.getRequestCount());
    }

    private Map<String, Object> readerForm(String action, Reader r) {
        return form("action", action, "ticketNumber", r.getTicketNumber(), "fullName", r.getFullName(),
                "phone", r.getPhone(), "email", r.getEmail());
    }

    private Map<String, Object> loanForm(String action, Loan l) {
        return form("action", action, "reservationId", l.getReservationId(), "issueDate", l.getIssueDate(),
                "dueDate", l.getDueDate(), "returnDate", l.getReturnDate());
    }

    private static Map<String, Object> form(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }

    // ---------- HTTP ----------
    private <T> List<T> list(String path, Class<T[]> type) {
        String json = send(HttpRequest.newBuilder(URI.create(base + path)).GET()
                .timeout(Duration.ofSeconds(15)).build());
        try {
            return Arrays.asList(mapper.readValue(json, type));
        } catch (IOException e) {
            throw new ApiException(500, "Некоректна JSON-відповідь сервера", e);
        }
    }

    /** POST-запит із параметрами форми; повертає id з відповіді {"status":"ok","id":N} або 0. */
    private long post(String path, Map<String, Object> params) {
        String body = params.entrySet().stream()
                .map(en -> enc(en.getKey()) + "=" + enc(en.getValue() == null ? "" : String.valueOf(en.getValue())))
                .collect(Collectors.joining("&"));
        String json = send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build());
        try {
            JsonNode node = mapper.readTree(json);
            return node.has("id") ? node.get("id").asLong() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private String send(HttpRequest req) {
        HttpResponse<String> r;
        try {
            r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException | HttpTimeoutException e) {
            throw new ApiException(0, "Сервер недоступний: " + base
                    + ". Перевірте, що Tomcat запущено і library-server.war розгорнуто", e);
        } catch (IOException e) {
            throw new ApiException(0, "Помилка мережі: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Запит перервано", e);
        }
        if (r.statusCode() >= 400) {
            throw new ApiException(r.statusCode(), errorMessage(r), null);
        }
        return r.body();
    }

    private String errorMessage(HttpResponse<String> r) {
        try {
            JsonNode node = mapper.readTree(r.body());
            if (node != null && node.has("error")) return node.get("error").asText();
        } catch (IOException ignored) {
            // відповідь не є JSON (наприклад, сторінка 404 Tomcat)
        }
        return "Сервер повернув помилку HTTP " + r.statusCode();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
