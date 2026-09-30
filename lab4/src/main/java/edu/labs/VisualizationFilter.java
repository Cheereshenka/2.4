package edu.labs;

import static edu.labs.AnalysisData.*;
public final class VisualizationFilter implements Filter<Analyzed, Report> {
    public Report process(Analyzed data) { return new Report(data.results(), data.warnings(), PlotDocument.boxPlot(data.results())); }
}
