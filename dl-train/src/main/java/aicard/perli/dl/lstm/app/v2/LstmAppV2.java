package aicard.perli.dl.lstm.app.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import aicard.perli.dl.lstm.service.v2.LstmPredictorV2;
import aicard.perli.dl.lstm.util.converter.v2.LstmDataConverterV2;
import aicard.perli.dl.lstm.util.loader.v2.LstmDataLoaderV2;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.evaluation.regression.RegressionEvaluation;

import java.io.File;
import java.util.List;

/**
 * LSTM 지출 예측 모델 학습 실행 어플리케이션 V2.
 * <p>
 * 입력 피처를 16개로 확장하여 지출 패턴의 다각도 분석을 수행하며,
 * 1000회의 고반복 학습(Epochs)을 통해 예측 오차를 최소화한 초개인화 모델을 생성합니다.
 * </p>
 */
@Slf4j
public class LstmAppV2 {

    /**
     * V2 고도화 모델 학습 및 성능 검증 프로세스를 실행합니다.
     * <p>
     * 확장된 피처 세트를 기반으로 고차원 텐서를 생성하고, 1000회에 걸친 대규모 에포크 학습 후
     * 회귀 분석 평가지표(RMSE, MAE 등)를 통해 모델의 완성도를 정밀하게 측정합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        LstmPredictorV2 predictor = new LstmPredictorV2();
        LstmDataLoaderV2 dataLoader = new LstmDataLoaderV2();
        LstmDataConverterV2 converter = new LstmDataConverterV2();

        // 고도화된 V2 데이터 및 모델 경로 설정
        String csvPath = "C:/Coding/perli-ai/resources/processed/lstm/v2/train_lstm_v2.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v2/dl4j_lstm_model_v2.zip";

        try {
            log.info("==== V2 초개인화 학습 프로세스 시작 ====");

            // 16개 피처가 포함된 고도화 데이터 로드 (30,000건)
            List<LstmAdvancedRequestV2> rawData = dataLoader.loadTrainingData(csvPath);
            log.info("V2 데이터 로딩 완료: {} 건", rawData.size());

            // 모델 초기화 (입력 피처: 16개로 확장, 출력: 1개)
            predictor.initModel(16, 1);

            // 텐서 변환 및 라벨링
            // Features Shape: [BatchSize, InputFeatures(16), TimeStep(6)]
            INDArray features = converter.toTrainingTensor(rawData);

            // 정답 데이터 텐서 생성 (Many-to-One 구조 유지)
            INDArray labels = Nd4j.zeros(rawData.size(), 1, 6);
            for (int i = 0; i < rawData.size(); i++) {
                // 시계열의 종착점(t=5)에 실제 소비 데이터를 매핑하여 학습 유도
                labels.putScalar(new int[]{i, 0, 5}, rawData.get(i).getLabel());
            }

            // 초정밀 학습 실행 (1000 Epochs)
            log.info("Deep Learning 고정밀 반복 학습 중 (Total: 1000 Epochs)...");
            for (int i = 1; i <= 1000; i++) {
                predictor.getModel().fit(features, labels);
                if (i % 10 == 0) {
                    log.info("Epoch [{}/1000] 완료 - 오차율 최적화 진행 중", i);
                }
            }

            log.info("==== V2 모델 성능 검증 시작 ====");

            // 예측 수행
            INDArray predicted = predictor.getModel().output(features);

            // 회귀 성능 지표 산출
            RegressionEvaluation eval = new RegressionEvaluation(1);
            eval.eval(labels, predicted);

            // RMSE, MAE, R^2 등 상세 스탯 로그 출력
            log.info("\n[V2 Model Evaluation Stats]\n{}", eval.stats());

            // 완성된 모델 저장 및 디렉토리 관리
            File saveFile = new File(modelPath);
            if (saveFile.getParentFile() != null && !saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            predictor.getModel().save(saveFile, true);

            log.info("==== V2 모델 생성 및 저장 완료 ====");
            log.info("최종 모델 저장 경로: {}", saveFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("V2 학습 프로세스 중 치명적 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
}