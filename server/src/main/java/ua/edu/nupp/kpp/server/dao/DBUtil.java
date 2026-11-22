package ua.edu.nupp.kpp.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Створення JDBC-з'єднання з MySQL.
 * Параметри підключення можна перевизначити змінними середовища
 * LIBRARY_DB_URL, LIBRARY_DB_USER, LIBRARY_DB_PASSWORD, щоб не зберігати пароль у GitHub.
 * За замовчуванням використовується локальний MySQL/XAMPP (root без пароля).
 */
public final class DBUtil {
    private static final Logger LOG = Logger.getLogger(DBUtil.class.getName());

    private static final String URL = env("LIBRARY_DB_URL",
            "jdbc:mysql://localhost:3306/library_reservations?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Kyiv");
    private static final String USER = env("LIBRARY_DB_USER", "root");
    private static final String PASSWORD = env("LIBRARY_DB_PASSWORD", "");

    static {
        try {
            // У Tomcat драйвер з WEB-INF/lib варто зареєструвати явно
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "MySQL JDBC-драйвер не знайдено", e);
        }
    }

    private DBUtil() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /** Ручна перевірка з'єднання (етап I): запустити main у IDE. */
    public static void main(String[] args) throws SQLException {
        try (Connection c = getConnection()) {
            System.out.println("З'єднання з MySQL встановлено: " + c.getMetaData().getURL());
        }
    }
}
