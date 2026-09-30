package edu.labs;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class TableExporter {
    public static final List<String> HEADERS = List.of("Столбец", "N", "Среднее", "Стандартное отклонение", "Q1", "Медиана / Q2", "Q3", "IQR", "Нижний ус", "Верхний ус", "Выбросы", "Делитель дисперсии");
    public static List<Object> row(Statistics.Result r) {
        return List.of(r.name(), r.count(), r.mean(), r.standardDeviation(), r.q1(), r.median(), r.q3(), r.iqr(), r.lowerWhisker(), r.upperWhisker(), r.outliers().toString(), r.sample() ? "n-1" : "n");
    }
    public static void export(List<Statistics.Result> results, Path path, String format) throws IOException {
        switch (format) {
            case "CSV" -> {
                StringBuilder s = new StringBuilder("\ufeff"); s.append(csv(new ArrayList<>(HEADERS))).append("\r\n");
                for (var r : results) s.append(csv(row(r))).append("\r\n"); Files.writeString(path, s);
            }
            case "Markdown" -> {
                StringBuilder s = new StringBuilder("| " + String.join(" | ", HEADERS) + " |\n|" + " --- |".repeat(HEADERS.size()) + "\n");
                for (var r : results) {
                    s.append("| "); for (Object value : row(r)) s.append(value.toString().replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", " ")).append(" | "); s.append('\n');
                }
                Files.writeString(path, s);
            }
            case "JSON" -> {
                StringBuilder s = new StringBuilder("[\n");
                for (int i = 0; i < results.size(); i++) {
                    if (i > 0) s.append(",\n"); s.append("  {"); var values = row(results.get(i));
                    for (int j = 0; j < HEADERS.size(); j++) {
                        if (j > 0) s.append(", "); s.append(json(HEADERS.get(j))).append(": ");
                        Object value = values.get(j);
                        if (j == 10) s.append(results.get(i).outliers());
                        else s.append(value instanceof Number ? value : json(value.toString()));
                    }
                    s.append('}');
                }
                Files.writeString(path, s.append("\n]\n"));
            }
            case "Excel" -> {
                try (XSSFWorkbook book = new XSSFWorkbook()) {
                    var sheet = book.createSheet("Статистика"); var header = sheet.createRow(0);
                    var style = book.createCellStyle(); var font = book.createFont(); font.setBold(true); style.setFont(font);
                    for (int i = 0; i < HEADERS.size(); i++) { header.createCell(i).setCellValue(HEADERS.get(i)); header.getCell(i).setCellStyle(style); }
                    for (int i = 0; i < results.size(); i++) {
                        var row = sheet.createRow(i + 1); var values = row(results.get(i));
                        for (int j = 0; j < values.size(); j++) { var cell = row.createCell(j); Object value = values.get(j); if (value instanceof Number n) cell.setCellValue(n.doubleValue()); else cell.setCellValue(value.toString()); }
                    }
                    sheet.createFreezePane(1, 1); for (int i = 0; i < HEADERS.size(); i++) { sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(16000, sheet.getColumnWidth(i) + 600)); }
                    try (var out = Files.newOutputStream(path)) { book.write(out); }
                }
            }
            default -> throw new IllegalArgumentException("Неизвестный формат таблицы");
        }
    }
    private static String csv(List<?> row) {
        return row.stream().map(v -> "\"" + v.toString().replace("\"", "\"\"") + "\"").collect(java.util.stream.Collectors.joining(","));
    }
    private static String json(String text) {
        StringBuilder s = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == '\\' || c == '"') s.append('\\').append(c);
            else if (c < 32) s.append(String.format("\\u%04x", (int)c)); else s.append(c);
        }
        return s.append('"').toString();
    }
}
