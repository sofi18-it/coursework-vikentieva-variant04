package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Reservation;
import ua.edu.nupp.kpp.server.model.ReservationState;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO для таблиці reservation. */
public class ReservationDAO {
    private static final String COLUMNS =
            "id,reader_id,edition_id,reservation_date,wait_days,state,f1,f2,f3,priority_index";

    public long add(Reservation rs) throws SQLException {
        String sql = "INSERT INTO reservation(reader_id,edition_id,reservation_date,wait_days,state,f1,f2,f3,priority_index) "
                + "VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, rs);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public List<Reservation> getAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM reservation ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    public Reservation getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT " + COLUMNS + " FROM reservation WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    public void update(Reservation rs) throws SQLException {
        String sql = "UPDATE reservation SET reader_id=?,edition_id=?,reservation_date=?,wait_days=?,state=?,"
                + "f1=?,f2=?,f3=?,priority_index=? WHERE id=?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, rs);
            p.setLong(10, rs.getId());
            p.executeUpdate();
        }
    }

    public void updateState(long id, ReservationState state) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE reservation SET state=? WHERE id=?")) {
            p.setString(1, state.name());
            p.setLong(2, id);
            p.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM reservation WHERE id=?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private void bind(PreparedStatement p, Reservation rs) throws SQLException {
        p.setLong(1, rs.getReaderId());
        p.setLong(2, rs.getEditionId());
        p.setString(3, rs.getReservationDate());
        p.setInt(4, rs.getWaitDays());
        p.setString(5, rs.getState().name());
        p.setDouble(6, rs.getF1());
        p.setDouble(7, rs.getF2());
        p.setDouble(8, rs.getF3());
        p.setDouble(9, rs.getPriorityIndex());
    }

    private Reservation map(ResultSet r) throws SQLException {
        return new Reservation(r.getLong("id"), r.getLong("reader_id"), r.getLong("edition_id"),
                r.getString("reservation_date"), r.getInt("wait_days"),
                ReservationState.valueOf(r.getString("state")),
                r.getDouble("f1"), r.getDouble("f2"), r.getDouble("f3"), r.getDouble("priority_index"));
    }
}
