package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.EditionDAO;
import ua.edu.nupp.kpp.server.model.Edition;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;
import java.util.logging.Logger;

import static ua.edu.nupp.kpp.server.service.Validation.blank;
import static ua.edu.nupp.kpp.server.service.Validation.require;

/** Предметна логіка видань: валідація, контроль унікальності ISBN, виклик DAO. */
public class EditionService {
    private static final Logger LOG = Logger.getLogger(EditionService.class.getName());
    private final EditionDAO dao = new EditionDAO();

    public List<Edition> getAll() throws SQLException {
        return dao.getAll();
    }

    public Edition getById(long id) throws SQLException {
        Edition e = dao.getById(id);
        require(e != null, "Видання з id=" + id + " не знайдено");
        return e;
    }

    public long add(Edition e) throws SQLException {
        normalize(e);
        validate(e);
        require(dao.getByIsbn(e.getIsbn()) == null, "Видання з ISBN " + e.getIsbn() + " уже існує");
        long id = dao.add(e);
        LOG.info("Додано видання id=" + id + ", ISBN=" + e.getIsbn());
        return id;
    }

    public void update(Edition e) throws SQLException {
        require(e != null && e.getId() != null, "Не задано id видання");
        getById(e.getId());
        normalize(e);
        validate(e);
        Edition sameIsbn = dao.getByIsbn(e.getIsbn());
        require(sameIsbn == null || sameIsbn.getId().equals(e.getId()),
                "Видання з ISBN " + e.getIsbn() + " уже існує");
        dao.update(e);
        LOG.info("Оновлено видання id=" + e.getId());
    }

    public void delete(long id) throws SQLException {
        getById(id);
        dao.delete(id);
        LOG.info("Видалено видання id=" + id);
    }

    static void normalize(Edition e) {
        if (e == null) return;
        if (e.getIsbn() != null) e.setIsbn(e.getIsbn().trim());
        if (e.getTitle() != null) e.setTitle(e.getTitle().trim());
        if (e.getAuthor() != null) e.setAuthor(e.getAuthor().trim());
    }

    /** Правила валідації видання (використовуються також у JUnit-тестах). */
    static void validate(Edition e) {
        require(e != null, "Дані видання відсутні");
        require(!blank(e.getIsbn()), "ISBN є обов'язковим");
        require(e.getIsbn().matches("[0-9Xx-]{10,17}"),
                "ISBN має містити 10–17 символів: цифри, дефіс або X");
        require(!blank(e.getTitle()), "Назва видання є обов'язковою");
        require(!blank(e.getAuthor()), "Автор є обов'язковим");
        int currentYear = Year.now().getValue();
        require(e.getPublishYear() >= 1450 && e.getPublishYear() <= currentYear,
                "Рік видання має бути в діапазоні 1450.." + currentYear);
        require(e.getTotalCopies() >= 1, "Загальна кількість примірників має бути не менше 1");
        require(e.getAvailableCopies() >= 0 && e.getAvailableCopies() <= e.getTotalCopies(),
                "Доступних примірників має бути від 0 до " + e.getTotalCopies());
        require(e.getRequestCount() >= 0, "Кількість запитів не може бути від'ємною");
    }
}
