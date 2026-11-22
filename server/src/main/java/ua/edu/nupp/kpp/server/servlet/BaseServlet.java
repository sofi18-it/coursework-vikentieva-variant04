package ua.edu.nupp.kpp.server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Спільна частина Servlet: формування JSON-відповідей, обробка помилок і читання параметрів.
 * Servlet не містить SQL-запитів і предметних правил — лише HTTP-рівень.
 */
public abstract class BaseServlet extends HttpServlet {
    protected final Logger log = Logger.getLogger(getClass().getName());
    protected final ObjectMapper mapper = new ObjectMapper();

    protected void writeJson(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    protected void writeJson(HttpServletResponse resp, Object data) throws IOException {
        writeJson(resp, HttpServletResponse.SC_OK, data);
    }

    protected void writeOk(HttpServletResponse resp, String message, Long id) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("message", message);
        if (id != null) body.put("id", id);
        writeJson(resp, body);
    }

    /** Перетворення винятку на зрозумілу JSON-відповідь {"error": "..."} без аварійного завершення сервера. */
    protected void writeError(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        int status;
        String message;
        if (e instanceof IllegalArgumentException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            message = e.getMessage() == null ? "Некоректний запит" : e.getMessage();
            log.warning("Відхилено запит " + req.getMethod() + " " + req.getRequestURI() + ": " + message);
        } else if (e instanceof SQLIntegrityConstraintViolationException) {
            status = HttpServletResponse.SC_CONFLICT;
            message = "Операцію відхилено: порушено цілісність даних "
                    + "(дублікат унікального значення або наявні пов'язані записи)";
            log.warning("Порушення цілісності БД: " + e.getMessage());
        } else {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            message = "Внутрішня помилка сервера. Перевірте підключення до MySQL";
            log.log(Level.SEVERE, "Помилка обробки " + req.getMethod() + " " + req.getRequestURI(), e);
        }
        writeJson(resp, status, Map.of("error", message));
    }

    protected String action(HttpServletRequest req) {
        String a = req.getParameter("action");
        if (a == null || a.isBlank()) throw new IllegalArgumentException("Не задано параметр action");
        return a.trim();
    }

    protected String str(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim();
    }

    protected long requiredLong(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("Не задано параметр " + name);
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути цілим числом");
        }
    }

    protected int requiredInt(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("Не задано параметр " + name);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути цілим числом");
        }
    }

    /** Повертає id з query-рядка або null, якщо параметр не передано. */
    protected Long optionalId(HttpServletRequest req) {
        String v = str(req, "id");
        return v == null || v.isEmpty() ? null : requiredLong(req, "id");
    }

    protected void prepare(HttpServletRequest req) throws IOException {
        req.setCharacterEncoding("UTF-8");
    }
}
