package aicard.perli.ml.h2o;

import aicard.perli.ml.h2o.service.H2oInferenceService;
import aicard.perli.ml.h2o.service.H2oTrainService;
import lombok.extern.slf4j.Slf4j;

/**
 * 카드 로열티 예측 모델의 전체 파이프라인(학습, 저장, 추론 테스트)을 실행하는 메인 엔트리 포인트 클래스입니다.
 * <p>
 * 업리프트 모델링(Uplift Modeling)을 지원하며, 추천 여부에 따른 고객 로열티의 증분 변화량을 측정합니다.
 * </p>
 */
@Slf4j
public class UpliftApp {

    public static void main(String[] args) {

        log.info("[S-Learner] 카드 로열티 업리프트 파이프라인 가동");

        // 입출력 경로 설정 (업리프트 전용 데이터 및 모델 경로)
        String dataPath = "C:/Coding/perli-ai/resources/processed/uplift/train_features_uplift.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/uplift/gbm_uplift_model.zip";

        // 모델 학습 및 MOJO 조립 절차 수행
        H2oTrainService trainService = new H2oTrainService();
        trainService.train(dataPath);

        // 생성된 MOJO 모델 기반의 추론 서비스 초기화
        log.info("실시간 추론 및 증분 분석 테스트 시작");
        H2oInferenceService inferenceService = new H2oInferenceService(modelPath);

        // 4. 가상 고객 데이터 (총액 3000, 50건, 할부 1.5, 최대액 1000, 평균액 60, 승인율 0.97)
        double totalAmount = 3000.0;
        int txCount = 50;
        double avgInstallments = 1.5;
        double maxAmount = 1000.0;
        double avgAmount = 60.0;
        double authRatio = 0.97;

        // 단순 예측
        double baseScore = inferenceService.predict(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
        log.info("테스트 고객 베이스라인 로열티 점수: {}", baseScore);

        // 업리프트 분석
        double upliftScore = inferenceService.predictUplift(totalAmount, txCount, avgInstallments, maxAmount, avgAmount, authRatio);
        log.info("최종 분석 결과: 순수 증분 효과(Uplift Score) = {}", String.format("%.6f", upliftScore));

        if (upliftScore > 0) {
            log.info("추천 시 점수 상승 확정: 마케팅 타겟(Persuadables) 그룹입니다.");
        } else {
            log.info("추천 효과 미비: 비용 절감이 필요한(Sure Things 또는 Lost Causes) 그룹입니다.");
        }
        log.info("프로젝트의 업리프트 모델링이 완벽히 가동되었습니다.");
    }
}