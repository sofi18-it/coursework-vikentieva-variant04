# Інформаційна система бронювання та видачі бібліотечних видань

Курсова робота з дисципліни «Кросплатформене програмування», варіант №4.
Виконала: Вікентьєва Софія Григорівна, група 202-ТН. Керівник: Заїка С. О.
НУ «Полтавська політехніка імені Юрія Кондратюка», 2026–2027 н. р.

Клієнт–серверна система: JavaFX-клієнт працює з сервером через HTTP/JSON, сервер на Servlet API
(Tomcat 9.x) розподіляє логіку між Servlet → Service → DAO, дані зберігаються в MySQL.

**Основні сутності:** Видання (`edition`), Читач (`reader`), Бронювання (`reservation`), Видача (`loan`).

**Життєвий цикл бронювання:** СТВОРЕНО → ПІДТВЕРДЖЕНО → ГОТОВЕ_ДО_ВИДАЧІ → ЗАВЕРШЕНО
(`CREATED → CONFIRMED → READY_FOR_PICKUP → COMPLETED`). Дозволено лише перехід на один крок уперед.
Перехід у ГОТОВЕ_ДО_ВИДАЧІ можливий, якщо є доступні примірники; оформлення видачі автоматично
завершує бронювання.

**Інтегральний показник пріоритету:** `I = 0,40·F1 + 0,35·F2 + 0,25·F3`, де
F1 — тривалість очікування (`waitDays / 30 · 100`), F2 — попит на видання (`requestCount / 20 · 100`),
F3 — дефіцит доступних примірників (`(total − available) / total · 100`). Усі фактори обмежено діапазоном 0–100.

## Технології

Java 21, Maven, JavaFX 21, Servlet API 4.0.1 (`javax.servlet`), Apache Tomcat 9.x, Jackson 2.18,
MySQL 8 / MariaDB (XAMPP), MySQL Connector/J, JUnit 5, java.util.logging.

## Структура

```
library-coursework/
├─ database.sql                 — створення БД library_reservations + демонстраційні дані
├─ docs/database-schema.drawio  — схема БД (diagrams.net)
├─ server/                      — серверний Maven-проєкт (WAR)
│  ├─ pom.xml
│  └─ src/main/java/ua/edu/nupp/kpp/server/
│     ├─ model/    Edition, Reader, Reservation, ReservationState, Loan
│     ├─ dao/      DBUtil, EditionDAO, ReaderDAO, ReservationDAO, LoanDAO
│     ├─ service/  EditionService, ReaderService, ReservationService, LoanService,
│     │            PriorityCalculator, ReservationLifecycle, Validation
│     └─ servlet/  BaseServlet, EditionServlet, ReaderServlet, ReservationServlet, LoanServlet
│  ├─ src/main/webapp/WEB-INF/web.xml
│  └─ src/test/java/.../service/CourseworkTest.java  — 16 тестів JUnit 5
└─ client/                      — JavaFX-клієнт
   ├─ pom.xml
   ├─ sample-data/editions.csv (+ editions_errors.csv, editions_bad_header.csv для перевірки помилок)
   └─ src/main/java/ua/edu/nupp/kpp/client/
      ├─ Edition, Reader, Reservation, ReservationState, Loan — клієнтські моделі
      ├─ LibraryApiClient, ApiException — HTTP-клієнт
      ├─ EditionCsvParser, ImportEditionsTask — фоновий CSV-імпорт
      └─ MainApp — графічний інтерфейс
```

## 1. Підготовка MySQL

1. Запустити MySQL (XAMPP Control Panel → MySQL → Start).
2. Відкрити phpMyAdmin → вкладка **SQL** → вставити вміст `database.sql` → **Виконати**.
3. Перевірити, що створено БД `library_reservations` з таблицями `edition`, `reader`, `reservation`, `loan`.

За замовчуванням сервер підключається до `localhost:3306` користувачем `root` без пароля.
Інші параметри задаються змінними середовища (пароль у репозиторій не записується):

