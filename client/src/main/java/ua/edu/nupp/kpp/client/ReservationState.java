package ua.edu.nupp.kpp.client;

/** Стани бронювання (назви збігаються із серверним enum для JSON-обміну). */
public enum ReservationState {
    CREATED("СТВОРЕНО"),
    CONFIRMED("ПІДТВЕРДЖЕНО"),
    READY_FOR_PICKUP("ГОТОВЕ_ДО_ВИДАЧІ"),
    COMPLETED("ЗАВЕРШЕНО");

    private final String label;

    ReservationState(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
