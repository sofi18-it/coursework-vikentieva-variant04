package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.nupp.kpp.server.model.Edition;
import ua.edu.nupp.kpp.server.model.Loan;
import ua.edu.nupp.kpp.server.model.Reader;
import ua.edu.nupp.kpp.server.model.Reservation;
import ua.edu.nupp.kpp.server.model.ReservationState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульні тести предметної логіки варіанта №4.
 * Виконуються без JavaFX, Tomcat і MySQL: перевіряються Service-методи, що не звертаються до БД.
 */
class CourseworkTest {

    private static Edition edition(int total, int available, int requests) {
        return new Edition(1L, "978-617-12-0001-1", "Кобзар", "Тарас Шевченко", 2020, total, available, requests);
    }

    // ---------- Інтегральний показник і нормування факторів ----------

    @Test
    @DisplayName("1. F1: 12 днів очікування → 40")
    void waitingFactorIsNormalized() {
        assertEquals(40.0, PriorityCalculator.factorWaiting(12), 0.001);
    }

    @Test
    @DisplayName("2. F1 обмежується значенням 100 для очікування понад 30 днів")
    void waitingFactorIsCappedAt100() {
        assertEquals(100.0, PriorityCalculator.factorWaiting(45), 0.001);
        assertEquals(0.0, PriorityCalculator.factorWaiting(0), 0.001);
    }

    @Test
    @DisplayName("3. F2: 15 запитів → 75; 40 запитів → 100")
    void demandFactorIsNormalized() {
        assertEquals(75.0, PriorityCalculator.factorDemand(15), 0.001);
        assertEquals(100.0, PriorityCalculator.factorDemand(40), 0.001);
    }

    @Test
    @DisplayName("4. F3: 5 примірників, доступний 1 → дефіцит 80")
    void shortageFactorIsNormalized() {
        assertEquals(80.0, PriorityCalculator.factorShortage(5, 1), 0.001);
        assertEquals(0.0, PriorityCalculator.factorShortage(4, 4), 0.001);
        assertEquals(100.0, PriorityCalculator.factorShortage(3, 0), 0.001);
    }

    @Test
    @DisplayName("5. I = 0,40·40 + 0,35·75 + 0,25·80 = 62,25")
    void integralIndexIsCalculated() {
        assertEquals(62.25, PriorityCalculator.calculate(40, 75, 80), 0.001);
    }

