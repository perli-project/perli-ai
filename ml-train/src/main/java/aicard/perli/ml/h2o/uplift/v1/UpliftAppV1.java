package aicard.perli.ml.h2o.uplift.v1;

import aicard.perli.ml.h2o.service.v1.H2oInferenceServiceV1;
import aicard.perli.ml.h2o.service.v1.H2oTrainServiceV1;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>[V1] 카드 로열티 예측 모델의 전체 파이프라인을 실행하는 메인 클래스입니다.</p>
 * <p>V1 표준 뼈대 피처셋을 사용하여 모델 학습, 수동 MOJO 조립, 추론 테스트를 순차적으로 수행합니다.</p>
 */
@Slf4j
public class UpliftAppV1 {

    public static void main(String[] args) {

        log.info("==========================================================");
        log.info("카드 로열티 업리프트 시스템 가동");
        log.info("==========================================================");

        // 뼈대 데이터 및 V1 모델 ZIP 경로
        String dataPath = "C:/Coding/perli-ai/resources/processed/uplift/v1/train_uplift_v1.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/uplift/v1/uplift_gbm_model_v1.zip";

        // 모델 학습 및 MOJO 조립 절차 수행 (Reflection 기반 버그 우회 포함)
        H2oTrainServiceV1 trainService = new H2oTrainServiceV1();
        trainService.train(dataPath);

        // 생성된 MOJO 모델 기반의 추론 서비스 초기화
        log.info("실시간 추론 및 증분 분석(Uplift) 테스트 시작");
        H2oInferenceServiceV1 inferenceService = new H2oInferenceServiceV1(modelPath);

        // 테스트용 가상 고객 데이터 (V1 표준 피처 8종 규격)
        // (데이터 예시: 총액 3000, 50건, 할부 1.5, 최대액 1000, 평균액 60, 승인율 0.97)
        double totalAmount = 3000.0;
        int txCount = 50;
        double avgInstallments = 1.5;
        double maxAmount = 1000.0;
        double avgAmount = 60.0;
        double authRatio = 0.97;

        // 단순 로열티 점수 예측 (Base Score)
        double baseScore = inferenceService.predict(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
        log.info("테스트 고객 베이스라인 점수: {}", baseScore);

        // 업리프트 분석 (S-Learner 알고리즘 가동)
        double upliftScore = inferenceService.predictUplift(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
        log.info("최종 분석 결과: 순수 증분 효과(Uplift Score) = {}", String.format("%.6f", upliftScore));

        // 결과 기반 마케팅 타겟팅 판단
        if (upliftScore > 0) {
            log.info("추천 시 점수 상승 확정: 마케팅 타겟(Persuadables) 그룹");
        } else {
            log.info("추천 효과 미비: 마케팅 제외 대상 그룹");
        }

        log.info("==========================================================");
        log.info("모든 프로세스가 완벽하게 종료되었습니다.");
        log.info("==========================================================");
    }
}