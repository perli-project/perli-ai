package aicard.perli.dl.lstm.app.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
import aicard.perli.dl.lstm.service.v1.LstmPredictorV1;
import aicard.perli.dl.lstm.util.loader.v1.LstmDataLoaderV1;
import aicard.perli.dl.lstm.util.converter.v1.LstmDataConverterV1;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.List;

/**
 * LSTM(Long Short-Term Memory) 지출 예측 모델 학습 실행 어플리케이션 V1.
 * <p>
 * 과거 6개월간의 개인별 지출 패턴(MCC, 시간대, 금액 등 10개 피처)을 시계열 데이터로 학습하여
 * 차월 예상 지출액을 예측하는 딥러닝 모델을 생성하고 저장합니다.
 * </p>
 */
@Slf4j
public class LstmAppV1 {

    /**
     * LSTM 모델 학습 프로세스를 제어하는 메인 메서드입니다.
     * <p>
     * 데이터 로딩, 모델 초기화, 텐서 변환, 반복 학습(Fit), 모델 저장으로 이어지는
     * 전체 딥러닝 파이프라인을 관장하며, 시계열 데이터의 특성에 최적화된 학습 환경을 구축합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        LstmPredictorV1 predictor = new LstmPredictorV1();
        LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();
        LstmDataConverterV1 converter = new LstmDataConverterV1();

        // 경로 설정
        String csvPath = "C:/Coding/perli-ai/resources/processed/lstm/v1/train_lstm_v1.csv";
        String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v1/dl4j_lstm_model_v1.zip";

        try {
            log.info("==== LSTM 학습 프로세스 시작 ====");

            // 통합 데이터 로드 (30,000건의 시계열 데이터 세트)
            List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
            log.info("데이터 로딩 완료: {} 건", rawData.size());

            // 모델 초기화 (입력 피처: 10개, 출력 노드: 1개 - 회귀 분석 모델)
            predictor.initModel(10, 1);

            // 텐서 변환 및 정답지(Label) 생성
            // Features Shape: [BatchSize, InputFeatures(10), TimeStep(6)]
            INDArray features = converter.toTrainingTensor(rawData);

            // Labels 구조 정의: [BatchSize, Output(1), TimeStep(6)]
            // 시계열 학습의 특성에 따라 마지막 시점(t=5)에 정답 지출액을 주입
            INDArray labels = Nd4j.zeros(rawData.size(), 1, 6);
            for (int i = 0; i < rawData.size(); i++) {
                // 6개월 시계열의 마지막 인덱스(5)에 실제 지출액(Label) 매핑
                labels.putScalar(new int[]{i, 0, 5}, rawData.get(i).getLabel());
            }

            // 학습 실행 (50 Epochs 반복 학습)
            log.info("Deep Learning 모델 학습 진행 중 (Total Epochs: 50)...");
            for (int i = 1; i <= 50; i++) {
                predictor.getModel().fit(features, labels);
                if (i % 10 == 0) {
                    log.info("Epoch [{}/50] 완료 - 학습 오차율 최적화 중", i);
                }
            }

            // 완성된 학습 모델 로컬 경로에 저장 (압축 ZIP 포맷)
            File saveFile = new File(modelPath);
            predictor.getModel().save(saveFile, true);

            log.info("==== 모델 생성 및 저장 완료 ====");
            log.info("저장 경로: {}", saveFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("학습 프로세스 수행 중 예기치 못한 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
}