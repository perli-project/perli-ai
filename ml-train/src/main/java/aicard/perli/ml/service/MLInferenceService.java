package aicard.perli.ml.service;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 학습된 MOJO 모델(.zip)을 로드하여 실시간 추론을 수행하는 서비스 클래스입니다.
 * <p>
 * H2O GenModel 라이브러리를 사용하여 무거운 H2O 엔진 없이도
 * 독립적인 환경(Web Server, 배치 등)에서 고성능 예측 기능을 제공합니다.
 * </p>
 */
@Slf4j
public class MLInferenceService {

    /**
     * H2O Easy Predict API를 래핑하여 타입 안전한 추론을 지원하는 객체입니다.
     */
    private EasyPredictModelWrapper modelWrapper;

    /**
     * 서비스 생성자로서 지정된 경로의 MOJO 모델 파일을 로드하여 추론 엔진을 초기화합니다.
     * * @param modelPath MOJO 모델 파일(.zip)이 위치한 서버의 물리적 경로
     */
    public MLInferenceService(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("모델 파일을 찾을 수 없습니다: {}", modelPath);
                return;
            }
            // MOJO 모델 로드 및 최적화된 추론 래퍼 생성
            this.modelWrapper = new EasyPredictModelWrapper(MojoModel.load(modelPath));
            log.info("MOJO 추론 엔진 로드 완료 (Path: {})", modelPath);
        } catch (Exception e) {
            log.error("추론 엔진 로딩 실패: {}", e.getMessage());
        }
    }

    /**
     * 입력된 고객의 거래 데이터를 바탕으로 로열티 점수를 예측합니다.
     * <p>
     * 입력 파라미터는 모델 학습 시 사용된 피처(Feature) 명칭과 정확히 매핑되어야 하며,
     * 내부적으로 모든 수치는 {@link Double} 타입으로 변환되어 처리됩니다.
     * </p>
     *
     * @param totalAmount      총 결제 금액
     * @param txCount          총 결제 건수
     * @param avgInstallments  평균 할부 개월 수
     * @param maxAmount        최대 결제 금액
     * @param avgAmount        평균 결제 금액
     * @param authRatio        결제 승인율 (0.0 ~ 1.0)
     * @return 예측된 로열티 점수 (회귀 분석 결과값), 엔진 미로드 시 0.0 반환
     */
    public double predict(double totalAmount, int txCount, double avgInstallments,
                          double maxAmount, double avgAmount, double authRatio) {
        try {
            if (modelWrapper == null) {
                log.warn("추론 엔진이 로드되지 않아 예측을 수행할 수 없습니다.");
                return 0.0;
            }

            // 입력 데이터를 H2O 전용 RowData 포맷으로 구성
            RowData row = new RowData();
            row.put("total_amount", Double.valueOf(totalAmount));
            row.put("tx_count", Double.valueOf(txCount));
            row.put("avg_installments", Double.valueOf(avgInstallments));
            row.put("max_amount", Double.valueOf(maxAmount));
            row.put("avg_amount", Double.valueOf(avgAmount));
            row.put("authorized_ratio", Double.valueOf(authRatio));

            // 모델 카테고리에 맞는 Regression 예측 실행
            RegressionModelPrediction p = (RegressionModelPrediction) modelWrapper.predict(row);
            return p.value;
        } catch (Exception e) {
            log.error("실시간 점수 계산 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
    }
}