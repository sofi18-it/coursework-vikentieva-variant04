package ua.edu.nupp.kpp.server.model;

/**
 * Бронювання видання читачем (таблиця reservation) — об'єкт життєвого циклу.
 * f1, f2, f3 та priorityIndex обчислюються на Service-рівні.
 */
public class Reservation {
    private Long id;
    private long readerId;
    private long editionId;
    private String reservationDate;
    private int waitDays;
    private ReservationState state;
    private double f1;
    private double f2;
    private double f3;
    private double priorityIndex;

    public Reservation() {}

    public Reservation(Long id, long readerId, long editionId, String reservationDate, int waitDays,
                       ReservationState state, double f1, double f2, double f3, double priorityIndex) {
        this.id = id; this.readerId = readerId; this.editionId = editionId;
        this.reservationDate = reservationDate; this.waitDays = waitDays; this.state = state;
        this.f1 = f1; this.f2 = f2; this.f3 = f3; this.priorityIndex = priorityIndex;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getReaderId() { return readerId; }
    public void setReaderId(long readerId) { this.readerId = readerId; }
    public long getEditionId() { return editionId; }
    public void setEditionId(long editionId) { this.editionId = editionId; }
    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }
    public int getWaitDays() { return waitDays; }
    public void setWaitDays(int waitDays) { this.waitDays = waitDays; }
    public ReservationState getState() { return state; }
    public void setState(ReservationState state) { this.state = state; }
    public double getF1() { return f1; }
    public void setF1(double f1) { this.f1 = f1; }
    public double getF2() { return f2; }
    public void setF2(double f2) { this.f2 = f2; }
    public double getF3() { return f3; }
    public void setF3(double f3) { this.f3 = f3; }
    public double getPriorityIndex() { return priorityIndex; }
    public void setPriorityIndex(double priorityIndex) { this.priorityIndex = priorityIndex; }
}
