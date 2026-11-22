package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Reader;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO для таблиці reader. */
public class ReaderDAO {
    private static final String COLUMNS = "id,ticket_number,full_name,phone,email";

    public long add(Reader rd) throws SQLException {
        String sql = "INSERT INTO reader(ticket_number,full_name,phone,email) VALUES(?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, rd);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public List<Reader> getAll() throws SQLException {
        List<Reader> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM reader ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public Reader getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM reader WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public void update(Reader rd) throws SQLException {
        String sql = "UPDATE reader SET ticket_number=?,full_name=?,phone=?,email=? WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, rd);
            p.setLong(5, rd.getId());
            p.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM reader WHERE id=?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private void bind(PreparedStatement p, Reader rd) throws SQLException {
        p.setString(1, rd.getTicketNumber());
        p.setString(2, rd.getFullName());
        p.setString(3, rd.getPhone());
        p.setString(4, rd.getEmail());
    }

    private Reader map(ResultSet r) throws SQLException {
        return new Reader(r.getLong("id"), r.getString("ticket_number"), r.getString("full_name"),
                r.getString("phone"), r.getString("email"));
    }
}
