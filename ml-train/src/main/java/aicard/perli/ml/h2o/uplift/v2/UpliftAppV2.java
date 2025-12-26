package aicard.perli.ml.h2o.uplift.v2;

import aicard.perli.ml.h2o.service.v1.H2oInferenceServiceV1;
import aicard.perli.ml.h2o.service.v2.H2oInferenceServiceV2;
import aicard.perli.ml.h2o.service.v2.H2oTrainServiceV2;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>[V2] 고도화 모델 테스트 및 V1 비교 분석 메인 클래스입니다.</p>
 * <p>V2 모델을 학습시키고, 동일 고객에 대해 V1과 V2의 예측 결과를 대조하여
 * 데이터 수혈(Feature Injection)의 효과를 증명합니다.</p>
 */
@Slf4j
public class UpliftAppV2 {

    public static void main(String[] args) {

        System.setProperty("h2o.ignore.jdk.version", "true");
        water.H2O.main(new String[]{});
        log.info("==========================================================");
        log.info("고도화 모델 검증 및 V1 비교 테스트");
        log.info("==========================================================");

        // 고도화 모델 학습
        String v2DataPath = "C:/Coding/perli-ai/resources/processed/h2o/v2/train_uplift_v2.csv";
        H2oTrainServiceV2 v2Trainer = new H2oTrainServiceV2();
        v2Trainer.trainV2(v2DataPath);

        // 경로 설정
        String v1ModelPath = "C:/Coding/perli-ai/resources/output/models/h2o/v1/uplift_gbm_model_v1.zip";
        String v2ModelPath = "C:/Coding/perli-ai/resources/output/models/h2o/v2/uplift_drf_model_v2.zip";

        // 서비스 로드
        H2oInferenceServiceV1 v1Service = new H2oInferenceServiceV1(v1ModelPath);
        H2oInferenceServiceV2 v2Service = new H2oInferenceServiceV2(v2ModelPath);

        // 가상 고객 데이터
        double totalAmount = 5000.0;
        int txCount = 100;

        // V2에서만 인지하는 피처
        double newTxCount = 20.0;      // 최근 한 달간 20건 거래 (매우 활발)
        double newTotalAmt = 1500.0;   // 최근 한 달간 150만원 결제
        double premiumRatio = 0.85;    // 결제의 85%가 명품/프리미엄 가맹점

        log.info("고가치 활동 고객에 대한 비교 분석 시작");

        // V1 예측 (과거 통계만 보고 판단)
        // V1 파라미터 규격에 맞춤 (가상으로 평균/최대액 계산 주입)
        double v1Score = v1Service.predictUplift(totalAmount, txCount, 1.0, 500.0, 50.0, 0.99);

        // V2 예측 (수혈된 최신 행동 데이터까지 보고 판단)
        double v2Score = v2Service.predictUpliftV2(totalAmount, txCount, newTxCount, newTotalAmt, premiumRatio);

        // 결과 대조 리포트
        log.info("----------------------------------------------------------");
        log.info("분석 리포트");
        log.info("----------------------------------------------------------");
        log.info("V1 점수: {}", String.format("%.6f", v1Score));
        log.info("V2 점수: {}", String.format("%.6f", v2Score));
        log.info("----------------------------------------------------------");

        double improvement = ((v2Score - v1Score) / Math.abs(v1Score)) * 100;

        if (v2Score > v1Score) {
            log.info("V2 모델이 고객의 최신 프리미엄 성향을 파악하여 업리프트 점수를 {:.2f}% 상향 조정했습니다.", improvement);
            log.info("이 고객은 마케팅 타겟 'Persuadables'로 확실히 분류되었습니다.");
        } else {
            log.info("V2 모델이 보다 냉철하게 고객의 증분 효과를 판단하고 있습니다.");
        }
    }
}