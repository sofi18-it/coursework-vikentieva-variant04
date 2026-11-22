package ua.edu.nupp.kpp.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Клієнтська модель видання: лише дані для відображення, без DAO/JDBC. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edition {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private int publishYear;
    private int totalCopies;
    private int availableCopies;
    private int requestCount;

    public Edition() {}

    public Edition(Long id, String isbn, String title, String author, int publishYear,
                   int totalCopies, int availableCopies, int requestCount) {
        this.id = id; this.isbn = isbn; this.title = title; this.author = author;
        this.publishYear = publishYear; this.totalCopies = totalCopies;
        this.availableCopies = availableCopies; this.requestCount = requestCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getPublishYear() { return publishYear; }
    public void setPublishYear(int publishYear) { this.publishYear = publishYear; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }

    @Override
    public String toString() {
        return "#" + id + " " + title + " — " + author;
    }
}
