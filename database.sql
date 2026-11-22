-- =====================================================================
-- Курсова робота «Кросплатформене програмування», варіант №4
-- Інформаційна система бронювання та видачі бібліотечних видань
-- Скрипт повністю відтворює базу даних з нуля (структура + демонстраційні дані).
-- Виконання: phpMyAdmin → вкладка SQL → вставити/відкрити файл → «Виконати».
-- =====================================================================

DROP DATABASE IF EXISTS library_reservations;
CREATE DATABASE library_reservations CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_reservations;

-- 1) Видання (каталог; саме ці дані імпортуються з CSV)
CREATE TABLE edition (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    isbn             VARCHAR(20)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(160) NOT NULL,
    publish_year     INT          NOT NULL,
    total_copies     INT          NOT NULL,
    available_copies INT          NOT NULL,
    request_count    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_edition_isbn UNIQUE (isbn),
    CONSTRAINT chk_edition_copies CHECK (total_copies >= 1 AND available_copies BETWEEN 0 AND total_copies),
    CONSTRAINT chk_edition_requests CHECK (request_count >= 0)
) ENGINE = InnoDB;

-- 2) Читачі
CREATE TABLE reader (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_number VARCHAR(20)  NOT NULL,
    full_name     VARCHAR(160) NOT NULL,
    phone         VARCHAR(20)  NULL,
    email         VARCHAR(120) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reader_ticket UNIQUE (ticket_number)
) ENGINE = InnoDB;

-- 3) Бронювання — об'єкт життєвого циклу
--    state: CREATED (СТВОРЕНО) → CONFIRMED (ПІДТВЕРДЖЕНО) → READY_FOR_PICKUP (ГОТОВЕ_ДО_ВИДАЧІ) → COMPLETED (ЗАВЕРШЕНО)
--    f1, f2, f3, priority_index обчислюються сервером: I = 0,40·F1 + 0,35·F2 + 0,25·F3
CREATE TABLE reservation (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    reader_id        BIGINT      NOT NULL,
    edition_id       BIGINT      NOT NULL,
    reservation_date DATE        NOT NULL,
    wait_days        INT         NOT NULL,
    state            VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    f1               DOUBLE      NOT NULL,
    f2               DOUBLE      NOT NULL,
    f3               DOUBLE      NOT NULL,
    priority_index   DOUBLE      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_reader  FOREIGN KEY (reader_id)  REFERENCES reader (id),
    CONSTRAINT fk_reservation_edition FOREIGN KEY (edition_id) REFERENCES edition (id),
    CONSTRAINT chk_reservation_state CHECK (state IN ('CREATED', 'CONFIRMED', 'READY_FOR_PICKUP', 'COMPLETED'))
) ENGINE = InnoDB;

-- 4) Видачі (оформлюються за бронюванням у стані READY_FOR_PICKUP)
CREATE TABLE loan (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    issue_date     DATE   NOT NULL,
    due_date       DATE   NOT NULL,
    return_date    DATE   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_loan_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_loan_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Початкові демонстраційні дані
-- ---------------------------------------------------------------------
INSERT INTO edition (isbn, title, author, publish_year, total_copies, available_copies, request_count) VALUES
('978-966-03-4000-1', 'Кобзар',                    'Тарас Шевченко',          2019, 5, 1, 15),
('978-617-679-210-5', 'Тіні забутих предків',      'Михайло Коцюбинський',    2018, 3, 2,  6),
('978-966-10-5577-2', 'Лісова пісня',              'Леся Українка',           2021, 4, 0, 12),
('978-617-12-8456-3', 'Java. Повне керівництво',   'Герберт Шилдт',           2023, 2, 1, 20),
('978-617-7409-35-4', 'Чистий код',                'Роберт Мартін',           2020, 3, 3,  4);

INSERT INTO reader (ticket_number, full_name, phone, email) VALUES
('RB-0001', 'Іваненко Олена Петрівна',     '+380501112233', 'olena.ivanenko@example.com'),
('RB-0002', 'Коваль Андрій Миколайович',   '+380672223344', 'a.koval@example.com'),
('RB-0003', 'Мельник Ірина Василівна',     '+380933334455', NULL),
('RB-0004', 'Бондаренко Дмитро Олегович',  NULL,            'd.bondarenko@example.com');

-- Значення F1–F3 та I відповідають розрахунку PriorityCalculator
INSERT INTO reservation (reader_id, edition_id, reservation_date, wait_days, state, f1, f2, f3, priority_index) VALUES
(1, 1, '2026-11-02', 12, 'CREATED',          40.00,  75.00,  80.00, 62.25),
(2, 3, '2026-10-28', 20, 'CONFIRMED',        66.67,  60.00, 100.00, 72.67),
(3, 4, '2026-11-05',  5, 'READY_FOR_PICKUP', 16.67, 100.00,  50.00, 54.17),
(4, 2, '2026-10-20',  3, 'COMPLETED',        10.00,  30.00,  33.33, 22.83);

INSERT INTO loan (reservation_id, issue_date, due_date, return_date) VALUES
(4, '2026-10-23', '2026-11-06', '2026-11-05');

-- Перевірка: SELECT * FROM edition; SELECT * FROM reader; SELECT * FROM reservation; SELECT * FROM loan;
