package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Edition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO для таблиці edition. Усі SQL-запити видань зосереджені тут. */
public class EditionDAO {
    private static final String COLUMNS =
            "id,isbn,title,author,publish_year,total_copies,available_copies,request_count";

    public long add(Edition e) throws SQLException {
        String sql = "INSERT INTO edition(isbn,title,author,publish_year,total_copies,available_copies,request_count) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, e);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public List<Edition> getAll() throws SQLException {
        List<Edition> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM edition ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public Edition getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM edition WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public Edition getByIsbn(String isbn) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM edition WHERE isbn=?")) {
            p.setString(1, isbn);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public void update(Edition e) throws SQLException {
        String sql = "UPDATE edition SET isbn=?,title=?,author=?,publish_year=?,total_copies=?,"
                + "available_copies=?,request_count=? WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, e);
            p.setLong(8, e.getId());
            p.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM edition WHERE id=?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private void bind(PreparedStatement p, Edition e) throws SQLException {
        p.setString(1, e.getIsbn());
        p.setString(2, e.getTitle());
        p.setString(3, e.getAuthor());
        p.setInt(4, e.getPublishYear());
        p.setInt(5, e.getTotalCopies());
        p.setInt(6, e.getAvailableCopies());
        p.setInt(7, e.getRequestCount());
    }

    private Edition map(ResultSet r) throws SQLException {
        return new Edition(r.getLong("id"), r.getString("isbn"), r.getString("title"), r.getString("author"),
                r.getInt("publish_year"), r.getInt("total_copies"), r.getInt("available_copies"),
                r.getInt("request_count"));
    }
}
