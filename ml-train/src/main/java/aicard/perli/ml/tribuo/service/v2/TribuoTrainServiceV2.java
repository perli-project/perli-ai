package aicard.perli.ml.tribuo.service.v2;

import aicard.perli.ml.tribuo.util.v2.TribuoDataConverterV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.xgboost.XGBoostRegressionTrainer;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * V2 XGBoost 알고리즘을 활용한 고도화 랭킹 모델 학습 서비스 클래스입니다.
 * <p>
 * 수혈된 피처의 비선형 패턴을 학습하기 위해 150개의 결정 트리를 사용하는 앙상블 모델을 구축하며,
 * 주입된 {@link TribuoDataConverterV2}를 사용하여 대용량 데이터셋에 최적화된 학습 환경을 조성합니다.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoTrainServiceV2 {

    /** 고도화된 수혈 피처를 Tribuo 규격으로 변환하는 전용 유틸리티 */
    private final TribuoDataConverterV2 dataConverter;

    /**
     * XGBoost 하이퍼파라미터를 최적화하여 고정밀 랭킹 모델을 학습하고 .gdpc 파일로 저장합니다.
     * <p>
     * 앙상블 기법을 통해 다수의 약한 학습기(Weak Learner)를 결합하며,
     * L1/L2 정규화와 트리 깊이 제어를 통해 모델의 일반화 성능을 극대화합니다.
     * </p>
     * @param csvPath   학습 데이터(수혈 피처 포함)가 위치한 경로
     * @param modelPath 학습 완료된 모델을 직렬화하여 저장할 경로 (.gdpc)
     */
    public void trainV2(String csvPath, String modelPath) {
        log.info("Tribuo XGBoost 고도화 학습 시작");

        try {
            // 전용 컨버터를 통한 고도화 데이터셋 로드
            MutableDataset<Regressor> trainDataset = dataConverter.loadV2Dataset(csvPath);

            // XGBoost Trainer 정밀 설정 (Boosting Parameters)
            // 비선형 관계 추출 및 과적합 방지를 위한 하이퍼파라미터 튜닝
            XGBoostRegressionTrainer trainer = new XGBoostRegressionTrainer(
                    XGBoostRegressionTrainer.RegressionType.LINEAR,
                    150,      // n_estimators: 부스팅을 수행할 나무의 총 개수
                    0.1,      // learning_rate (eta): 각 트리의 기여도를 조절하여 수렴 안정성 확보
                    0.1,      // gamma: 트리의 리프 노드를 추가 분할하기 위한 최소 손실 감소 값
                    15,       // max_depth: 복잡한 피처 상호작용을 포착하기 위한 트리의 최대 깊이
                    1.0,      // min_child_weight: 과적합 방지를 위한 관측치 가중치 합의 최솟값
                    0.8,      // subsample: 각 트리 학습에 사용할 데이터 샘플링 비율 (80%)
                    0.8,      // colsample_bytree: 각 트리 학습에 사용할 피처 샘플링 비율 (80%)
                    1.0,      // lambda (L2): 가중치에 대한 L2 정규화 강도
                    0.1,      // alpha (L1): 희소 피처 처리를 위한 L1 정규화 강도
                    4,        // nthread: 병렬 처리를 위한 CPU 스레드 수
                    true,     // silent: 상세 로그 출력 억제 여부
                    777L      // seed: 결과의 재현성을 보장하기 위한 난수 시드
            );

            log.info("XGBoost 앙상블 빌드 중 (Trees: 150, Max Depth: 15)...");
            Model<Regressor> model = trainer.train(trainDataset);

            // 모델 직렬화 저장 (Persistence)
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelPath))) {
                oos.writeObject(model);
            }
            log.info("고도화 모델 저장 완료: {}", modelPath);

        } catch (Exception e) {
            log.error("학습 중 치명적 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}