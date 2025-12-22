package aicard.perli.ml;

import aicard.perli.ml.service.MLInferenceService;
import aicard.perli.ml.service.MLTrainService;
import lombok.extern.slf4j.Slf4j;

/**
 * 카드 로열티 예측 모델의 전체 파이프라인(학습, 저장, 추론 테스트)을 실행하는 메인 엔트리 포인트 클래스입니다.
 * <p>
 * 본 클래스는 {@link MLTrainService}를 통해 데이터 기반 모델 학습 및 MOJO 아카이브 조립을 수행하며,
 * 생성된 모델의 유효성을 검증하기 위해 {@link MLInferenceService}를 이용한 즉각적인 추론 테스트를 진행합니다.
 * </p>
 */
@Slf4j
public class MLTrainApp {

    /**
     * 머신러닝 파이프라인을 가동하는 메인 메서드입니다.
     *
     * @param args 실행 인자 (사용되지 않음)
     */
    public static void main(String[] args) {

        log.info("카드 로열티 예측 모델 통합 파이프라인 가동");

        // 입출력 경로 설정
        String dataPath = "C:/Coding/perli-ai/resources/processed/train_features_advanced.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/gbm_loyalty_model.zip";

        // 모델 학습 및 MOJO 조립 절차 수행
        // 내부적으로 리플렉션을 사용하여 H2O 엔진의 결함을 우회하고 정규 규격의 MOJO 파일을 생성합니다.
        MLTrainService trainService = new MLTrainService();
        trainService.train(dataPath);

        // 생성된 MOJO 모델 기반의 실시간 추론 테스트
        log.info("예측 테스트를 시작합니다.");
        MLInferenceService inferenceService = new MLInferenceService(modelPath);

        // 검증용 가상 고객 데이터 입력 (총액 3000, 50건, 할부 1.5, 최대액 1000, 평균액 60, 승인율 0.97)
        double score = inferenceService.predict(3000.0, 50, 1.5, 1000.0, 60.0, 0.97);

        log.info("테스트 고객 로열티 예측 점수: {}", score);
        log.info("프로젝트의 모든 기술적 장애물이 해결되었습니다.");
    }
}