package aicard.perli.dl.lstm.app.v2;

import aicard.perli.dl.lstm.dto.request.advanced.v2.LstmAdvancedRequestV2;
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
 * LSTM 지출 예측 모델 학습 실행 어플리케이션 V2
 */
@Slf4j
public class LstmAppV2 {

    public static void main(String[] args) {
        LstmPredictorV2 predictor = new LstmPredictorV2();
        LstmDataLoaderV2 dataLoader = new LstmDataLoaderV2();
        LstmDataConverterV2 converter = new LstmDataConverterV2();

        // 경로 설정
        String csvPath = "C:/Coding/perli-ai/resources/processed/lstm/v2/train_lstm_v2.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v2/dl4j_lstm_model_v2.zip";

        try {
            log.info("==== 학습 프로세스 시작 ====");


            // 통합 데이터 로드 (30,000건)
            List<LstmAdvancedRequestV2> rawData = dataLoader.loadTrainingData(csvPath);
            log.info("데이터 로딩 완료: " + rawData.size() + " 건");

            // 모델 초기화 (입력 피처 16개로 고도화)
            predictor.initModel(16, 1);

            // 텐서 변환 및 정답지(Label) 생성
            INDArray features = converter.toTrainingTensor(rawData);

            // [BatchSize, Output, TimeStep] -> [30000, 1, 6]
            INDArray labels = Nd4j.zeros(rawData.size(), 1, 6);
            for (int i = 0; i < rawData.size(); i++) {
                // 6개월의 마지막 시점에 정답 주입
                labels.putScalar(new int[]{i, 0, 5}, rawData.get(i).getLabel());
            }

            // 학습 실행 (200 Epochs)
            log.info("Deep Learning 학습 진행 중");
            for (int i = 1; i <= 200; i++) {
                predictor.getModel().fit(features, labels);
                if (i % 10 == 0) log.info("Epoch [" + i + "/200] 완료");
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

            if (saveFile.getParentFile() != null && !saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            predictor.getModel().save(saveFile, true);

            log.info("====모델 생성 및 저장 완료 ====");
            log.info("경로: " + saveFile.getAbsolutePath());



        } catch (Exception e) {
            log.error("학습 도중 오류가 발생했습니다");
            e.printStackTrace();
        }
    }
}