```
LIBRARY_DB_URL=jdbc:mysql://localhost:3306/library_reservations
LIBRARY_DB_USER=root
LIBRARY_DB_PASSWORD=...
```

## 2. Складання і запуск сервера

```bash
cd server
mvn clean package
```

Скопіювати `server/target/library-server.war` до каталогу `webapps` Apache Tomcat 9.x і запустити Tomcat
(`bin/startup.bat` або `bin/startup.sh`). Перевірка в браузері:

| Маршрут | Метод | Параметри | Відповідь |
|---|---|---|---|
| `/library-server/editions` | GET | `id` (необов'язковий) | масив/об'єкт видань |
| `/library-server/editions` | POST | `action=add\|update\|delete`, `id`, `isbn`, `title`, `author`, `publishYear`, `totalCopies`, `availableCopies`, `requestCount` | `{"status":"ok","message":..,"id":..}` |
| `/library-server/readers` | GET / POST | `action`, `id`, `ticketNumber`, `fullName`, `phone`, `email` | те саме |
| `/library-server/reservations` | GET / POST | `action=add\|update\|transition\|delete`, `id`, `readerId`, `editionId`, `reservationDate`, `waitDays`, `state` | те саме |
| `/library-server/loans` | GET / POST | `action`, `id`, `reservationId`, `issueDate`, `dueDate`, `returnDate` | те саме |

Помилки повертаються як `{"error":"..."}` зі статусом 400 (валідація, недопустимий перехід,
неіснуючий пов'язаний запис), 409 (дублікат / наявні пов'язані записи) або 500.

Приклад перевірки з терміналу:

```bash
curl http://localhost:8080/library-server/reservations
curl -d "action=transition&id=1&state=COMPLETED" http://localhost:8080/library-server/reservations
```

## 3. Запуск JavaFX-клієнта

```bash
cd client
mvn clean javafx:run
```

Клієнт запускається з каталогу `client`, тому CSV за замовчуванням знаходиться за відносним шляхом
`sample-data/editions.csv`. Інший файл можна обрати кнопкою «Обрати CSV…».
Адресу сервера можна змінити параметром JVM `-Dlibrary.server=http://host:8080/library-server`
(VM options у конфігурації запуску IntelliJ IDEA).

## 4. Тести JUnit 5

```bash
cd server
mvn clean test
```

16 модульних тестів перевіряють нормування факторів F1–F3, обчислення показника I, відхилення
недопустимих значень, дозволені й заборонені переходи життєвого циклу та валідацію всіх сутностей.
Тести не потребують запуску JavaFX, Tomcat і MySQL.

## 5. Формат CSV (каталог видань)

Кодування UTF-8, роздільник `;`, перший рядок — заголовок:

```
isbn;title;author;publish_year;total_copies;available_copies;request_count
978-617-8023-11-2;Захар Беркут;Іван Франко;2020;4;2;9
```

| Стовпець | Тип | Обов'язковий | Правило |
|---|---|---|---|
| isbn | текст | так | 10–17 символів: цифри, дефіс, X; унікальний |
| title | текст | так | не порожній |
| author | текст | так | не порожній |
| publish_year | ціле | так | 1450..поточний рік |
| total_copies | ціле | так | ≥ 1 |
| available_copies | ціле | так | 0..total_copies |
| request_count | ціле | так | ≥ 0 |

Рядки з неправильною кількістю полів, нечисловими значеннями або порожніми обов'язковими полями
пропускаються клієнтом; рядки, які сервер відхилив (дублікат ISBN, недопустимі значення), також
пропускаються з причиною. Недоступність сервера перериває імпорт. Повторний імпорт того самого файла
не створює дублікатів — сервер відхиляє наявні ISBN.

## 6. GitHub

Репозиторій: https://github.com/sofi18-it/coursework-vikentieva-variant04
Фінальна версія позначена тегом `coursework-final`.
