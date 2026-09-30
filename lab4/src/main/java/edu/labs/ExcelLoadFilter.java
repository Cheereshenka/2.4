package edu.labs;

import org.apache.poi.ss.usermodel.*;
import edu.labs.AnalysisData.CellValue;
import org.apache.poi.ss.util.CellReference;
import java.nio.file.*;
import java.util.*;
import static edu.labs.AnalysisData.*;

public final class ExcelLoadFilter implements Filter<Request, Raw> {
    public static List<SheetInfo> inspect(Path path) throws Exception {
        try (var input = Files.newInputStream(path); Workbook book = WorkbookFactory.create(input)) {
            List<SheetInfo> result = new ArrayList<>(); DataFormatter format = new DataFormatter(Locale.US);
            FormulaEvaluator evaluator = book.getCreationHelper().createFormulaEvaluator();
            for (Sheet sheet : book) {
                Row header = sheet.getRow(0); int count = header == null ? 0 : Math.max(0, header.getLastCellNum());
                for (Row row : sheet) count = Math.max(count, row.getLastCellNum());
                List<String> columns = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String title = header == null ? "" : format.formatCellValue(header.getCell(i), evaluator);
                    columns.add(CellReference.convertNumToColString(i) + " · " + (title.isBlank() ? "Столбец " + (i + 1) : title));
                }
                result.add(new SheetInfo(sheet.getSheetName(), sheet.getLastRowNum() + 1, List.copyOf(columns)));
            }
            return List.copyOf(result);
        }
    }
    @Override public Raw process(Request request) throws Exception {
        try (var input = Files.newInputStream(request.file()); Workbook book = WorkbookFactory.create(input)) {
            if (request.sheet() >= book.getNumberOfSheets()) throw new IllegalArgumentException("Лист не найден");
            Sheet sheet = book.getSheetAt(request.sheet());
            if (request.lastRow() > sheet.getLastRowNum() + 1) throw new IllegalArgumentException("Последняя строка выходит за границы листа");
            DataFormatter format = new DataFormatter(Locale.US); FormulaEvaluator evaluator = book.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(0); List<RawColumn> columns = new ArrayList<>();
            for (int col : request.columns()) {
                if (col < 0 || col >= 16384) throw new IllegalArgumentException("Некорректный номер столбца");
                String title = header == null ? "" : format.formatCellValue(header.getCell(col), evaluator);
                String name = CellReference.convertNumToColString(col) + " · " + (title.isBlank() ? "Столбец " + (col + 1) : title);
                List<CellValue> cells = new ArrayList<>();
                for (int row = request.firstRow() - 1; row < request.lastRow(); row++) {
                    Row r = sheet.getRow(row); Cell cell = r == null ? null : r.getCell(col);
                    String text = ""; Double number = null;
                    if (cell != null) {
                        try {
                            text = format.formatCellValue(cell, evaluator);
                                boolean numericCell = cell.getCellType() == CellType.NUMERIC ||
                                    (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC);
                                if (numericCell && DateUtil.isCellDateFormatted(cell)) text = "[дата] " + text;
                            else {
                                CellType type = cell.getCellType();
                                if (type == CellType.NUMERIC) number = cell.getNumericCellValue();
                                else if (type == CellType.FORMULA) {
                                    var value = evaluator.evaluate(cell); if (value != null && value.getCellType() == CellType.NUMERIC) number = value.getNumberValue();
                                }
                            }
                        } catch (RuntimeException e) { text = "[ошибка ячейки]"; }
                    }
                    cells.add(new CellValue(row + 1, text, number));
                }
                columns.add(new RawColumn(name, List.copyOf(cells)));
            }
            return new Raw(request, List.copyOf(columns));
        }
    }
}
