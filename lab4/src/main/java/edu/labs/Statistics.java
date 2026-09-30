package edu.labs;

import java.util.*;
public final class Statistics {
    public record Result(String name, int count, double mean, double standardDeviation, double q1, double median, double q3,
                         double iqr, double lowerWhisker, double upperWhisker, List<Double> outliers, List<Double> sorted, boolean sample) {}
    private Statistics() {}
    /** Квантили типа 7: линейная интерполяция в позиции (n - 1) * p. */
    public static double quantile(List<Double> sorted, double p) {
        if (sorted.isEmpty() || p < 0 || p > 1 || !Double.isFinite(p)) throw new IllegalArgumentException("Некорректный квантиль");
        double position = (sorted.size() - 1) * p; int low = (int) position; int high = Math.min(low + 1, sorted.size() - 1);
        double weight = position - low; return sorted.get(low) * (1 - weight) + sorted.get(high) * weight;
    }
    public static Result calculate(String name, List<Double> values, boolean sample) {
        if (values.isEmpty()) throw new IllegalArgumentException(name + ": нет числовых данных");
        if (sample && values.size() < 2) throw new IllegalArgumentException(name + ": для несмещённой дисперсии нужно минимум 2 значения");
        if (values.stream().anyMatch(v -> v == null || !Double.isFinite(v))) throw new IllegalArgumentException("Требуются конечные числа");
        List<Double> sorted = values.stream().sorted().toList();
        // Алгоритм Уэлфорда уменьшает потерю точности при близких значениях.
        double mean = 0, m2 = 0; int n = 0;
        for (double x : values) { n++; double delta = x - mean; mean += delta / n; m2 += delta * (x - mean); }
        double std = Math.sqrt(Math.max(0, m2 / (sample ? n - 1 : n)));
        double q1 = quantile(sorted, .25), median = quantile(sorted, .5), q3 = quantile(sorted, .75), iqr = q3 - q1;
        double low = q1 - 1.5 * iqr, high = q3 + 1.5 * iqr;
        if (!Double.isFinite(mean) || !Double.isFinite(std) || !Double.isFinite(low) || !Double.isFinite(high))
            throw new IllegalArgumentException(name + ": слишком большие числа для расчёта double");
        List<Double> outliers = sorted.stream().filter(v -> v < low || v > high).toList();
        double whiskerLow = sorted.stream().filter(v -> v >= low).findFirst().orElse(q1);
        double whiskerHigh = sorted.stream().filter(v -> v <= high).reduce((a,b) -> b).orElse(q3);
        return new Result(name, n, mean, std, q1, median, q3, iqr, whiskerLow, whiskerHigh, outliers, sorted, sample);
    }
}
