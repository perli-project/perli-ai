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
 * LSTM 지출 예측 모델 학습 실행 어플리케이션 EvaluationV1
 */
@Slf4j
public class LstmEvaluationV1 {

    public static void main(String[] args) {
        LstmPredictorEvaluationV1 predictor = new LstmPredictorEvaluationV1();
        LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();
        LstmDataConverterV1 converter = new LstmDataConverterV1();

        // 경로 설정
        String csvPath = "C:/Coding/perli-ai/resources/processed/lstm/v1/train_lstm_v1.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v1/dl4j_lstm_model_evaluation_v1.zip";

        try {
            log.info("==== 학습 프로세스 시작 ====");


            // 통합 데이터 로드 (30,000건)
            List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
            log.info("데이터 로딩 완료: " + rawData.size() + " 건");

            // 모델 초기화 (입력 피처 10개로 고도화)
            predictor.initModel(10, 1);

            // 텐서 변환 및 정답지(Label) 생성
            INDArray features = converter.toTrainingTensor(rawData);

            // [BatchSize, Output, TimeStep] -> [30000, 1, 6]
            INDArray labels = Nd4j.zeros(rawData.size(), 1, 6);
            for (int i = 0; i < rawData.size(); i++) {
                // 6개월의 마지막 시점에 정답 주입
                labels.putScalar(new int[]{i, 0, 5}, rawData.get(i).getLabel());
            }

            // 학습 실행 (100 Epochs)
            log.info("Deep Learning 학습 진행 중");
            for (int i = 1; i <= 100; i++) {
                predictor.getModel().fit(features, labels);
                if (i % 10 == 0) log.info("Epoch [" + i + "/100] 완료");
            }

            log.info("모델 성능 검증 시작");

            // 테스트 데이터를 통한 예측 수행
            INDArray predicted = predictor.getModel().output(features);

            // regression용 Evaluation 객체 생성 (피처가 1개이므로 1로 설정)
            RegressionEvaluation eval = new RegressionEvaluation(1);
            eval.eval(labels, predicted);

            // RMSE, MAE 등이 상세히 출력됨
            log.info(eval.stats());

            // 완성된 모델 저장
            File saveFile = new File(modelPath);
            predictor.getModel().save(saveFile, true);

            log.info("====모델 생성 및 저장 완료 ====");
            log.info("경로: " + saveFile.getAbsolutePath());



        } catch (Exception e) {
            log.error("학습 도중 오류가 발생했습니다");
            e.printStackTrace();
        }
    }
}