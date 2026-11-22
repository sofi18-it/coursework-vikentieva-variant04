package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Loan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO для таблиці loan. */
public class LoanDAO {
    private static final String COLUMNS = "id,reservation_id,issue_date,due_date,return_date";

    public long add(Loan l) throws SQLException {
        String sql = "INSERT INTO loan(reservation_id,issue_date,due_date,return_date) VALUES(?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, l);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public List<Loan> getAll() throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM loan ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public Loan getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM loan WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public Loan getByReservationId(long reservationId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM loan WHERE reservation_id=?")) {
            p.setLong(1, reservationId);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public void update(Loan l) throws SQLException {
        String sql = "UPDATE loan SET reservation_id=?,issue_date=?,due_date=?,return_date=? WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, l);
            p.setLong(5, l.getId());
            p.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM loan WHERE id=?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private void bind(PreparedStatement p, Loan l) throws SQLException {
        p.setLong(1, l.getReservationId());
        p.setString(2, l.getIssueDate());
        p.setString(3, l.getDueDate());
        if (l.getReturnDate() == null || l.getReturnDate().isBlank()) {
            p.setNull(4, Types.DATE);
        } else {
            p.setString(4, l.getReturnDate());
        }
    }

    private Loan map(ResultSet r) throws SQLException {
        return new Loan(r.getLong("id"), r.getLong("reservation_id"), r.getString("issue_date"),
                r.getString("due_date"), r.getString("return_date"));
    }
}
