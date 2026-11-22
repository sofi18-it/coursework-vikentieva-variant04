package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.LoanDAO;
import ua.edu.nupp.kpp.server.dao.ReservationDAO;
import ua.edu.nupp.kpp.server.model.Loan;
import ua.edu.nupp.kpp.server.model.Reservation;
import ua.edu.nupp.kpp.server.model.ReservationState;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import static ua.edu.nupp.kpp.server.service.Validation.blank;
import static ua.edu.nupp.kpp.server.service.Validation.require;

/**
 * Предметна логіка видач. Видача можлива лише для бронювання у стані ГОТОВЕ_ДО_ВИДАЧІ;
 * після оформлення видачі бронювання переводиться у стан ЗАВЕРШЕНО.
 */
public class LoanService {
    private static final Logger LOG = Logger.getLogger(LoanService.class.getName());
    private final LoanDAO dao = new LoanDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public List<Loan> getAll() throws SQLException {
        return dao.getAll();
    }

    public Loan getById(long id) throws SQLException {
        Loan l = dao.getById(id);
        require(l != null, "Видачу з id=" + id + " не знайдено");
        return l;
    }

    public long add(Loan l) throws SQLException {
        validate(l);
        Reservation r = reservationDAO.getById(l.getReservationId());
        require(r != null, "Бронювання з id=" + l.getReservationId() + " не знайдено");
        require(r.getState() == ReservationState.READY_FOR_PICKUP,
                "Видача можлива лише для бронювання у стані ГОТОВЕ_ДО_ВИДАЧІ (поточний стан: "
                        + r.getState().getLabel() + ")");
        require(dao.getByReservationId(r.getId()) == null, "Для цього бронювання видачу вже оформлено");
        ReservationLifecycle.requireAllowed(r.getState(), ReservationState.COMPLETED);
        long id = dao.add(l);
        reservationDAO.updateState(r.getId(), ReservationState.COMPLETED);
        LOG.info("Оформлено видачу id=" + id + " за бронюванням id=" + r.getId()
                + "; бронювання переведено у стан ЗАВЕРШЕНО");
        return id;
    }

    public void update(Loan l) throws SQLException {
        require(l != null && l.getId() != null, "Не задано id видачі");
        Loan old = getById(l.getId());
        validate(l);
        require(old.getReservationId() == l.getReservationId(),
                "Бронювання, за яким оформлено видачу, змінювати не можна");
        dao.update(l);
        LOG.info("Оновлено видачу id=" + l.getId());
    }

    public void delete(long id) throws SQLException {
        getById(id);
        dao.delete(id);
        LOG.info("Видалено видачу id=" + id);
    }

    /** Правила валідації видачі: дати у форматі РРРР-ММ-ДД, строк повернення не раніше дати видачі. */
    static void validate(Loan l) {
        require(l != null, "Дані видачі відсутні");
        require(l.getReservationId() > 0, "Оберіть бронювання");
        LocalDate issue = Validation.date(l.getIssueDate(), "Дата видачі");
        LocalDate due = Validation.date(l.getDueDate(), "Повернути до");
        require(!due.isBefore(issue), "Строк повернення не може бути раніше дати видачі");
        require(!due.isAfter(issue.plusDays(90)), "Строк користування — не більше 90 днів");
        if (!blank(l.getReturnDate())) {
            LocalDate ret = Validation.date(l.getReturnDate(), "Дата повернення");
            require(!ret.isBefore(issue), "Дата повернення не може бути раніше дати видачі");
        } else {
            l.setReturnDate(null);
        }
    }
}
