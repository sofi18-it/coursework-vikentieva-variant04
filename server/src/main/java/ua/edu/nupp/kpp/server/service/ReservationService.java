package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.EditionDAO;
import ua.edu.nupp.kpp.server.dao.ReaderDAO;
import ua.edu.nupp.kpp.server.dao.ReservationDAO;
import ua.edu.nupp.kpp.server.model.Edition;
import ua.edu.nupp.kpp.server.model.Reservation;
import ua.edu.nupp.kpp.server.model.ReservationState;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static ua.edu.nupp.kpp.server.service.Validation.require;

/**
 * Предметна логіка бронювань: перевірка пов'язаних записів, розрахунок факторів F1–F3
 * та інтегрального показника пріоритету, контроль життєвого циклу.
 */
public class ReservationService {
    private static final Logger LOG = Logger.getLogger(ReservationService.class.getName());
    static final int MAX_WAIT_DAYS_INPUT = 365;

    private final ReservationDAO dao = new ReservationDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final EditionDAO editionDAO = new EditionDAO();

    public List<Reservation> getAll() throws SQLException {
        return dao.getAll();
    }

    public Reservation getById(long id) throws SQLException {
        Reservation r = dao.getById(id);
        require(r != null, "Бронювання з id=" + id + " не знайдено");
        return r;
    }

    public long add(long readerId, long editionId, String reservationDate, int waitDays) throws SQLException {
        Reservation r = build(null, readerId, editionId, reservationDate, waitDays, ReservationState.CREATED);
        long id = dao.add(r);
        LOG.info("Створено бронювання id=" + id + " (I=" + r.getPriorityIndex() + ")");
        return id;
    }

    /** Редагування з повторним розрахунком факторів; стан бронювання зберігається. */
    public void update(long id, long readerId, long editionId, String reservationDate, int waitDays)
            throws SQLException {
        Reservation old = getById(id);
        require(old.getState() != ReservationState.COMPLETED, "Завершене бронювання не можна редагувати");
        dao.update(build(id, readerId, editionId, reservationDate, waitDays, old.getState()));
        LOG.info("Оновлено бронювання id=" + id);
    }

    public void changeState(long id, ReservationState next) throws SQLException {
        Reservation current = getById(id);
        ReservationLifecycle.requireAllowed(current.getState(), next);
        if (next == ReservationState.READY_FOR_PICKUP) {
            Edition e = editionDAO.getById(current.getEditionId());
            require(e != null && e.getAvailableCopies() > 0,
                    "Немає доступних примірників видання — бронювання не може бути готовим до видачі");
        }
        dao.updateState(id, next);
        LOG.info("Бронювання id=" + id + ": " + current.getState().getLabel() + " → " + next.getLabel());
    }

    public void delete(long id) throws SQLException {
        getById(id);
        dao.delete(id);
        LOG.info("Видалено бронювання id=" + id);
    }

    private Reservation build(Long id, long readerId, long editionId, String reservationDate, int waitDays,
                              ReservationState state) throws SQLException {
        validate(readerId, editionId, reservationDate, waitDays);
        require(readerDAO.getById(readerId) != null, "Читача з id=" + readerId + " не знайдено");
        Edition edition = editionDAO.getById(editionId);
        require(edition != null, "Видання з id=" + editionId + " не знайдено");
        return calculate(id, readerId, edition, reservationDate.trim(), waitDays, state);
    }

    /** Обчислення факторів і показника для бронювання (без звернення до БД). */
    static Reservation calculate(Long id, long readerId, Edition edition, String reservationDate,
                                 int waitDays, ReservationState state) {
        double f1 = PriorityCalculator.factorWaiting(waitDays);
        double f2 = PriorityCalculator.factorDemand(edition.getRequestCount());
        double f3 = PriorityCalculator.factorShortage(edition.getTotalCopies(), edition.getAvailableCopies());
        double index = PriorityCalculator.calculate(f1, f2, f3);
        return new Reservation(id, readerId, edition.getId(), reservationDate, waitDays, state, f1, f2, f3, index);
    }

    static void validate(long readerId, long editionId, String reservationDate, int waitDays) {
        require(readerId > 0, "Оберіть читача");
        require(editionId > 0, "Оберіть видання");
        Validation.date(reservationDate, "Дата бронювання");
        require(waitDays >= 0 && waitDays <= MAX_WAIT_DAYS_INPUT,
                "Тривалість очікування має бути в діапазоні 0.." + MAX_WAIT_DAYS_INPUT + " днів");
    }
}
