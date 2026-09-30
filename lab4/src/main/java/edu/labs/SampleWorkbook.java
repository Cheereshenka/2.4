package edu.labs;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;
public final class SampleWorkbook {
    public static void write(Path path) throws Exception {
        try (var book = new XSSFWorkbook()) {
            var sheet = book.createSheet("Измерения"); var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Наблюдение"); header.createCell(1).setCellValue("Время, сек"); header.createCell(2).setCellValue("Баллы");
            double[] data = {10,11,12,12,13,14,15,15,16,17,18,45};
            for (int i = 0; i < data.length; i++) {
                var row = sheet.createRow(i + 1); row.createCell(0).setCellValue(i + 1); row.createCell(1).setCellValue(data[i]); row.createCell(2).setCellValue(60 + i * 2);
            }
            for (int i = 0; i < 3; i++) sheet.autoSizeColumn(i);
            Path parent = path.toAbsolutePath().getParent(); Files.createDirectories(parent);
            try (var out = Files.newOutputStream(path)) { book.write(out); }
        }
    }
    public static void main(String[] args) throws Exception { write(Path.of(args.length == 0 ? "examples/sample.xlsx" : args[0])); }
}
