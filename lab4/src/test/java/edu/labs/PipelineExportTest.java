package edu.labs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.Loader;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PipelineExportTest {
    @TempDir Path dir;
    @Test void excelPipelineHandlesFormulaTextAndInvalidCells() throws Exception {
        Path file = dir.resolve("input.xlsx");
        try (var book = new XSSFWorkbook()) {
            var s = book.createSheet("Лист"); s.createRow(0).createCell(0).setCellValue("Данные");
            s.createRow(1).createCell(0).setCellValue(2); s.createRow(2).createCell(0).setCellFormula("A2*2");
            s.createRow(3).createCell(0).setCellValue("6,0"); s.createRow(4).createCell(0).setCellValue("текст");
            try (var out = Files.newOutputStream(file)) { book.write(out); }
        }
        var request = new AnalysisData.Request(file, 0, List.of(0), 2, 5, true, true);
        var report = new AnalysisPipeline().analyze(request); assertEquals(4, report.results().get(0).mean()); assertEquals(1, report.warnings().size());
        assertThrows(IllegalArgumentException.class, () -> new AnalysisPipeline().analyze(new AnalysisData.Request(file, 0, List.of(0), 2, 5, false, true)));
    }
    @Test void everyExportIsReadableAndPreservesResults() throws Exception {
        var r = Statistics.calculate("Время <сек> & тест", List.of(1.,2.,3.,4.,5.,100.), true); var plot = PlotDocument.boxPlot(List.of(r));
        for (String format : List.of("PNG", "SVG", "PDF", "HTML")) ChartExporter.export(plot, dir.resolve("plot." + format.toLowerCase()), format);
        assertEquals(plot.width() * 2, ImageIO.read(dir.resolve("plot.png").toFile()).getWidth());
        try (var pdf = Loader.loadPDF(dir.resolve("plot.pdf").toFile())) { assertEquals(1, pdf.getNumberOfPages()); }
        var xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dir.resolve("plot.svg").toFile()); assertEquals("svg", xml.getDocumentElement().getTagName());
        assertTrue(Files.readString(dir.resolve("plot.html")).contains("<svg"));
        for (String format : List.of("CSV", "Excel", "JSON", "Markdown")) TableExporter.export(List.of(r), dir.resolve("table." + format), format);
        try (var book = WorkbookFactory.create(dir.resolve("table.Excel").toFile())) { assertEquals(r.mean(), book.getSheetAt(0).getRow(1).getCell(2).getNumericCellValue()); }
        assertTrue(Files.readString(dir.resolve("table.JSON")).contains("[100.0]")); assertTrue(Files.readString(dir.resolve("table.CSV")).contains("Время"));
        assertTrue(Files.readString(dir.resolve("table.Markdown")).contains("| --- |"));
    }
}
