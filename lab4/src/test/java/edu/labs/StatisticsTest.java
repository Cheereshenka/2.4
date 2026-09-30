package edu.labs;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StatisticsTest {
    @Test void knownQuartilesMeanAndBothDeviations() {
        var pop = Statistics.calculate("x", List.of(1.,2.,3.,4.), false);
        assertEquals(2.5, pop.mean()); assertEquals(1.75, pop.q1()); assertEquals(2.5, pop.median()); assertEquals(3.25, pop.q3()); assertEquals(1.5, pop.iqr());
        assertEquals(Math.sqrt(1.25), pop.standardDeviation(), 1e-12);
        assertEquals(Math.sqrt(5. / 3), Statistics.calculate("x", List.of(1.,2.,3.,4.), true).standardDeviation(), 1e-12);
    }
    @Test void outlierAndWhiskersAreObservedValues() {
        var r = Statistics.calculate("x", List.of(1.,2.,3.,4.,5.,100.), false);
        assertEquals(List.of(100.), r.outliers()); assertEquals(1., r.lowerWhisker()); assertEquals(5., r.upperWhisker());
    }
    @Test void equalValuesSingletonAndInvalidInput() {
        var r = Statistics.calculate("x", List.of(7.,7.,7.), true); assertEquals(0, r.standardDeviation()); assertEquals(0, r.iqr()); assertTrue(r.outliers().isEmpty());
        assertEquals(0, Statistics.calculate("x", List.of(7.), false).standardDeviation());
        assertThrows(IllegalArgumentException.class, () -> Statistics.calculate("x", List.of(7.), true));
        assertThrows(IllegalArgumentException.class, () -> Statistics.calculate("x", List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> Statistics.calculate("x", List.of(Double.NaN), false));
    }
}
