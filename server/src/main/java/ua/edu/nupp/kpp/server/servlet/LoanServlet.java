package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.Loan;
import ua.edu.nupp.kpp.server.service.LoanService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /loans, /loans?id=N;
 * POST /loans action=add|update|delete.
 */
public class LoanServlet extends BaseServlet {
    private final LoanService service = new LoanService();

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
                case "add" -> writeOk(resp, "Видачу оформлено, бронювання завершено", service.add(read(req, null)));
                case "update" -> {
                    long id = requiredLong(req, "id");
                    service.update(read(req, id));
                    writeOk(resp, "Видачу оновлено", id);
                }
                case "delete" -> {
                    long id = requiredLong(req, "id");
                    service.delete(id);
                    writeOk(resp, "Видачу видалено", id);
                }
                default -> throw new IllegalArgumentException("Невідома операція");
            }
        } catch (Exception e) {
            writeError(req, resp, e);
        }
    }

    private Loan read(HttpServletRequest req, Long id) {
        return new Loan(id, requiredLong(req, "reservationId"), str(req, "issueDate"),
                str(req, "dueDate"), str(req, "returnDate"));
    }
}
