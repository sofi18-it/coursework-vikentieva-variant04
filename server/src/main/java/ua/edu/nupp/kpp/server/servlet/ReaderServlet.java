package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.Reader;
import ua.edu.nupp.kpp.server.service.ReaderService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /readers, /readers?id=N;
 * POST /readers action=add|update|delete.
 */
public class ReaderServlet extends BaseServlet {
    private final ReaderService service = new ReaderService();

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
                case "add" -> writeOk(resp, "Читача додано", service.add(read(req, null)));
                case "update" -> {
                    long id = requiredLong(req, "id");
                    service.update(read(req, id));
                    writeOk(resp, "Дані читача оновлено", id);
                }
                case "delete" -> {
                    long id = requiredLong(req, "id");
                    service.delete(id);
                    writeOk(resp, "Читача видалено", id);
                }
                default -> throw new IllegalArgumentException("Невідома операція");
            }
        } catch (Exception e) {
            writeError(req, resp, e);
        }
    }

    private Reader read(HttpServletRequest req, Long id) {
        return new Reader(id, str(req, "ticketNumber"), str(req, "fullName"), str(req, "phone"), str(req, "email"));
    }
}
