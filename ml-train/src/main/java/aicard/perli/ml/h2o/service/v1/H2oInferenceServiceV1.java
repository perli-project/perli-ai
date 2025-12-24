package aicard.perli.ml.h2o.service.v1;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

/**
 * <p>V1 학습 모델 전용 실시간 추론 서비스입니다.</p>
 * <p>뼈대 피처(과거 통계 8종)를 입력받아 S-Learner 기반의 업리프트 점수를 산출합니다.</p>
 */
@Slf4j
public class H2oInferenceServiceV1 {

    private EasyPredictModelWrapper modelWrapper;

    /**
     * 생성자를 통해 지정된 경로의 MOJO 모델을 로드합니다.
     *
     * @param modelPath 로드할 .zip 모델 파일의 경로
     */
    public H2oInferenceServiceV1(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("모델 파일을 찾을 수 없습니다: {}", modelPath);
                return;
            }
            this.modelWrapper = new EasyPredictModelWrapper(MojoModel.load(modelPath));
            log.info("추론 엔진 준비 완료");
        } catch (Exception e) {
            log.error("엔진 로딩 중 치명적 오류: {}", e.getMessage());
        }
    }

    /**
     * <p>추천 처치에 따른 순수 증분 효과(Uplift)를 계산합니다.</p>
     * <p>S-Learner 원리에 따라 처치 변수를 1.0과 0.0으로 시뮬레이션한 차이값을 반환합니다.</p>
     *
     * @param totalAmount 총 결제액
     * @param txCount 결제 건수
     * @param avgInstallments 평균 할부
     * @param maxAmount 최대 결제액
     * @param avgAmount 평균 결제액
     * @param authRatio 승인율
     * @return Uplift Score (추천 시 기대 로열티 증분)
     */
    public double predictUplift(double totalAmount, int txCount, double avgInstallments,
                                double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) return 0.0;

            // 시나리오 A: 추천 제공
            RowData treatmentRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            treatmentRow.put("is_recommended", 1.0);
            double scoreWithAction = ((RegressionModelPrediction) modelWrapper.predict(treatmentRow)).value;

            // 시나리오 B: 미제공 (통제군)
            RowData controlRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            controlRow.put("is_recommended", 0.0);
            double scoreWithoutAction = ((RegressionModelPrediction) modelWrapper.predict(controlRow)).value;

            double uplift = scoreWithAction - scoreWithoutAction;

            log.info("ScoreDiff: {}", String.format("%.4f", uplift));
            return uplift;

        } catch (Exception e) {
            log.error("점수 계산 실패: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 단순 로열티 점수를 예측합니다.
     */
    public double predict(double totalAmount, int txCount, double avgInstallments,
                          double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) return 0.0;
            RowData row = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            return ((RegressionModelPrediction) modelWrapper.predict(row)).value;
        } catch (Exception e) {
            log.error("예측 실패: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * V1 표준 규격에 맞는 입력 데이터를 생성합니다.
     */
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