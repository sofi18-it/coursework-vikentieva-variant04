package ua.edu.nupp.kpp.client;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * JavaFX-клієнт ІС бронювання та видачі бібліотечних видань.
 * Маршрут: JavaFX → LibraryApiClient → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL.
 * Клієнт не обчислює показник і не перевіряє переходи станів — це робить сервер.
 */
public class MainApp extends Application {
    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    private final LibraryApiClient api = new LibraryApiClient();

    private final TableView<Edition> editions = new TableView<>();
    private final TableView<Reader> readers = new TableView<>();
    private final TableView<Reservation> reservations = new TableView<>();
    private final TableView<Loan> loans = new TableView<>();

    private final ComboBox<Reader> resReader = new ComboBox<>();
    private final ComboBox<Edition> resEdition = new ComboBox<>();
    private final ComboBox<Reservation> loanReservation = new ComboBox<>();

    private Map<Long, Reader> readerById = new HashMap<>();
    private Map<Long, Edition> editionById = new HashMap<>();
    private Map<Long, Reservation> reservationById = new HashMap<>();

    private final Label statusBar = new Label("Готово");
    private Stage stage;

    /** Файл за замовчуванням — відносний шлях від каталогу запуску клієнта (client/). */
    private Path csvFile = Path.of("sample-data", "editions.csv");
    private boolean importRunning = false;

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        TabPane tabs = new TabPane(
                new Tab("Видання", editionPane()),
                new Tab("Читачі", readerPane()),
                new Tab("Бронювання", reservationPane()),
                new Tab("Видачі", loanPane()));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        statusBar.setPadding(new Insets(4, 10, 4, 10));
        BorderPane root = new BorderPane(tabs);
        root.setBottom(statusBar);

