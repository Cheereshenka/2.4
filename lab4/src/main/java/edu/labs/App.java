package edu.labs;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import static edu.labs.AnalysisData.*;

public class App extends Application {
    private Path file;
    private List<SheetInfo> sheets = List.of();
    private Report report;
    private final ComboBox<String> sheet = new ComboBox<>();
    private final ListView<String> columns = new ListView<>();
    private final TextField first = new TextField("2"), last = new TextField();
    private final CheckBox skip = new CheckBox("Пропускать пустые / нечисловые ячейки"), sample = new CheckBox("Дисперсия с делителем n−1");
    private final Label filename = Ui.label("Файл ещё не выбран", "muted"), status = new Label("Откройте Excel или создайте пример"), summary = Ui.label("Результаты появятся после расчёта", "section");
    private final TableView<Statistics.Result> table = new TableView<>();
    private final ScrollPane chart = new ScrollPane();
    private final TextArea warnings = new TextArea();
    private final ComboBox<String> chartFormat = new ComboBox<>(), tableFormat = new ComboBox<>();
    private VBox controls;
    private HBox exports;
    @Override public void start(Stage stage) {
            columns.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); columns.setPrefHeight(125);
        columns.getSelectionModel().getSelectedIndices().addListener((javafx.collections.ListChangeListener<Integer>) c -> invalidate());
            sample.setSelected(true); skip.setSelected(true); skip.setWrapText(true); sample.setWrapText(true);
            skip.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE); sample.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        first.textProperty().addListener((o,a,b) -> invalidate()); last.textProperty().addListener((o,a,b) -> invalidate());
        sample.setOnAction(e -> invalidate()); skip.setOnAction(e -> invalidate());
        sheet.setMaxWidth(Double.MAX_VALUE);
        sheet.setOnAction(e -> {
            int index = sheet.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < sheets.size()) {
                columns.getItems().setAll(sheets.get(index).columns()); first.setText("2"); last.setText("" + sheets.get(index).lastRow());
                if (!columns.getItems().isEmpty()) columns.getSelectionModel().select(columns.getItems().size() > 1 ? 1 : 0); invalidate();
            }
        });
        Button open = Ui.primary("Открыть Excel", () -> {
            FileChooser chooser = new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
            File f = chooser.showOpenDialog(stage); if (f != null) load(f.toPath());
        });
        Button demo = Ui.button("Создать пример Excel", () -> {
            FileChooser chooser = new FileChooser(); chooser.setInitialFileName("sample.xlsx"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            File f = chooser.showSaveDialog(stage); if (f != null) run("Создание примера…", () -> { SampleWorkbook.write(f.toPath()); return ExcelLoadFilter.inspect(f.toPath()); }, result -> acceptFile(f.toPath(), result));
        });
        controls = Ui.card(Ui.label("01 / Источник данных", "section"), open, demo, filename, Ui.field("Лист", sheet), Ui.field("Столбцы · Ctrl / Shift для нескольких", columns),
            new HBox(10, Ui.field("Первая строка", first), Ui.field("Последняя строка", last)), skip, sample,
            Ui.primary("Рассчитать", () -> {
                if (file == null) throw new IllegalArgumentException("Сначала откройте Excel-файл");
                Request request;
                try { request = new Request(file, sheet.getSelectionModel().getSelectedIndex(), new ArrayList<>(columns.getSelectionModel().getSelectedIndices()), Integer.parseInt(first.getText().trim()), Integer.parseInt(last.getText().trim()), skip.isSelected(), sample.isSelected()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Введите целые номера строк"); }
                invalidate(); run("Выполняется конвейер…", () -> new AnalysisPipeline().analyze(request), this::display);
            }), Ui.label("Первая строка листа — заголовки. Данные начинаются со второй строки. Каждый выбранный столбец анализируется отдельно.", "muted"));
            controls.setPrefWidth(315); controls.setMinWidth(300); controls.setSpacing(10);
        ScrollPane inputScroll = new ScrollPane(controls); inputScroll.setFitToWidth(true); inputScroll.setPrefWidth(345); inputScroll.setMinWidth(330);
        for (int i = 0; i < TableExporter.HEADERS.size(); i++) {
            final int col = i; TableColumn<Statistics.Result,String> c = new TableColumn<>(TableExporter.HEADERS.get(i));
            c.setCellValueFactory(v -> { Object value = TableExporter.row(v.getValue()).get(col); return new ReadOnlyStringWrapper(value instanceof Double d ? String.format(Locale.US, "%.6g", d) : value.toString()); });
            c.setPrefWidth(i == 0 ? 180 : 130); table.getColumns().add(c);
        }
        table.setPlaceholder(new Label("Нет рассчитанных данных")); table.setPrefHeight(170);
        warnings.setEditable(false); warnings.setWrapText(true); warnings.setPrefRowCount(3);
            chart.setContent(Ui.label("Здесь будет ящик с усами", "muted")); chart.setPrefHeight(280); chart.setFitToWidth(true);
        chartFormat.getItems().addAll("PNG", "SVG", "PDF", "HTML"); chartFormat.setValue("PNG");
        tableFormat.getItems().addAll("CSV", "Excel", "JSON", "Markdown"); tableFormat.setValue("Excel");
        exports = new HBox(8, chartFormat, Ui.button("Сохранить график", () -> export(stage, true)), tableFormat, Ui.button("Сохранить таблицу", () -> export(stage, false))); exports.setDisable(true);
        VBox result = Ui.card(summary, chart, Ui.label("Числовые результаты", "section"), table, exports, warnings); VBox.setVgrow(chart, Priority.ALWAYS); HBox.setHgrow(result, Priority.ALWAYS);
        Ui.show(stage, "Статистический анализатор", "Лабораторная 04 / Каналы и фильтры / Вариант 1", new HBox(18, inputScroll, result), status);
    }
    private void load(Path path) { run("Чтение структуры Excel…", () -> ExcelLoadFilter.inspect(path), info -> acceptFile(path, info)); }
    private void acceptFile(Path path, List<SheetInfo> info) {
        file = path; sheets = info; filename.setText(path.getFileName().toString()); sheet.getItems().setAll(info.stream().map(SheetInfo::name).toList()); sheet.getSelectionModel().selectFirst(); invalidate(); status.setText("Файл открыт: " + path.getFileName());
    }
    private void invalidate() {
        report = null; if (exports != null) exports.setDisable(true); table.getItems().clear();
        chart.setContent(Ui.label("Выберите параметры и нажмите «Рассчитать»", "muted")); summary.setText("Ожидание расчёта"); warnings.clear();
    }
    private void display(Report value) {
        report = value; table.getItems().setAll(value.results()); chart.setContent(new ChartView(value.chart())); exports.setDisable(false);
        summary.setText("Проанализировано столбцов: " + value.results().size());
        warnings.setText(value.warnings().isEmpty() ? "Все ячейки обработаны без пропусков." : "Пропущено ячеек: " + value.warnings().size() + "\n" + String.join("\n", value.warnings().stream().limit(100).toList()));
        status.setText("Расчёт завершён. Квартили: линейная интерполяция (тип 7). Границы выбросов: 1,5 × IQR.");
    }
    private void export(Stage stage, boolean graph) {
        if (report == null) throw new IllegalArgumentException("Сначала выполните расчёт");
        String format = graph ? chartFormat.getValue() : tableFormat.getValue();
        String extension = switch (format) { case "Excel" -> "xlsx"; case "Markdown" -> "md"; default -> format.toLowerCase(Locale.ROOT); };
        FileChooser chooser = new FileChooser(); chooser.setInitialFileName((graph ? "boxplot." : "statistics.") + extension); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format, "*." + extension));
        File f = chooser.showSaveDialog(stage); if (f == null) return; Report snapshot = report;
        Path destination = f.toPath();
        run("Сохранение результата…", () -> { if (graph) ChartExporter.export(snapshot.chart(), destination, format); else TableExporter.export(snapshot.results(), destination, format); return destination; }, p -> status.setText("Сохранено: " + p));
    }
    private <T> void run(String message, java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        status.setText(message); controls.setDisable(true); exports.setDisable(true);
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(e -> { controls.setDisable(false); exports.setDisable(report == null); Ui.guard(() -> success.accept(task.getValue())); });
        task.setOnFailed(e -> { controls.setDisable(false); exports.setDisable(report == null); status.setText("Ошибка: " + task.getException().getMessage()); Ui.error(task.getException()); });
        Thread thread = new Thread(task, "analysis-worker"); thread.setDaemon(true); thread.start();
    }
    public static void main(String[] args) { launch(args); }
}
