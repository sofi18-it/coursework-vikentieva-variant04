package ua.edu.nupp.kpp.client;

/**
 * Помилка взаємодії з сервером.
 * status = 0 означає, що сервер недоступний (немає з'єднання);
 * 4xx — запит відхилено сервером (валідація, дублікат, неіснуючий пов'язаний запис);
 * 5xx — внутрішня помилка сервера.
 */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() { return status; }

    public boolean isServerUnavailable() { return status == 0; }
}
