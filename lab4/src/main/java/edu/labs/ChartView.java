package edu.labs;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.layout.Region;
public final class ChartView extends Region {
    private final PlotDocument plot;
    private final Canvas canvas = new Canvas();
    public ChartView(PlotDocument plot) {
        this.plot = plot; getChildren().add(canvas); setMinWidth(0);
    }
    @Override protected double computePrefWidth(double height) { return plot.width(); }
    @Override protected double computePrefHeight(double width) { return plot.height() * (width > 0 ? width / plot.width() : 1); }
    @Override protected void layoutChildren() {
        double scale = getWidth() / plot.width();
        canvas.setWidth(getWidth()); canvas.setHeight(plot.height() * scale);
        var g = canvas.getGraphicsContext2D(); g.setTransform(1, 0, 0, 1, 0, 0);
        g.setFill(Color.WHITE); g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight()); g.scale(scale, scale);
        for (var shape : plot.shapes()) {
            if (shape instanceof PlotDocument.Line l) { g.setStroke(Color.web(l.color())); g.setLineWidth(l.thickness()); g.strokeLine(l.x1(), l.y1(), l.x2(), l.y2()); }
            else if (shape instanceof PlotDocument.Rect r) { g.setFill(Color.web(r.fill())); g.fillRect(r.x(), r.y(), r.w(), r.h()); g.setStroke(Color.web(r.stroke())); g.setLineWidth(2); g.strokeRect(r.x(), r.y(), r.w(), r.h()); }
            else if (shape instanceof PlotDocument.Dot d) { g.setFill(Color.web(d.color())); g.fillOval(d.x()-d.radius(), d.y()-d.radius(), d.radius()*2, d.radius()*2); }
            else if (shape instanceof PlotDocument.Text t) { g.setFill(Color.web(t.color())); g.setFont(Font.font("SansSerif", t.size())); g.fillText(t.text(), t.x(), t.y()); }
        }
    }
}