    @Test
    @DisplayName("6. Фактор поза діапазоном 0..100 відхиляється")
    void factorOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(101, 50, 50));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(50, -1, 50));
    }

    @Test
    @DisplayName("7. Недопустимі вхідні дані факторів відхиляються")
    void invalidFactorInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factorWaiting(-3));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factorShortage(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factorShortage(2, 3));
    }

    @Test
    @DisplayName("8. Service формує бронювання зі станом СТВОРЕНО та обчисленим показником")
    void reservationIsCalculatedFromEdition() {
        Reservation r = ReservationService.calculate(null, 7L, edition(5, 1, 15), "2026-11-10", 12,
                ReservationState.CREATED);
        assertEquals(ReservationState.CREATED, r.getState());
        assertEquals(40.0, r.getF1(), 0.001);
        assertEquals(75.0, r.getF2(), 0.001);
        assertEquals(80.0, r.getF3(), 0.001);
        assertEquals(62.25, r.getPriorityIndex(), 0.001);
    }

    // ---------- Життєвий цикл ----------

    @Test
    @DisplayName("9. Дозволені переходи: СТВОРЕНО → ПІДТВЕРДЖЕНО → ГОТОВЕ_ДО_ВИДАЧІ → ЗАВЕРШЕНО")
    void allowedTransitions() {
        assertTrue(ReservationLifecycle.isAllowedTransition(ReservationState.CREATED, ReservationState.CONFIRMED));
        assertTrue(ReservationLifecycle.isAllowedTransition(ReservationState.CONFIRMED, ReservationState.READY_FOR_PICKUP));
        assertTrue(ReservationLifecycle.isAllowedTransition(ReservationState.READY_FOR_PICKUP, ReservationState.COMPLETED));
    }

    @Test
    @DisplayName("10. Заборонені переходи: пропуск стану, повернення назад, вихід із ЗАВЕРШЕНО")
    void forbiddenTransitions() {
        assertFalse(ReservationLifecycle.isAllowedTransition(ReservationState.CREATED, ReservationState.COMPLETED));
        assertFalse(ReservationLifecycle.isAllowedTransition(ReservationState.CREATED, ReservationState.READY_FOR_PICKUP));
        assertFalse(ReservationLifecycle.isAllowedTransition(ReservationState.CONFIRMED, ReservationState.CREATED));
        assertFalse(ReservationLifecycle.isAllowedTransition(ReservationState.COMPLETED, ReservationState.CREATED));
        assertFalse(ReservationLifecycle.isAllowedTransition(ReservationState.CONFIRMED, ReservationState.CONFIRMED));
    }

    @Test
    @DisplayName("11. Заборонений перехід викликає IllegalArgumentException із назвами станів")
    void forbiddenTransitionThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReservationLifecycle.requireAllowed(ReservationState.CREATED, ReservationState.COMPLETED));
        assertTrue(ex.getMessage().contains("СТВОРЕНО"));
        assertTrue(ex.getMessage().contains("ЗАВЕРШЕНО"));
        assertNull(ReservationLifecycle.next(ReservationState.COMPLETED));
        assertEquals(ReservationState.CONFIRMED, ReservationLifecycle.next(ReservationState.CREATED));
    }

    // ---------- Валідація сутностей ----------

    @Test
    @DisplayName("12. Коректне видання проходить валідацію")
    void validEditionPasses() {
        assertDoesNotThrow(() -> EditionService.validate(edition(5, 2, 10)));
    }

    @Test
    @DisplayName("13. Видання з порожньою назвою або доступними > загальних відхиляється")
    void invalidEditionIsRejected() {
        Edition noTitle = edition(5, 2, 10);
        noTitle.setTitle("  ");
        assertThrows(IllegalArgumentException.class, () -> EditionService.validate(noTitle));
        assertThrows(IllegalArgumentException.class, () -> EditionService.validate(edition(2, 5, 10)));
        Edition badIsbn = edition(5, 2, 10);
        badIsbn.setIsbn("abc");
        assertThrows(IllegalArgumentException.class, () -> EditionService.validate(badIsbn));
    }

    @Test
    @DisplayName("14. Читач без номера квитка або з некоректним e-mail відхиляється")
    void invalidReaderIsRejected() {
        assertDoesNotThrow(() -> ReaderService.validate(
                new Reader(null, "RB-0001", "Іваненко Олена", "+380501234567", "olena@example.com")));
        assertThrows(IllegalArgumentException.class, () -> ReaderService.validate(
                new Reader(null, "", "Іваненко Олена", null, null)));
        assertThrows(IllegalArgumentException.class, () -> ReaderService.validate(
                new Reader(null, "RB-0002", "Петренко Іван", null, "not-an-email")));
    }

    @Test
    @DisplayName("15. Бронювання з некоректною датою або від'ємним очікуванням відхиляється")
    void invalidReservationIsRejected() {
        assertDoesNotThrow(() -> ReservationService.validate(1, 1, "2026-11-10", 5));
        assertThrows(IllegalArgumentException.class, () -> ReservationService.validate(1, 1, "10.11.2026", 5));
        assertThrows(IllegalArgumentException.class, () -> ReservationService.validate(1, 1, "2026-11-10", -1));
        assertThrows(IllegalArgumentException.class, () -> ReservationService.validate(0, 1, "2026-11-10", 5));
    }

    @Test
    @DisplayName("16. Видача: строк повернення раніше дати видачі відхиляється")
    void invalidLoanDatesAreRejected() {
        assertDoesNotThrow(() -> LoanService.validate(new Loan(null, 1, "2026-11-15", "2026-11-29", "")));
        assertThrows(IllegalArgumentException.class,
                () -> LoanService.validate(new Loan(null, 1, "2026-11-15", "2026-11-01", null)));
        assertThrows(IllegalArgumentException.class,
                () -> LoanService.validate(new Loan(null, 1, "2026-11-15", "2026-11-20", "2026-11-10")));
    }
}
