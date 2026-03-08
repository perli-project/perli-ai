package aicard.perli.ml.h2o.eval.v1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 업리프트 랭킹 품질을 AUUC/Qini로 평가하는 유틸리티.
 */
public final class UpliftMetricsV1 {

    private UpliftMetricsV1() {
    }

    @Getter
    public static class ScoredSample {
        private final double upliftScore;
        private final int treatment;
        private final int target;

        public ScoredSample(double upliftScore, int treatment, int target) {
            this.upliftScore = upliftScore;
            this.treatment = treatment;
            this.target = target;
        }
    }

    @Getter
    public static class UpliftMetricResult {
        private final double auuc;
        private final double qini;
        private final double totalIncrementalGain;

        public UpliftMetricResult(double auuc, double qini, double totalIncrementalGain) {
            this.auuc = auuc;
            this.qini = qini;
            this.totalIncrementalGain = totalIncrementalGain;
        }
    }

    /**
     * 점수 내림차순으로 정렬한 뒤 누적 증분이득 곡선으로 AUUC/Qini를 계산합니다.
     */
    public static UpliftMetricResult evaluate(List<ScoredSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return new UpliftMetricResult(0.0, 0.0, 0.0);
        }

        List<ScoredSample> sorted = new ArrayList<>(samples);
        sorted.sort(Comparator.comparingDouble(ScoredSample::getUpliftScore).reversed());

        int n = sorted.size();
        double treatedCount = 0.0;
        double controlCount = 0.0;
        double treatedPositives = 0.0;
        double controlPositives = 0.0;

        List<Double> incrementalCurve = new ArrayList<>(n);

        for (ScoredSample row : sorted) {
            if (row.getTreatment() == 1) {
                treatedCount += 1.0;
                if (row.getTarget() == 1) {
                    treatedPositives += 1.0;
                }
            } else {
                controlCount += 1.0;
                if (row.getTarget() == 1) {
                    controlPositives += 1.0;
                }
            }

            double scaledControlPositives = controlCount == 0.0
                    ? 0.0
                    : controlPositives * (treatedCount / controlCount);
            double incremental = treatedPositives - scaledControlPositives;
            incrementalCurve.add(incremental);
        }

        double totalIncrementalGain = incrementalCurve.get(n - 1);

        double areaModel = 0.0;
        double areaRandom = 0.0;
        for (int i = 0; i < n; i++) {
            double xRatio = (i + 1) / (double) n;
            areaModel += incrementalCurve.get(i);
            areaRandom += xRatio * totalIncrementalGain;
        }

        double auuc = areaModel / n;
        double qini = (areaModel - areaRandom) / n;
        return new UpliftMetricResult(auuc, qini, totalIncrementalGain);
    }
}
