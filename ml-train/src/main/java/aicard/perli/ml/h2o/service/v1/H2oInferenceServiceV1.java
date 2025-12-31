package aicard.perli.ml.h2o.service.v1;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

/**
 * V1 학습 모델 전용 실시간 추론 서비스입니다.
 * <p>
 * 뼈대 피처(과거 통계 8종)를 입력받아 S-Learner 기반의 업리프트 점수를 산출합니다.
 * H2O MOJO(Model Object, Optimized) 포맷을 사용하여 JVM 환경에서 별도의 의존성 없이
 * 초고속 스코어링 연산을 수행합니다.
 * </p>
 */
@Slf4j
public class H2oInferenceServiceV1 {

    /** H2O 모델 예측을 최적화된 방식으로 처리하는 래퍼 객체 */
    private EasyPredictModelWrapper modelWrapper;

    /**
     * 생성자를 통해 지정된 경로의 MOJO 모델을 로드합니다.
     * <p>
     * 모델 파일의 존재 여부를 검증하고, H2O의 고속 추론 엔진을 메모리에 상주시켜
     * 실시간 요청에 즉각 대응할 수 있도록 준비합니다.
     * </p>
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
     * 추천 처치에 따른 순수 증분 효과(Uplift)를 계산합니다.
     * <p>
     * S-Learner 원리에 따라 처치 변수(is_recommended)를 1.0과 0.0으로 시뮬레이션한 차이값을 반환합니다.
     * 이를 통해 특정 카드 추천이 유저의 로열티 점수를 실제로 얼마나 끌어올리는지(Treatment Effect)를 측정합니다.
     * </p>
     * @param totalAmount 총 결제액
     * @param txCount 결제 건수
     * @param avgInstallments 평균 할부
     * @param maxAmount 최대 결제액
     * @param avgAmount 평균 결제액
     * @param authRatio 승인율
     * @return Uplift Score (추천 시 기대 로열티 증분 가치)
     */
    public double predictUplift(double totalAmount, int txCount, double avgInstallments,
                                double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) return 0.0;

            // 처치 변수를 1.0으로 설정하여 결과 예측
            RowData treatmentRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            treatmentRow.put("is_recommended", 1.0);
            double scoreWithAction = ((RegressionModelPrediction) modelWrapper.predict(treatmentRow)).value;

            // 처치 변수를 0.0으로 설정하여 결과 예측
            RowData controlRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            controlRow.put("is_recommended", 0.0);
            double scoreWithoutAction = ((RegressionModelPrediction) modelWrapper.predict(controlRow)).value;

            // 두 시나리오 간의 차이(Incremental Benefit) 산출
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
     * <p>
     * 처치 변수의 조작 없이 현재 유저의 상태 피처만을 바탕으로 모델이 예측한 기대값을 반환합니다.
     * </p>
     * @param totalAmount 총 결제액
     * @param txCount 결제 건수
     * @param avgInstallments 평균 할부
     * @param maxAmount 최대 결제액
     * @param avgAmount 평균 결제액
     * @param authRatio 승인율
     * @return 현재 상태의 예측 로열티 점수
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
     * <p>
     * H2O EasyPredict 래퍼가 요구하는 RowData 형식으로 수치형 피처들을 매핑합니다.
     * </p>
     * @return H2O 모델 입력용 RowData 객체
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