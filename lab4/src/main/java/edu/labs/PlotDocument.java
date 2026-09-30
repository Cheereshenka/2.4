package edu.labs;

import java.util.*;
/** Единая геометрия графика для JavaFX и всех экспортёров. */
public record PlotDocument(int width, int height, List<Shape> shapes) {
    public sealed interface Shape permits Line, Rect, Dot, Text {}
    public record Line(double x1, double y1, double x2, double y2, String color, double thickness) implements Shape {}
    public record Rect(double x, double y, double w, double h, String fill, String stroke) implements Shape {}
    public record Dot(double x, double y, double radius, String color) implements Shape {}
    public record Text(double x, double y, String text, int size, String color) implements Shape {}
    public static PlotDocument boxPlot(List<Statistics.Result> results) {
        int width = 960, height = 140 + results.size() * 105;
        List<Shape> shapes = new ArrayList<>();
        shapes.add(new Text(24, 34, "Базовая статистика · Ящик с усами", 22, "#17233c"));
        shapes.add(new Text(24, 58, "Ящик: Q1–Q3 · линия: медиана · точки: выбросы по IQR", 14, "#65738a"));
        double min = results.stream().mapToDouble(r -> r.sorted().get(0)).min().orElse(0);
        double max = results.stream().mapToDouble(r -> r.sorted().get(r.sorted().size() - 1)).max().orElse(1);
        // Масштабирование до [-1;1] позволяет избежать переполнения max-min.
        double scale = Math.max(Math.abs(min), Math.abs(max)); if (scale == 0) scale = 1;
        double a = min / scale, b = max / scale;
        if (a == b) { a -= .1; b += .1; }
        double padding = (b - a) * .08; a -= padding; b += padding;
        final double low = a, span = b - a, unit = scale;
        java.util.function.DoubleUnaryOperator x = value -> 220 + (value / unit - low) / span * 690;
        for (int tick = 0; tick <= 5; tick++) {
            double px = 220 + tick * 138;
            shapes.add(new Line(px, 82, px, height - 44, "#e2e7f0", 1));
            shapes.add(new Text(px - 22, height - 18, String.format(Locale.US, "%.4g", (low + span * tick / 5) * unit), 12, "#65738a"));
        }
        for (int i = 0; i < results.size(); i++) {
            Statistics.Result r = results.get(i); double y = 114 + i * 105;
            String name = r.name().length() > 23 ? r.name().substring(0, 22) + "…" : r.name();
            shapes.add(new Text(24, y, name, 14, "#17233c")); shapes.add(new Text(24, y + 22, "n = " + r.count(), 12, "#65738a"));
            shapes.add(new Line(x.applyAsDouble(r.lowerWhisker()), y, x.applyAsDouble(r.upperWhisker()), y, "#4264d0", 2));
            for (double whisker : new double[]{r.lowerWhisker(), r.upperWhisker()}) shapes.add(new Line(x.applyAsDouble(whisker), y - 14, x.applyAsDouble(whisker), y + 14, "#4264d0", 2));
            shapes.add(new Rect(x.applyAsDouble(r.q1()), y - 23, Math.max(1, x.applyAsDouble(r.q3()) - x.applyAsDouble(r.q1())), 46, "#dee6ff", "#4264d0"));
            shapes.add(new Line(x.applyAsDouble(r.median()), y - 23, x.applyAsDouble(r.median()), y + 23, "#17233c", 3));
            for (double outlier : r.outliers()) shapes.add(new Dot(x.applyAsDouble(outlier), y, 4, "#df6b49"));
        }
        return new PlotDocument(width, height, List.copyOf(shapes));
    }
}
