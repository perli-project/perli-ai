package aicard.perli.ml.h2o.eval.v1;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpliftMetricsV1Test {

    @Test
    void evaluateProducesPositiveSignalWhenRankingIsGood() {
        List<UpliftMetricsV1.ScoredSample> samples = List.of(
                new UpliftMetricsV1.ScoredSample(0.95, 1, 1),
                new UpliftMetricsV1.ScoredSample(0.90, 1, 1),
                new UpliftMetricsV1.ScoredSample(0.70, 0, 0),
                new UpliftMetricsV1.ScoredSample(0.20, 0, 1),
                new UpliftMetricsV1.ScoredSample(0.10, 1, 0),
                new UpliftMetricsV1.ScoredSample(0.05, 0, 0)
        );

        UpliftMetricsV1.UpliftMetricResult result = UpliftMetricsV1.evaluate(samples);
        assertTrue(result.getAuuc() > 0.0);
    }
}
