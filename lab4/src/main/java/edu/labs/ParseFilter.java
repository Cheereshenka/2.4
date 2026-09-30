package edu.labs;

import java.util.*;
import static edu.labs.AnalysisData.*;
public final class ParseFilter implements Filter<Raw, Numeric> {
    public Numeric process(Raw raw) {
        List<Column> columns = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        for (RawColumn column : raw.columns()) {
            List<Double> values = new ArrayList<>();
            for (CellValue cell : column.cells()) {
                try {
                    String text = cell.text().trim().replace("\u00a0", "").replace(" ", "").replace(',', '.');
                    double value = cell.number() != null ? cell.number() : Double.parseDouble(text);
                    if (!Double.isFinite(value)) throw new NumberFormatException(); values.add(value);
                } catch (NumberFormatException e) {
                    String message = column.name() + ", строка " + cell.row() + ": пустая или нечисловая ячейка";
                    if (!raw.request().skipInvalid()) throw new IllegalArgumentException(message);
                    warnings.add(message);
                }
            }
            columns.add(new Column(column.name(), List.copyOf(values)));
        }
        return new Numeric(raw.request(), List.copyOf(columns), List.copyOf(warnings));
    }
}
