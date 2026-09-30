package edu.labs;

import static edu.labs.AnalysisData.*;
/** Подготовка: проверка объёма и сортировка. Повторы сохраняются. */
public final class PrepareFilter implements Filter<Numeric, Numeric> {
    public Numeric process(Numeric input) {
        var columns = input.columns().stream().map(c -> {
            int required = input.request().sample() ? 2 : 1;
            if (c.values().size() < required) throw new IllegalArgumentException(c.name() + ": нужно минимум " + required + " числовых значений");
            return new Column(c.name(), c.values().stream().sorted().toList());
        }).toList();
        return new Numeric(input.request(), columns, input.warnings());
    }
}
