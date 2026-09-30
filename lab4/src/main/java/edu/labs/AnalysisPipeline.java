package edu.labs;

import static edu.labs.AnalysisData.*;
public final class AnalysisPipeline {
    private final Pipeline<Request, Report> pipeline = new Pipeline<>(new ExcelLoadFilter())
        .then(new ParseFilter()).then(new PrepareFilter()).then(new StatisticsFilter()).then(new VisualizationFilter());
    public Report analyze(Request request) throws Exception { return pipeline.execute(request); }
}
