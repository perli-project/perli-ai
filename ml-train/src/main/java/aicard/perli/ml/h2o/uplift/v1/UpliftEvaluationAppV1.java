package aicard.perli.ml.h2o.uplift.v1;

import aicard.perli.ml.h2o.eval.v1.UpliftMetricsV1;
import aicard.perli.ml.h2o.service.v1.H2oInferenceServiceTLearnerV1;
import aicard.perli.ml.h2o.service.v1.H2oInferenceServiceV1;
import aicard.perli.ml.h2o.service.v1.H2oTrainServiceTLearnerV1;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * S-Learner vs T-Learner 업리프트 품질 비교 앱.
 */
@Slf4j
public class UpliftEvaluationAppV1 {

    public static void main(String[] args) {
        int maxRows = args.length > 0 ? Integer.parseInt(args[0]) : 50000;

        String dataPath = resolvePath("resources/processed/h2o/v1/train_uplift_v1.csv").toString();
        String sModelPath = resolvePath("resources/output/models/h2o/v1/uplift_gbm_model_v1.zip").toString();
        String tTreatmentModelPath = resolvePath("resources/output/models/h2o/v1/uplift_treatment_gbm_v1.zip").toString();
        String tControlModelPath = resolvePath("resources/output/models/h2o/v1/uplift_control_gbm_v1.zip").toString();

        H2oTrainServiceTLearnerV1 tTrainer = new H2oTrainServiceTLearnerV1();
        tTrainer.train(dataPath, tTreatmentModelPath, tControlModelPath, maxRows);

        H2oInferenceServiceV1 sLearner = new H2oInferenceServiceV1(sModelPath);
        H2oInferenceServiceTLearnerV1 tLearner =
                new H2oInferenceServiceTLearnerV1(tTreatmentModelPath, tControlModelPath);

        try {
            List<UpliftMetricsV1.ScoredSample> sSamples = new ArrayList<>();
            List<UpliftMetricsV1.ScoredSample> tSamples = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(dataPath))) {
                String header = br.readLine();
                if (header == null) {
                    throw new IllegalStateException("평가 데이터가 비어 있습니다.");
                }

                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    if (count >= maxRows) {
                        break;
                    }
                    String[] v = line.split(",");
                    if (v.length < 9) {
                        continue;
                    }

                    double totalAmount = Double.parseDouble(v[1]);
                    int txCount = Integer.parseInt(v[2]);
                    double avgInstallments = Double.parseDouble(v[3]);
                    double maxAmount = Double.parseDouble(v[4]);
                    double avgAmount = Double.parseDouble(v[5]);
                    double authRatio = Double.parseDouble(v[6]);
                    double targetRaw = Double.parseDouble(v[7]);
                    int treatment = Integer.parseInt(v[8]);

                    int target = targetRaw > 0 ? 1 : 0;
                    double sScore = sLearner.predictUplift(
                            totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio, false
                    );
                    double tScore = tLearner.predictUplift(
                            totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio
                    );

                    sSamples.add(new UpliftMetricsV1.ScoredSample(sScore, treatment, target));
                    tSamples.add(new UpliftMetricsV1.ScoredSample(tScore, treatment, target));
                    count++;
                }
            }

            UpliftMetricsV1.UpliftMetricResult sResult = UpliftMetricsV1.evaluate(sSamples);
            UpliftMetricsV1.UpliftMetricResult tResult = UpliftMetricsV1.evaluate(tSamples);

            log.info("============== Uplift 평가 리포트 ==============");
            log.info("S-Learner => AUUC: {}, Qini: {}, TotalGain: {}",
                    String.format("%.6f", sResult.getAuuc()),
                    String.format("%.6f", sResult.getQini()),
                    String.format("%.4f", sResult.getTotalIncrementalGain()));
            log.info("T-Learner => AUUC: {}, Qini: {}, TotalGain: {}",
                    String.format("%.6f", tResult.getAuuc()),
                    String.format("%.6f", tResult.getQini()),
                    String.format("%.4f", tResult.getTotalIncrementalGain()));
            log.info("Best by Qini: {}", tResult.getQini() >= sResult.getQini() ? "T-Learner" : "S-Learner");

        } catch (Exception e) {
            log.error("업리프트 평가 실패", e);
            System.exit(1);
        }

        System.exit(0);
    }

    private static Path resolvePath(String candidate) {
        Path p1 = Paths.get(candidate);
        if (p1.toFile().exists()) {
            return p1;
        }
        Path p2 = Paths.get("..", candidate);
        if (p2.toFile().exists()) {
            return p2.normalize();
        }
        return p1;
    }
}
