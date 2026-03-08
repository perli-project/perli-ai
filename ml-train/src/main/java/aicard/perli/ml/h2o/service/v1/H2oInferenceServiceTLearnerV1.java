package aicard.perli.ml.h2o.service.v1;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * H2O 기반 T-Learner 추론 서비스.
 * treatment/control 전용 모델을 각각 호출하여 uplift를 계산합니다.
 */
@Slf4j
public class H2oInferenceServiceTLearnerV1 {

    private EasyPredictModelWrapper treatmentModel;
    private EasyPredictModelWrapper controlModel;

    public H2oInferenceServiceTLearnerV1(String treatmentModelPath, String controlModelPath) {
        try {
            File treatmentFile = new File(treatmentModelPath);
            File controlFile = new File(controlModelPath);
            if (!treatmentFile.exists() || !controlFile.exists()) {
                log.error("T-Learner 모델 파일이 없습니다. treatment={}, control={}",
                        treatmentModelPath, controlModelPath);
                return;
            }

            this.treatmentModel = new EasyPredictModelWrapper(MojoModel.load(treatmentModelPath));
            this.controlModel = new EasyPredictModelWrapper(MojoModel.load(controlModelPath));
            log.info("T-Learner 추론 엔진 로드 완료");
        } catch (Exception e) {
            log.error("T-Learner 엔진 로드 실패: {}", e.getMessage());
        }
    }

    public double predictUplift(double totalAmount, int txCount, double avgInstallments,
                                double maxAmount, double avgAmount, double authRatio) {
        try {
            if (treatmentModel == null || controlModel == null) {
                return 0.0;
            }

            RowData row = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            double treatmentScore = ((RegressionModelPrediction) treatmentModel.predict(row)).value;
            double controlScore = ((RegressionModelPrediction) controlModel.predict(row)).value;
            return treatmentScore - controlScore;
        } catch (Exception e) {
            log.error("T-Learner uplift 계산 실패: {}", e.getMessage());
            return 0.0;
        }
    }

    private RowData createBaseRow(double totalAmount, int txCount, double avgInstallments,
                                  double maxAmount, double avgAmount, double authRatio) {
        RowData row = new RowData();
        row.put("total_amount", totalAmount);
        row.put("tx_count", (double) txCount);
        row.put("avg_installments", avgInstallments);
        row.put("max_amount", maxAmount);
        row.put("avg_amount", avgAmount);
        row.put("authorized_ratio", authRatio);
        return row;
    }
}
