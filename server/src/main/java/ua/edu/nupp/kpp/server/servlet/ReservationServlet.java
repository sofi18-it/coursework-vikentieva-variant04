package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.ReservationState;
import ua.edu.nupp.kpp.server.service.ReservationService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /reservations, /reservations?id=N;
 * POST /reservations action=add|update|transition|delete.
 * Фактори F1–F3 та показник I обчислюються сервером (ReservationService), клієнт їх не передає.
 */
public class ReservationServlet extends BaseServlet {
    private final ReservationService service = new ReservationService();

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
                case "add" -> writeOk(resp, "Бронювання створено",
                        service.add(requiredLong(req, "readerId"), requiredLong(req, "editionId"),
                                str(req, "reservationDate"), requiredInt(req, "waitDays")));
                case "update" -> {
                    long id = requiredLong(req, "id");
                    service.update(id, requiredLong(req, "readerId"), requiredLong(req, "editionId"),
                            str(req, "reservationDate"), requiredInt(req, "waitDays"));
                    writeOk(resp, "Бронювання оновлено, показник перераховано", id);
                }
                case "transition" -> {
                    long id = requiredLong(req, "id");
                    service.changeState(id, state(req));
                    writeOk(resp, "Стан бронювання змінено", id);
                }
                case "delete" -> {
                    long id = requiredLong(req, "id");
                    service.delete(id);
                    writeOk(resp, "Бронювання видалено", id);
                }
                default -> throw new IllegalArgumentException("Невідома операція");
            }
        } catch (Exception e) {
            writeError(req, resp, e);
        }
    }

    private ReservationState state(HttpServletRequest req) {
        String v = str(req, "state");
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("Не задано новий стан бронювання");
        try {
            return ReservationState.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невідомий стан бронювання: " + v);
        }
    }
}
