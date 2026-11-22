package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.Edition;
import ua.edu.nupp.kpp.server.service.EditionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /editions          — список видань;
 * GET  /editions?id=N     — одне видання;
 * POST /editions action=add|update|delete.
 */
public class EditionServlet extends BaseServlet {
    private final EditionService service = new EditionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = optionalId(req);
            writeJson(resp, id == null ? service.getAll() : service.getById(id));
        } catch (Exception e) {
            writeError(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        prepare(req);
        try {
            switch (action(req)) {
                case "add" -> writeOk(resp, "Видання додано", service.add(read(req, null)));
                case "update" -> {
                    long id = requiredLong(req, "id");
                    service.update(read(req, id));
                    writeOk(resp, "Видання оновлено", id);
                }
                case "delete" -> {
                    long id = requiredLong(req, "id");
                    service.delete(id);
                    writeOk(resp, "Видання видалено", id);
                }
                default -> throw new IllegalArgumentException("Невідома операція");
            }
        } catch (Exception e) {
            writeError(req, resp, e);
        }
    }

    private Edition read(HttpServletRequest req, Long id) {
        return new Edition(id, str(req, "isbn"), str(req, "title"), str(req, "author"),
                requiredInt(req, "publishYear"), requiredInt(req, "totalCopies"),
                requiredInt(req, "availableCopies"), requiredInt(req, "requestCount"));
    }
}