        stage.setScene(new Scene(root, 1250, 720));
        stage.setTitle("Бронювання та видача бібліотечних видань — варіант №4");
        stage.show();
        refreshAll();
    }

    // =====================================================================
    // Вкладка «Видання» + фоновий CSV-імпорт
    // =====================================================================
    private Pane editionPane() {
        editions.getColumns().setAll(col("ID", "id"), col("ISBN", "isbn"), col("Назва", "title"),
                col("Автор", "author"), col("Рік", "publishYear"), col("Усього", "totalCopies"),
                col("Доступно", "availableCopies"), col("Запитів", "requestCount"));

        TextField isbn = new TextField(), title = new TextField(), author = new TextField(),
                year = new TextField(), total = new TextField(), available = new TextField(),
                requests = new TextField();
        Button add = new Button("Додати"), update = new Button("Змінити"),
                delete = new Button("Видалити"), clear = new Button("Очистити"), refresh = new Button("Оновити");

        add.setOnAction(e -> safe(() -> {
            api.addEdition(readEdition(null, isbn, title, author, year, total, available, requests));
            refreshEditions();
            info("Видання додано");
        }));
        update.setOnAction(e -> safe(() -> {
            Edition s = selected(editions);
            api.updateEdition(readEdition(s.getId(), isbn, title, author, year, total, available, requests));
            refreshEditions();
            refreshReservations();
            info("Видання оновлено");
        }));
        delete.setOnAction(e -> safe(() -> {
            Edition s = selected(editions);
            if (confirm("Видалити видання «" + s.getTitle() + "»?")) {
                api.deleteEdition(s.getId());
                refreshEditions();
                info("Видання видалено");
            }
        }));
        clear.setOnAction(e -> {
            editions.getSelectionModel().clearSelection();
            clearFields(isbn, title, author, year, total, available, requests);
        });
        refresh.setOnAction(e -> safe(this::refreshEditions));
        editions.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                isbn.setText(s.getIsbn()); title.setText(s.getTitle()); author.setText(s.getAuthor());
                year.setText("" + s.getPublishYear()); total.setText("" + s.getTotalCopies());
                available.setText("" + s.getAvailableCopies()); requests.setText("" + s.getRequestCount());
            }
        });

        // ---- CSV-імпорт ----
        Label fileLabel = new Label(csvFile.toString());
        fileLabel.setWrapText(true);
        Button choose = new Button("Обрати CSV…");
        Button importCsv = new Button("Імпортувати CSV");
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(300);
        Label importStatus = new Label("Імпорт не запускався");
        importStatus.setWrapText(true);

        choose.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Оберіть CSV-каталог видань");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файли", "*.csv"));
            Path dir = csvFile.toAbsolutePath().getParent();
            if (dir != null && Files.isDirectory(dir)) fc.setInitialDirectory(dir.toFile());
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                csvFile = f.toPath();
                fileLabel.setText(f.getName());
            }
        });
        importCsv.setOnAction(e -> startImport(importCsv, choose, progress, importStatus));

        VBox form = form(
                new Label("ISBN"), isbn, new Label("Назва"), title, new Label("Автор"), author,
                new Label("Рік видання"), year, new Label("Усього примірників"), total,
                new Label("Доступно примірників"), available, new Label("Кількість запитів (для F2)"), requests,
                new HBox(6, add, update, delete), new HBox(6, clear, refresh),
                new Separator(),
                bold("Фоновий імпорт каталогу видань (CSV)"),
                new Label("Файл:"), fileLabel, new HBox(6, choose, importCsv), progress, importStatus);
        return layout(editions, form);
    }

    private void startImport(Button importBtn, Button chooseBtn, ProgressBar progress, Label status) {
        if (importRunning) {
            showWarning("Імпорт уже виконується. Дочекайтеся його завершення.");
            return;
        }
        importRunning = true;
        importBtn.setDisable(true);
        chooseBtn.setDisable(true);

        ImportEditionsTask task = new ImportEditionsTask(csvFile, api);
        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            finishImport(importBtn, chooseBtn, progress, status);
            ImportEditionsTask.Result r = task.getValue();
            status.setText("Готово. Рядків: " + r.total() + ", імпортовано: " + r.imported()
                    + ", пропущено: " + r.skipped());
            safe(this::refreshEditions);
            if (r.errors().isEmpty()) {
                info("CSV-імпорт завершено успішно: " + r.imported() + " записів");
            } else {
                List<String> shown = r.errors().size() > 15 ? r.errors().subList(0, 15) : r.errors();
                showWarning("Імпортовано " + r.imported() + " із " + r.total() + ". Пропущені рядки:\n"
                        + String.join("\n", shown) + (r.errors().size() > 15 ? "\n…" : ""));
            }
        });
        task.setOnFailed(e -> {
            finishImport(importBtn, chooseBtn, progress, status);
            status.setText("Імпорт завершився з помилкою");
            safe(this::refreshEditions);
            showError(task.getException());
        });

        Thread t = new Thread(task, "csv-import");
        t.setDaemon(true);
        t.start();
        info("CSV-імпорт запущено у фоновому потоці");
    }

    private void finishImport(Button importBtn, Button chooseBtn, ProgressBar progress, Label status) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        importRunning = false;
        importBtn.setDisable(false);
        chooseBtn.setDisable(false);
    }

    private Edition readEdition(Long id, TextField isbn, TextField title, TextField author, TextField year,
                                TextField total, TextField available, TextField requests) {
        return new Edition(id, isbn.getText(), title.getText(), author.getText(),
                integer(year, "Рік видання"), integer(total, "Усього примірників"),
                integer(available, "Доступно примірників"), integer(requests, "Кількість запитів"));
    }

    // =====================================================================
    // Вкладка «Читачі»
    // =====================================================================
    private Pane readerPane() {
        readers.getColumns().setAll(col("ID", "id"), col("Читацький квиток", "ticketNumber"),
                col("ПІБ", "fullName"), col("Телефон", "phone"), col("E-mail", "email"));
        TextField ticket = new TextField(), name = new TextField(), phone = new TextField(), email = new TextField();
        Button add = new Button("Додати"), update = new Button("Змінити"),
                delete = new Button("Видалити"), clear = new Button("Очистити"), refresh = new Button("Оновити");

        add.setOnAction(e -> safe(() -> {
            api.addReader(new Reader(null, ticket.getText(), name.getText(), phone.getText(), email.getText()));
            refreshReaders();
            info("Читача додано");
        }));
        update.setOnAction(e -> safe(() -> {
            Reader s = selected(readers);
            api.updateReader(new Reader(s.getId(), ticket.getText(), name.getText(), phone.getText(), email.getText()));
            refreshReaders();
            reservations.refresh();
            info("Дані читача оновлено");
        }));
        delete.setOnAction(e -> safe(() -> {
            Reader s = selected(readers);
            if (confirm("Видалити читача «" + s.getFullName() + "»?")) {
                api.deleteReader(s.getId());
                refreshReaders();
                info("Читача видалено");
            }
        }));
        clear.setOnAction(e -> {
            readers.getSelectionModel().clearSelection();
            clearFields(ticket, name, phone, email);
        });
        refresh.setOnAction(e -> safe(this::refreshReaders));
        readers.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                ticket.setText(s.getTicketNumber()); name.setText(s.getFullName());
                phone.setText(s.getPhone()); email.setText(s.getEmail());
            }
        });
        return layout(readers, form(new Label("Номер читацького квитка"), ticket, new Label("ПІБ"), name,
                new Label("Телефон"), phone, new Label("E-mail"), email,
                new HBox(6, add, update, delete), new HBox(6, clear, refresh)));
    }

    // =====================================================================
    // Вкладка «Бронювання»: CRUD, показник пріоритету, життєвий цикл
    // =====================================================================
    private Pane reservationPane() {
        reservations.getColumns().setAll(col("ID", "id"),
                textCol("Читач", r -> readerName(r.getReaderId())),
                textCol("Видання", r -> editionTitle(r.getEditionId())),
                col("Дата", "reservationDate"), col("Очікування, дн.", "waitDays"),
                textCol("Стан", r -> r.getState() == null ? "" : r.getState().getLabel()),
                col("F1", "f1"), col("F2", "f2"), col("F3", "f3"), col("I (пріоритет)", "priorityIndex"));
        resReader.setMaxWidth(Double.MAX_VALUE);
        resEdition.setMaxWidth(Double.MAX_VALUE);
        resReader.setPromptText("Оберіть читача");
        resEdition.setPromptText("Оберіть видання");

        TextField date = new TextField(LocalDate.now().toString());
        TextField waitDays = new TextField("0");
        Button add = new Button("Створити"), update = new Button("Змінити / перерахувати I"),
                delete = new Button("Видалити"), refresh = new Button("Оновити");

        Label currentState = new Label("—");
        ComboBox<ReservationState> nextState =
                new ComboBox<>(FXCollections.observableArrayList(ReservationState.values()));
        nextState.setPromptText("Новий стан");
        Button change = new Button("Змінити стан");

        add.setOnAction(e -> safe(() -> {
            api.addReservation(idOf(resReader.getValue(), "читача"), idOf(resEdition.getValue(), "видання"),
                    date.getText().trim(), integer(waitDays, "Тривалість очікування"));
            refreshReservations();
            info("Бронювання створено у стані СТВОРЕНО, показник I обчислено сервером");
        }));
        update.setOnAction(e -> safe(() -> {
            Reservation s = selected(reservations);
            api.updateReservation(s.getId(), idOf(resReader.getValue(), "читача"),
                    idOf(resEdition.getValue(), "видання"), date.getText().trim(),
                    integer(waitDays, "Тривалість очікування"));
            refreshReservations();
            info("Бронювання оновлено, показник перераховано");
        }));
        delete.setOnAction(e -> safe(() -> {
            Reservation s = selected(reservations);
            if (confirm("Видалити бронювання #" + s.getId() + "?")) {
                api.deleteReservation(s.getId());
                refreshReservations();
                info("Бронювання видалено");
            }
        }));
        change.setOnAction(e -> safe(() -> {
            Reservation s = selected(reservations);
            if (nextState.getValue() == null) throw new IllegalArgumentException("Оберіть новий стан");
            api.changeReservationState(s.getId(), nextState.getValue());
            refreshReservations();
            info("Стан бронювання #" + s.getId() + " змінено на " + nextState.getValue().getLabel());
        }));
        refresh.setOnAction(e -> safe(this::refreshReservations));
        reservations.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                selectById(resReader, Reader::getId, s.getReaderId());
                selectById(resEdition, Edition::getId, s.getEditionId());
                date.setText(s.getReservationDate());
                waitDays.setText("" + s.getWaitDays());
                currentState.setText(s.getState() == null ? "—" : s.getState().getLabel());
            }
        });

        Label formula = new Label("I = 0,40·F1 + 0,35·F2 + 0,25·F3\n"
                + "F1 — тривалість очікування (30 днів = 100)\n"
                + "F2 — попит на видання (20 запитів = 100)\n"
                + "F3 — дефіцит доступних примірників, %");
        formula.setWrapText(true);
        Label scheme = new Label("СТВОРЕНО → ПІДТВЕРДЖЕНО → ГОТОВЕ_ДО_ВИДАЧІ → ЗАВЕРШЕНО");
        scheme.setWrapText(true);

        VBox form = form(new Label("Читач"), resReader, new Label("Видання"), resEdition,
                new Label("Дата бронювання (РРРР-ММ-ДД)"), date,
                new Label("Тривалість очікування, днів"), waitDays,
                new HBox(6, add, delete, refresh), update,
                new Separator(), bold("Інтегральний показник пріоритету"), formula,
                new Separator(), bold("Життєвий цикл бронювання"), scheme,
                new HBox(6, new Label("Поточний стан:"), currentState), nextState, change);
        return layout(reservations, form);
    }

    // =====================================================================
    // Вкладка «Видачі»
    // =====================================================================
    private Pane loanPane() {
        loans.getColumns().setAll(col("ID", "id"),
                textCol("Бронювання", l -> "#" + l.getReservationId()),
                textCol("Читач", l -> {
                    Reservation r = reservationById.get(l.getReservationId());
                    return r == null ? "" : readerName(r.getReaderId());
                }),
                textCol("Видання", l -> {
                    Reservation r = reservationById.get(l.getReservationId());
                    return r == null ? "" : editionTitle(r.getEditionId());
                }),
                col("Дата видачі", "issueDate"), col("Повернути до", "dueDate"),
                textCol("Повернено", l -> l.getReturnDate() == null ? "—" : l.getReturnDate()));
        loanReservation.setMaxWidth(Double.MAX_VALUE);
        loanReservation.setPromptText("Оберіть бронювання");
        loanReservation.setConverter(new StringConverter<>() {
            @Override public String toString(Reservation r) { return r == null ? "" : describe(r); }
            @Override public Reservation fromString(String s) { return null; }
        });

        TextField issue = new TextField(LocalDate.now().toString());
        TextField due = new TextField(LocalDate.now().plusDays(14).toString());
        TextField returned = new TextField();
        returned.setPromptText("порожньо, якщо не повернуто");
        Button add = new Button("Оформити видачу"), update = new Button("Змінити"),
                delete = new Button("Видалити"), refresh = new Button("Оновити");

        add.setOnAction(e -> safe(() -> {
            Reservation r = loanReservation.getValue();
            api.addLoan(new Loan(null, idOf(r, "бронювання"), issue.getText().trim(), due.getText().trim(),
                    returned.getText().trim()));
            refreshReservations();
            refreshLoans();
            info("Видачу оформлено, бронювання #" + r.getId() + " переведено у стан ЗАВЕРШЕНО");
        }));
        update.setOnAction(e -> safe(() -> {
            Loan s = selected(loans);
            api.updateLoan(new Loan(s.getId(), s.getReservationId(), issue.getText().trim(), due.getText().trim(),
                    returned.getText().trim()));
            refreshLoans();
            info("Видачу оновлено");
        }));
        delete.setOnAction(e -> safe(() -> {
            Loan s = selected(loans);
            if (confirm("Видалити видачу #" + s.getId() + "?")) {
                api.deleteLoan(s.getId());
                refreshLoans();
                info("Видачу видалено");
            }
        }));
        refresh.setOnAction(e -> safe(() -> { refreshReservations(); refreshLoans(); }));
        loans.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            if (s != null) {
                selectById(loanReservation, Reservation::getId, s.getReservationId());
                issue.setText(s.getIssueDate());
                due.setText(s.getDueDate());
                returned.setText(s.getReturnDate() == null ? "" : s.getReturnDate());
            }
        });
        Label hint = new Label("Видача можлива лише для бронювання у стані ГОТОВЕ_ДО_ВИДАЧІ. "
                + "Після оформлення видачі сервер переводить бронювання у стан ЗАВЕРШЕНО.");
        hint.setWrapText(true);
        return layout(loans, form(new Label("Бронювання"), loanReservation,
                new Label("Дата видачі (РРРР-ММ-ДД)"), issue, new Label("Повернути до"), due,
                new Label("Дата повернення"), returned,
                new HBox(6, add, update), new HBox(6, delete, refresh), new Separator(), hint));
    }

    // =====================================================================
    // Оновлення даних
    // =====================================================================
    private void refreshAll() {
        try {
            refreshEditions();
            refreshReaders();
            refreshReservations();
            refreshLoans();
            info("Дані завантажено із сервера " + api.getBaseUrl());
        } catch (Exception e) {
            LOG.warning("Не вдалося завантажити дані: " + e.getMessage());
            statusBar.setText("Немає зв'язку із сервером");
            showError(e);
        }
    }

    private void refreshEditions() {
        List<Edition> list = api.getEditions();
        editions.setItems(FXCollections.observableArrayList(list));
        editionById = index(list, Edition::getId);
        resEdition.setItems(FXCollections.observableArrayList(list));
        reservations.refresh();
    }

    private void refreshReaders() {
        List<Reader> list = api.getReaders();
        readers.setItems(FXCollections.observableArrayList(list));
        readerById = index(list, Reader::getId);
        resReader.setItems(FXCollections.observableArrayList(list));
        reservations.refresh();
    }

    private void refreshReservations() {
        List<Reservation> list = api.getReservations();
        reservations.setItems(FXCollections.observableArrayList(list));
        reservationById = index(list, Reservation::getId);
        loanReservation.setItems(FXCollections.observableArrayList(list));
        loans.refresh();
    }

    private void refreshLoans() {
        loans.setItems(FXCollections.observableArrayList(api.getLoans()));
    }

    // =====================================================================
    // Допоміжні методи
    // =====================================================================
    private String readerName(long id) {
        Reader r = readerById.get(id);
        return r == null ? "#" + id : r.getFullName();
    }

    private String editionTitle(long id) {
        Edition e = editionById.get(id);
        return e == null ? "#" + id : e.getTitle();
    }

    private String describe(Reservation r) {
        return "#" + r.getId() + " " + readerName(r.getReaderId()) + " — " + editionTitle(r.getEditionId())
                + " [" + (r.getState() == null ? "" : r.getState().getLabel()) + "]";
    }

    private static <T> Map<Long, T> index(List<T> list, Function<T, Long> id) {
        Map<Long, T> m = new HashMap<>();
        for (T t : list) m.put(id.apply(t), t);
        return m;
    }

    private static <T> void selectById(ComboBox<T> box, Function<T, Long> id, long value) {
        for (T t : box.getItems()) {
            if (id.apply(t) != null && id.apply(t) == value) {
                box.getSelectionModel().select(t);
                return;
            }
        }
        box.getSelectionModel().clearSelection();
    }

    private static long idOf(Object item, String what) {
        if (item == null) throw new IllegalArgumentException("Оберіть " + what);
        if (item instanceof Reader r) return r.getId();
        if (item instanceof Edition e) return e.getId();
        if (item instanceof Reservation r) return r.getId();
        throw new IllegalArgumentException("Невідомий тип запису");
    }

    private static int integer(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Поле «" + name + "» має бути цілим числом");
        }
    }

    private static void clearFields(TextField... fields) {
        for (TextField f : fields) f.clear();
    }

    private <T> T selected(TableView<T> table) {
        T v = table.getSelectionModel().getSelectedItem();
        if (v == null) throw new IllegalArgumentException("Виберіть запис у таблиці");
        return v;
    }

    private void safe(Action action) {
        try {
            action.run();
        } catch (Exception e) {
            LOG.warning(e.getMessage());
            statusBar.setText("Помилка: " + e.getMessage());
            showError(e);
        }
    }

    private void info(String message) {
        statusBar.setText(message);
        LOG.info(message);
    }

    private void showError(Throwable e) {
        String msg = e == null ? "Невідома помилка" : (e.getMessage() == null ? e.toString() : e.getMessage());
        dialog(javafx.scene.control.Alert.AlertType.ERROR, "Помилка", msg);
    }

    private void showWarning(String message) {
        dialog(javafx.scene.control.Alert.AlertType.WARNING, "Увага", message);
    }

    private void dialog(javafx.scene.control.Alert.AlertType type, String header, String message) {
        javafx.scene.control.Alert d = new javafx.scene.control.Alert(type, message, ButtonType.OK);
        d.setHeaderText(header);
        d.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        d.initOwner(stage);
        d.showAndWait();
    }

    private boolean confirm(String message) {
        javafx.scene.control.Alert d = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        d.setHeaderText("Підтвердження");
        d.initOwner(stage);
        return d.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static VBox form(javafx.scene.Node... nodes) {
        VBox box = new VBox(6, nodes);
        box.setPadding(new Insets(8));
        return box;
    }

    private Pane layout(TableView<?> table, VBox form) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        SplitPane split = new SplitPane(table, scroll);
        split.setDividerPositions(0.70);
        BorderPane root = new BorderPane(split);
        root.setPadding(new Insets(10));
        return root;
    }

    private static <S, T> TableColumn<S, T> col(String title, String property) {
        TableColumn<S, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    private static <S> TableColumn<S, String> textCol(String title, Function<S, String> value) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(value.apply(cd.getValue())));
        return c;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
