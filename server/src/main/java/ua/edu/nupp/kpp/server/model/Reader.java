package ua.edu.nupp.kpp.server.model;

/** Читач бібліотеки (таблиця reader). */
public class Reader {
    private Long id;
    private String ticketNumber;
    private String fullName;
    private String phone;
    private String email;

    public Reader() {}

    public Reader(Long id, String ticketNumber, String fullName, String phone, String email) {
        this.id = id; this.ticketNumber = ticketNumber; this.fullName = fullName;
        this.phone = phone; this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
