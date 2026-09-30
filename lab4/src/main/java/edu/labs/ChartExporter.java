package edu.labs;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
public final class ChartExporter {
    public static void export(PlotDocument plot, Path path, String format) throws IOException {
        switch (format) {
            case "PNG" -> ImageIO.write(image(plot), "png", path.toFile());
            case "SVG" -> Files.writeString(path, svg(plot));
            case "HTML" -> Files.writeString(path, "<!doctype html><html lang=\"ru\"><meta charset=\"utf-8\"><title>Статистический анализ</title><body style=\"margin:24px;background:#f3f5fa;font-family:sans-serif\"><h1>Результаты анализа</h1>" + svg(plot) + "</body></html>");
            case "PDF" -> {
                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage(new PDRectangle(plot.width(), plot.height())); doc.addPage(page);
                    var image = LosslessFactory.createFromImage(doc, image(plot));
                    try (PDPageContentStream stream = new PDPageContentStream(doc, page)) { stream.drawImage(image, 0, 0, plot.width(), plot.height()); }
                    doc.save(path.toFile());
                }
            }
            default -> throw new IllegalArgumentException("Неизвестный формат графика");
        }
    }
    public static BufferedImage image(PlotDocument plot) {
        BufferedImage image = new BufferedImage(plot.width() * 2, plot.height() * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.scale(2, 2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, plot.width(), plot.height());
        for (var shape : plot.shapes()) {
            if (shape instanceof PlotDocument.Line l) { g.setColor(Color.decode(l.color())); g.setStroke(new BasicStroke((float) l.thickness())); g.draw(new Line2D.Double(l.x1(), l.y1(), l.x2(), l.y2())); }
            else if (shape instanceof PlotDocument.Rect r) { var rect = new Rectangle2D.Double(r.x(), r.y(), r.w(), r.h()); g.setColor(Color.decode(r.fill())); g.fill(rect); g.setColor(Color.decode(r.stroke())); g.setStroke(new BasicStroke(2)); g.draw(rect); }
            else if (shape instanceof PlotDocument.Dot d) { g.setColor(Color.decode(d.color())); g.fill(new Ellipse2D.Double(d.x()-d.radius(), d.y()-d.radius(), d.radius()*2, d.radius()*2)); }
            else if (shape instanceof PlotDocument.Text t) { g.setColor(Color.decode(t.color())); g.setFont(new Font("SansSerif", Font.PLAIN, t.size())); g.drawString(t.text(), (float)t.x(), (float)t.y()); }
        }
        g.dispose(); return image;
    }
    private static String xml(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    public static String svg(PlotDocument plot) {
        StringBuilder s = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + plot.width() + "\" height=\"" + plot.height() + "\" viewBox=\"0 0 " + plot.width() + " " + plot.height() + "\">");
        s.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>");
        for (var shape : plot.shapes()) {
            if (shape instanceof PlotDocument.Line l) s.append("<line x1=\"").append(l.x1()).append("\" y1=\"").append(l.y1()).append("\" x2=\"").append(l.x2()).append("\" y2=\"").append(l.y2()).append("\" stroke=\"").append(l.color()).append("\" stroke-width=\"").append(l.thickness()).append("\"/>");
            else if (shape instanceof PlotDocument.Rect r) s.append("<rect x=\"").append(r.x()).append("\" y=\"").append(r.y()).append("\" width=\"").append(r.w()).append("\" height=\"").append(r.h()).append("\" fill=\"").append(r.fill()).append("\" stroke=\"").append(r.stroke()).append("\" stroke-width=\"2\"/>");
            else if (shape instanceof PlotDocument.Dot d) s.append("<circle cx=\"").append(d.x()).append("\" cy=\"").append(d.y()).append("\" r=\"").append(d.radius()).append("\" fill=\"").append(d.color()).append("\"/>");
            else if (shape instanceof PlotDocument.Text t) s.append("<text x=\"").append(t.x()).append("\" y=\"").append(t.y()).append("\" font-family=\"sans-serif\" font-size=\"").append(t.size()).append("\" fill=\"").append(t.color()).append("\">").append(xml(t.text())).append("</text>");
        }
        return s.append("</svg>").toString();
    }
}
