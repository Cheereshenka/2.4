package edu.labs;

import static edu.labs.AnalysisData.*;
public final class StatisticsFilter implements Filter<Numeric, Analyzed> {
    public Analyzed process(Numeric data) {
        return new Analyzed(data.columns().stream().map(c -> Statistics.calculate(c.name(), c.values(), data.request().sample())).toList(), data.warnings());
    }
}
