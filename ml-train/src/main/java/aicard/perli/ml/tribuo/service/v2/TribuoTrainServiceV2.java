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
 * <p>[V2] XGBoost 알고리즘을 활용한 고도화 랭킹 모델 학습 서비스입니다.</p>
 *
 * <p>수혈된 피처의 비선형 패턴을 학습하기 위해 150개의 결정 트리를 사용하는 앙상블 모델을 구축하며,
 * 주입된 {@link TribuoDataConverterV2}를 사용하여 학습 환경을 조성합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoTrainServiceV2 {

    /** 데이터 변환 유틸리티 (RequiredArgsConstructor 주입) */
    private final TribuoDataConverterV2 dataConverter;

    /**
     * <p>XGBoost 하이퍼파라미터를 최적화하여 모델을 학습하고 .gdpc 파일로 저장합니다.</p>
     *
     * @param csvPath   학습 데이터 경로
     * @param modelPath 모델 저장 파일 경로
     */
    public void trainV2(String csvPath, String modelPath) {
        log.info("Tribuo XGBoost 학습 시작");

        try {
            // 데이터 로드
            MutableDataset<Regressor> trainDataset = dataConverter.loadV2Dataset(csvPath);

            // XGBoost Trainer 설정
            XGBoostRegressionTrainer trainer = new XGBoostRegressionTrainer(
                    XGBoostRegressionTrainer.RegressionType.LINEAR,
                    150,      // 나무 개수
                    0.1,      // 학습률 (eta)
                    0.1,      // 감마 (최소 손실 감소)
                    15,       // 트리 최대 깊이
                    1.0,      // 최소 자식 가중치
                    0.8,      // 샘플링 비율
                    0.8,      // 피처 샘플링 비율
                    1.0,      // L2 정규화
                    0.1,      // L1 정규화
                    4,        // 병렬 스레드 수
                    true,     // 상세 로그 억제
                    777L      // 결과 재현 시드
            );

            log.info("XGBoost 앙상블 빌드 중...");
            Model<Regressor> model = trainer.train(trainDataset);

            //모델 저장
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelPath))) {
                oos.writeObject(model);
            }
            log.info("고도화 모델 저장 완료: {}", modelPath);

        } catch (Exception e) {
            log.error("학습 중 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}