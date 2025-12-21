package aicard.perli.ml.service;

import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import hex.tree.gbm.GBMModel.GBMParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import water.Key;
import water.Scope;
import water.fvec.Frame;
import water.fvec.NFSFileVec;
import water.parser.ParseDataset;

import java.io.File;

import static water.H2O.*;

/**
 * H2O.ai 엔진을 활용하여 사용자의 카드 로열티 점수를 예측하는 GBM 모델을 학습시키는 서비스입니다.
 * <p>Lombok의 {@link Slf4j}를 통해 로깅을 수행하며, 가공된 피처 데이터를 기반으로 회귀 분석을 수행합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class MLTrainService {

    public void train(String dataPath) {

        // 로그 저장 폴더 지정
        String logDirPath = new File("C:/Coding/perli-ai/resources/output/logs").getAbsolutePath();
        log.info("H20 머신러닝 엔진 가동 중");
        main(new String[]{"-log_dir", logDirPath});

        try {
            Scope.enter();

            log.info("학습용 피처 데이터 로딩 시작: {}", dataPath);
            File f = new File(dataPath);
            if(!f.exists()) {
                log.error("학습 데이터를 찾을 수 없습니다 경로를 확인하세요: {}", dataPath);
                return;
            }

            NFSFileVec nfs = NFSFileVec.make(f);
            Frame fr = ParseDataset.parse(Key.make("train_frame"), nfs._key);
            log.info("데이터 로딩 완료 (총 행 수: {}, 총 열 수: {})", fr.numRows(), fr.numCols());

            // Predictors 및 Response 컬럼 설정
            String[] predictors = {
                    "total_amount", "tx_count", "avg_installments",
                    "max_amount", "avg_amount", "authorized_ratio"
            };
            String response = "target";

            // 알고리즘 파라미터 구성 (GBM)
            log.info("AI 모델 파라미터 구성 중");
            GBMParameters params = new GBMParameters();
            params._train = fr._key;
            params._response_column = response;
            // 학습에 사용하지 않는 식별자 제외
            params._ignored_columns = new String[]{"card_id"};

            // 모델 세부 설정
            params._ntrees = 500;                // 나무 개수를 500개로 설정 
            params._max_depth = 10;              // 나무 깊이를 깊게 설정
            params._learn_rate = 0.05;           // 학습률을 낮춰 더 세밀하게 최적화
            params._min_rows = 5;                // 노드당 최소 데이터 수 5개
            params._sample_rate = 0.8;           // 데이터의 80%만 무작위 사용 (과적합 방지)
            params._col_sample_rate = 0.8;       // 피처의 80%만 무작위 사용
            params._seed = 1234L;

            // 조기 종료 로직
            params._stopping_rounds = 5;         // 5번 연속 개선 안 되면 멈춤
            params._stopping_tolerance = 0.0001; // 아주 미세한 오차 개선까지 추적

            // 학습 프로세스 실행
            log.info("AI 모델 학습 프로세스 시작 (Algorithm: GBM)");
            GBM job = new GBM(params);
            GBMModel model = job.trainModel().get();

            // 학습 성능 지표 요약 로깅
            log.info("머신러닝 모델 학습 완료");
            log.info("Training RMSE (오차): {}", model._output._training_metrics.rmse());

            // 결과 모델 저장
            exportTrainedModel(model);

        } catch (Exception e) {
            log.error("모델 학습 중 예외 발생: {}", e.getMessage(), e);
        } finally {
            Scope.exit();
            // 학습이 끝난 후 H20 엔진 정지 (프로세스 종료)
            log.info("H20 엔진을 종료합니다.");
            shutdown(0);
        }
    }

    /**
     * 학습된 모델을 지정된 경로에 물리 파일로 저장합니다.
     * @param model 학습 완료된 GBM 모델 객체
     */
    private void exportTrainedModel(GBMModel model) {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/gbm_loyalty_model";
        try {
            File dir = new File("C:/Coding/perli-ai/resources/output/models");
            if (!dir.exists()) dir.mkdirs();

            model.exportBinaryModel(modelPath, true);
            log.info("모델 저장 성공: {}", modelPath);
        } catch (Exception e) {
            log.error("모델 저장 실패 (경로: {}): {}", modelPath, e.getMessage());
        }
    }
}
