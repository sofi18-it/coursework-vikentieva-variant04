package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.model.ReservationState;

/**
 * Правила чотиристанного життєвого циклу бронювання.
 * Дозволено лише послідовний перехід уперед на один крок:
 * CREATED → CONFIRMED → READY_FOR_PICKUP → COMPLETED.
 * Повернення назад, пропуск стану та переходи із ЗАВЕРШЕНО заборонені.
 */
public final class ReservationLifecycle {
    private ReservationLifecycle() {}

    public static boolean isAllowedTransition(ReservationState from, ReservationState to) {
        if (from == null || to == null) return false;
        return to.ordinal() == from.ordinal() + 1;
    }

    /** Наступний допустимий стан або null для кінцевого стану. */
    public static ReservationState next(ReservationState from) {
        ReservationState[] all = ReservationState.values();
        return from == null || from.ordinal() == all.length - 1 ? null : all[from.ordinal() + 1];
    }

    public static void requireAllowed(ReservationState from, ReservationState to) {
        if (!isAllowedTransition(from, to)) {
            throw new IllegalArgumentException("Недопустимий перехід стану: "
                    + label(from) + " → " + label(to));
        }
    }

    private static String label(ReservationState s) {
        return s == null ? "?" : s.getLabel();
    }
}
