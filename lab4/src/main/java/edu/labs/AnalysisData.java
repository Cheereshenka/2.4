package edu.labs;

import java.nio.file.Path;
import java.util.*;
public final class AnalysisData {
    public record Request(Path file, int sheet, List<Integer> columns, int firstRow, int lastRow, boolean skipInvalid, boolean sample) {
        public Request {
            columns = List.copyOf(columns);
            if (file == null || sheet < 0 || columns.isEmpty() || firstRow < 2 || lastRow < firstRow) throw new IllegalArgumentException("Выберите файл, столбцы и диапазон строк (начиная со второй)");
        }
    }
    public record CellValue(int row, String text, Double number) {}
    public record RawColumn(String name, List<CellValue> cells) {}
    public record Raw(Request request, List<RawColumn> columns) {}
    public record Column(String name, List<Double> values) {}
    public record Numeric(Request request, List<Column> columns, List<String> warnings) {}
    public record Analyzed(List<Statistics.Result> results, List<String> warnings) {}
    public record Report(List<Statistics.Result> results, List<String> warnings, PlotDocument chart) {}
    public record SheetInfo(String name, int lastRow, List<String> columns) {}
}
