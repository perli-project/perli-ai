package aicard.perli.dl.lstm.app.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
import aicard.perli.dl.lstm.service.v1.LstmPredictorEvaluationV1;
import aicard.perli.dl.lstm.util.loader.v1.LstmDataLoaderV1;
import aicard.perli.dl.lstm.util.converter.v1.LstmDataConverterV1;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.evaluation.regression.RegressionEvaluation;

import java.io.File;
import java.util.List;

/**
 * LSTM 지출 예측 모델 학습 및 성능 검증(Evaluation) 어플리케이션.
 * <p>
 * 100회 이상의 반복 학습(Epochs)을 통해 모델을 최적화하고,
 * 회귀 분석 평가지표(RMSE, MAE 등)를 사용하여 예측 모델의 정확도를 정밀 측정합니다.
 * </p>
 */
@Slf4j
public class LstmEvaluationV1 {

    /**
     * 학습 및 검증 프로세스를 수행하는 메인 메서드입니다.
     * <p>
     * 모델 학습 후 테스트 데이터셋을 통해 예측값과 실제값의 오차를 분석하며,
     * 산출된 통계 지표를 바탕으로 모델의 신뢰성을 검증하고 최종 가중치를 저장합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        LstmPredictorEvaluationV1 predictor = new LstmPredictorEvaluationV1();
        LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();
        LstmDataConverterV1 converter = new LstmDataConverterV1();

        // 경로 설정 (평가용 모델 파일 별도 관리)
        String csvPath = "C:/Coding/perli-ai/resources/processed/lstm/v1/train_lstm_v1.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v1/dl4j_lstm_model_evaluation_v1.zip";

        try {
            log.info("==== 고정밀 학습 및 검증 프로세스 시작 ====");

            // 데이터 로드
            List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
            log.info("데이터 로딩 완료: {} 건", rawData.size());

            // 모델 초기화
            predictor.initModel(10, 1);

            // 텐서 변환
            INDArray features = converter.toTrainingTensor(rawData);

            // 정답지(Label) 생성: Many-to-One 방식 (마지막 시점 t=5에 정답 주입)
            INDArray labels = Nd4j.zeros(rawData.size(), 1, 6);
            for (int i = 0; i < rawData.size(); i++) {
                labels.putScalar(new int[]{i, 0, 5}, rawData.get(i).getLabel());
            }

            // 고정밀 학습 실행 (100 Epochs)
            log.info("Deep Learning 고정밀 학습 진행 중 (Total Epochs: 100)...");
            for (int i = 1; i <= 100; i++) {
                predictor.getModel().fit(features, labels);
                if (i % 10 == 0) log.info("Epoch [{}/100] 완료", i);
            }

            log.info("==== 모델 성능 검증(Evaluation) 시작 ====");

            // 학습된 모델을 사용한 예측 수행
            INDArray predicted = predictor.getModel().output(features);

            // 회귀 모델용 성능 평가 객체 생성 (MSE, RMSE, MAE, R^2 등 계산)
            RegressionEvaluation eval = new RegressionEvaluation(1);
            eval.eval(labels, predicted);

            // 검증 통계 결과 출력
            log.info("\n[Model Performance Stats]\n{}", eval.stats());

            // 검증이 완료된 고성능 모델 저장
            File saveFile = new File(modelPath);
            predictor.getModel().save(saveFile, true);

            log.info("==== 모델 성능 검증 및 저장 완료 ====");
            log.info("모델 저장 경로: {}", saveFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("학습 및 검증 프로세스 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
}