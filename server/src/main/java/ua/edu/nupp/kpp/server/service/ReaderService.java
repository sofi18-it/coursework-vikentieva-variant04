package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.ReaderDAO;
import ua.edu.nupp.kpp.server.model.Reader;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static ua.edu.nupp.kpp.server.service.Validation.blank;
import static ua.edu.nupp.kpp.server.service.Validation.require;

/** Предметна логіка читачів. */
public class ReaderService {
    private static final Logger LOG = Logger.getLogger(ReaderService.class.getName());
    private final ReaderDAO dao = new ReaderDAO();

    public List<Reader> getAll() throws SQLException {
        return dao.getAll();
    }

    public Reader getById(long id) throws SQLException {
        Reader r = dao.getById(id);
        require(r != null, "Читача з id=" + id + " не знайдено");
        return r;
    }

    public long add(Reader r) throws SQLException {
        validate(r);
        long id = dao.add(r);
        LOG.info("Додано читача id=" + id + ", квиток " + r.getTicketNumber());
        return id;
    }

    public void update(Reader r) throws SQLException {
        require(r != null && r.getId() != null, "Не задано id читача");
        getById(r.getId());
        validate(r);
        dao.update(r);
        LOG.info("Оновлено читача id=" + r.getId());
    }

    public void delete(long id) throws SQLException {
        getById(id);
        dao.delete(id);
        LOG.info("Видалено читача id=" + id);
    }

    /** Правила валідації читача. Телефон і e-mail необов'язкові, але мають бути коректними. */
    static void validate(Reader r) {
        require(r != null, "Дані читача відсутні");
        require(!blank(r.getTicketNumber()), "Номер читацького квитка є обов'язковим");
        require(r.getTicketNumber().trim().length() <= 20, "Номер квитка — не більше 20 символів");
        require(!blank(r.getFullName()), "ПІБ читача є обов'язковим");
        require(blank(r.getPhone()) || r.getPhone().trim().matches("\\+?[0-9 ()-]{7,20}"),
                "Телефон має містити 7–20 цифр (допускаються +, пробіл, дужки, дефіс)");
        require(blank(r.getEmail()) || r.getEmail().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"),
                "Некоректна адреса e-mail");
    }
}
