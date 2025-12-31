package aicard.perli.ml.h2o.service.v2;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.UpliftBinomialModelPrediction;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

/**
 * V2 고도화 Uplift 모델 기반 실시간 추론 서비스 클래스입니다.
 * <p>
 * 본 서비스는 H2O UpliftDRF 알고리즘으로 학습된 MOJO 모델을 활용하여,
 * 고객에게 특정 마케팅 처치(Treatment)를 수행했을 때의 순수 반응 증분 확률을 계산합니다.
 * </p>
 * <p><b>주요 특징:</b>
 * <ul>
 * <li><b>Advanced Feature Integration:</b> 수혈된 최신 행동 데이터(금액, 건수, 프리미엄 비중) 반영</li>
 * <li><b>Direct Uplift Scoring:</b> 별도의 뺄셈 연산 없이 모델 내부에서 산출된 순수 증분값(ITE) 추출</li>
 * <li><b>High Performance:</b> MOJO 포맷을 통한 밀리초(ms) 단위의 초고속 추론 성능</li>
 * </ul>
 * </p>
 */
@Slf4j
public class H2oInferenceServiceV2 {

    /** H2O Easy Predict API를 위한 최적화 래퍼 객체 */
    private EasyPredictModelWrapper modelWrapper;

    /**
     * Uplift MOJO 모델 파일을 로드하여 추론 엔진을 초기화합니다.
     * <p>
     * 모델 파일의 무결성을 검증하고, UpliftDRF 특화 예측 엔진을 메모리에 로드하여
     * 실시간 마케팅 타겟팅을 위한 스코어링 준비를 마칩니다.
     * </p>
     * @param modelPath 학습 완료된 uplift_drf_model_v2.zip 파일의 물리적 경로
     */
    public H2oInferenceServiceV2(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                log.error("모델 파일을 찾을 수 없습니다: {}", modelPath);
                return;
            }
            // MOJO 모델 로드 및 래퍼 생성
            this.modelWrapper = new EasyPredictModelWrapper(MojoModel.load(modelPath));
            log.info("고도화 추론 엔진 로드 성공");
        } catch (Exception e) {
            log.error("엔진 초기화 중 치명적 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 고객의 최신 활동 데이터를 입력받아 정밀 업리프트 점수를 산출합니다.
     * <p>
     * <b>작동 원리:</b><br>
     * 입력된 데이터를 바탕으로 모델 내부의 'Treatment' 트리군과 'Control' 트리군의 예측값 차이를
     * 직접 추출하여 반환합니다. 결과값이 클수록 마케팅 처치 시 반응 확률이 비약적으로 상승하는 'Persuadables(설득 가능군)'입니다.
     * </p>
     * @param totalAmount  과거 누적 총 결제 금액
     * @param txCount      과거 누적 총 결제 건수
     * @param newTxCount   최근 한 달간 거래 건수 (수혈 피처)
     * @param newTotalAmt  최근 한 달간 총 결제 금액 (수혈 피처)
     * @param premiumRatio 전체 거래 중 프리미엄 가맹점 결제 비중 (수혈 피처)
     * @return Uplift Score (P(Y=1|T=1) - P(Y=1|T=0)). 일반적으로 -1.0 ~ 1.0 사이의 확률 차이값.
     */
    public double predictUpliftV2(double totalAmount, int txCount,
                                  double newTxCount, double newTotalAmt, double premiumRatio) {
        try {
            if (modelWrapper == null) {
                log.warn("모델 래퍼가 초기화되지 않았습니다.");
                return 0.0;
            }

            // 모델 입력 규격에 맞게 RowData 구성
            RowData row = createV2Row(totalAmount, txCount, newTxCount, newTotalAmt, premiumRatio);

            // UpliftDRF 모델의 MOJO는 예측 결과로 UpliftBinomialModelPrediction을 반환합니다.
            UpliftBinomialModelPrediction p = (UpliftBinomialModelPrediction) modelWrapper.predict(row);

            // p.predictions[0] 에는 (P(Y=1|T=1) - P(Y=1|T=0)) 값이 모델 내부 로직에 의해 미리 계산되어 있음
            double upliftScore = p.predictions[0];

            log.info("Input(Amt: {}, Ratio: {}) -> Result Uplift: {}",
                    newTotalAmt, premiumRatio, String.format("%.6f", upliftScore));

            return upliftScore;

        } catch (Exception e) {
            log.error("점수 산출 중 예외 발생: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 추론을 위한 데이터 행(RowData) 객체를 생성하는 내부 헬퍼 메서드입니다.
     * <p>
     * V2 모델의 핵심인 수혈 피처(최신 거래 추세 및 가맹점 특성)를 포함하여 매핑합니다.
     * </p>
     * @param totalAmount  누적 금액
     * @param txCount      누적 건수
     * @param newTxCount   최신 건수
     * @param newTotalAmt  최신 금액
     * @param premiumRatio 프리미엄 비중
     * @return H2O 모델 입력용 RowData 객체
     */
    private RowData createV2Row(double totalAmount, int txCount,
                                double newTxCount, double newTotalAmt, double premiumRatio) {
        RowData row = new RowData();
        row.put("total_amount", totalAmount);
        row.put("tx_count", (double) txCount);
        row.put("new_tx_count", newTxCount);
        row.put("new_total_amt", newTotalAmt);
        row.put("premium_ratio", premiumRatio);
        return row;
    }
}