package aicard.perli.ml.h2o.service;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 학습된 MOJO 모델(.zip)을 로드하여 실시간 추론을 수행하는 서비스 클래스입니다.
 * <p>
 * 본 클래스는 단순 예측(Predict) 뿐만 아니라, S-Learner 알고리즘을 활용한
 * 업리프트 모델링(Uplift Modeling) 로직을 포함하여 마케팅 처치 효과를 수치화합니다.
 * </p>
 */
@Slf4j
public class H2oInferenceService {

    private EasyPredictModelWrapper modelWrapper;

    public H2oInferenceService(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("모델 파일을 찾을 수 없습니다: {}", modelPath);
                return;
            }
            this.modelWrapper = new EasyPredictModelWrapper(MojoModel.load(modelPath));
            log.info("MOJO 추론 엔진 로드 완료 (Path: {})", modelPath);
        } catch (Exception e) {
            log.error("추론 엔진 로딩 실패: {}", e.getMessage());
        }
    }

    /**
     * S-Learner 알고리즘을 사용하여 마케팅 추천의 '순수 증분 효과(Uplift Score)'를 계산합니다.
     * <p>
     * 동일한 고객 데이터셋에 대해 처치 변수({@code is_recommended})를 각각 1.0(추천)과 0.0(미추천)으로
     * 대조 주입하여 두 시나리오 간의 로열티 점수 차이를 산출합니다.
     * </p>
     *
     * @param totalAmount      총 결제 금액
     * @param txCount          총 결제 건수
     * @param avgInstallments  평균 할부 개월 수
     * @param maxAmount        최대 결제 금액
     * @param avgAmount        평균 결제 금액
     * @param authRatio        결제 승인율
     * @return 추천 여부에 따른 순수 로열티 점수 증분(Uplift), 계산 불가 시 0.0 반환
     */
    public double predictUplift(double totalAmount, int txCount, double avgInstallments,
                                double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) return 0.0;

            // 추천 시나리오 예측 (Treatment Group: is_recommended = 1.0)
            RowData treatmentRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            treatmentRow.put("is_recommended", 1.0);
            double scoreWithAction = ((RegressionModelPrediction) modelWrapper.predict(treatmentRow)).value;

            // 미추천 시나리오 예측 (Control Group: is_recommended = 0.0)
            RowData controlRow = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            controlRow.put("is_recommended", 0.0);
            double scoreWithoutAction = ((RegressionModelPrediction) modelWrapper.predict(controlRow)).value;

            // 증분 효과 계산 (Uplift = Treatment Score - Control Score)
            double upliftScore = scoreWithAction - scoreWithoutAction;

            log.info("추천시: {}, 미추천시: {}, 순수효과: {}",
                    String.format("%.4f", scoreWithAction),
                    String.format("%.4f", scoreWithoutAction),
                    String.format("%.4f", upliftScore));

            return upliftScore;
        } catch (Exception e) {
            log.error("업리프트 점수 계산 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 단순 로열티 점수를 예측합니다. (is_recommended 변수는 모델의 기본값 혹은 누락값으로 처리됨)
     */
    public double predict(double totalAmount, int txCount, double avgInstallments,
                          double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) return 0.0;
            RowData row = createBaseRow(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
            RegressionModelPrediction p = (RegressionModelPrediction) modelWrapper.predict(row);
            return p.value;
        } catch (Exception e) {
            log.error("예측 오류: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 반복되는 RowData 생성을 위한 내부 공통 헬퍼 메서드입니다.
     * 모든 수치형 데이터는 H2O MOJO 규격에 맞게 Double 타입으로 명시적 변환합니다.
     */
    private RowData createBaseRow(double totalAmount, int txCount, double avgInstallments,
                                  double maxAmount, double avgAmount, double authRatio) {
        RowData row = new RowData();
        row.put("total_amount", Double.valueOf(totalAmount));
        row.put("tx_count", Double.valueOf((double) txCount)); // Integer 에러 방지를 위해 Double 변환
        row.put("avg_installments", Double.valueOf(avgInstallments));
        row.put("max_amount", Double.valueOf(maxAmount));
        row.put("avg_amount", Double.valueOf(avgAmount));
        row.put("authorized_ratio", Double.valueOf(authRatio));
        return row;
    }
}