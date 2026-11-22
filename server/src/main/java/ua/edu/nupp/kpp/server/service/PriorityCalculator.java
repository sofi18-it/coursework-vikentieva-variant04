package ua.edu.nupp.kpp.server.service;

/**
 * Обчислення інтегрального показника пріоритету бронювання
 * I = 0,40·F1 + 0,35·F2 + 0,25·F3, де всі фактори нормовані до діапазону 0–100.
 *
 * F1 — тривалість очікування: waitDays / 30 днів · 100 (обмежується 100);
 * F2 — попит на видання: requestCount / 20 запитів · 100 (обмежується 100);
 * F3 — дефіцит доступних примірників: (total − available) / total · 100.
 */
public final class PriorityCalculator {
    public static final double W1 = 0.40;
    public static final double W2 = 0.35;
    public static final double W3 = 0.25;
    /** Строк очікування, що відповідає максимальному значенню F1 = 100. */
    public static final int MAX_WAIT_DAYS = 30;
    /** Кількість запитів, що відповідає максимальному значенню F2 = 100. */
    public static final int MAX_REQUESTS = 20;

    private PriorityCalculator() {}

    public static double factorWaiting(int waitDays) {
        if (waitDays < 0) throw new IllegalArgumentException("Тривалість очікування не може бути від'ємною");
        return round(Math.min(100.0, waitDays * 100.0 / MAX_WAIT_DAYS));
    }

    public static double factorDemand(int requestCount) {
        if (requestCount < 0) throw new IllegalArgumentException("Кількість запитів не може бути від'ємною");
        return round(Math.min(100.0, requestCount * 100.0 / MAX_REQUESTS));
    }

    public static double factorShortage(int totalCopies, int availableCopies) {
        if (totalCopies <= 0) throw new IllegalArgumentException("Загальна кількість примірників має бути більшою за 0");
        if (availableCopies < 0 || availableCopies > totalCopies)
            throw new IllegalArgumentException("Кількість доступних примірників має бути в діапазоні 0.." + totalCopies);
        return round((totalCopies - availableCopies) * 100.0 / totalCopies);
    }

    public static double calculate(double f1, double f2, double f3) {
        check(f1, "F1");
        check(f2, "F2");
        check(f3, "F3");
        return round(W1 * f1 + W2 * f2 + W3 * f3);
    }

    private static void check(double value, String name) {
        if (Double.isNaN(value) || value < 0 || value > 100)
            throw new IllegalArgumentException("Фактор " + name + " має бути в діапазоні 0..100");
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
