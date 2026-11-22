package ua.edu.nupp.kpp.server.model;

/** Видача видання за бронюванням (таблиця loan). returnDate може бути null, доки видання не повернуто. */
public class Loan {
    private Long id;
    private long reservationId;
    private String issueDate;
    private String dueDate;
    private String returnDate;

    public Loan() {}

    public Loan(Long id, long reservationId, String issueDate, String dueDate, String returnDate) {
        this.id = id; this.reservationId = reservationId; this.issueDate = issueDate;
        this.dueDate = dueDate; this.returnDate = returnDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getReservationId() { return reservationId; }
    public void setReservationId(long reservationId) { this.reservationId = reservationId; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
}
