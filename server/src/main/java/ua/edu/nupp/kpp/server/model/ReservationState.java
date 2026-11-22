package ua.edu.nupp.kpp.server.model;

/**
 * Чотири стани життєвого циклу бронювання:
 * СТВОРЕНО → ПІДТВЕРДЖЕНО → ГОТОВЕ_ДО_ВИДАЧІ → ЗАВЕРШЕНО.
 */
public enum ReservationState {
    CREATED("СТВОРЕНО"),
    CONFIRMED("ПІДТВЕРДЖЕНО"),
    READY_FOR_PICKUP("ГОТОВЕ_ДО_ВИДАЧІ"),
    COMPLETED("ЗАВЕРШЕНО");

    private final String label;

    ReservationState(String label) { this.label = label; }

    public String getLabel() { return label; }
}
