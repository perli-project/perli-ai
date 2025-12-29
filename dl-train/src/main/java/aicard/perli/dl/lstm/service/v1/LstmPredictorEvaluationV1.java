package aicard.perli.dl.lstm.service.v1;

import aicard.perli.dl.lstm.dto.request.advanced.v1.LstmAdvancedRequestV1;
import aicard.perli.dl.lstm.util.loader.v1.LstmDataLoaderV1;
import aicard.perli.dl.lstm.util.converter.v1.LstmDataConverterV1;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.util.List;

/**
 * 다차원 결제 데이터(10개 피처)를 학습하는 LSTM 지출 예측 모델 서비스.
 * 통합 데이터셋을 로드하여 모델을 생성하고 학습한 뒤 파일로 저장함.
 */
@Getter
@Slf4j
public class LstmPredictorEvaluationV1 {

    private MultiLayerNetwork model;
    private final LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();
    private final LstmDataConverterV1 converter = new LstmDataConverterV1();

    /**
     * 통합 데이터셋을 활용한 전체 학습 프로세스 실행 및 모델 저장.
     * @param csvPath 학습용 CSV 경로
     * @param modelSavePath 결과 모델(.zip) 저장 경로
     */
    public void runTraining(String csvPath, String modelSavePath) throws Exception {
        // 데이터 로드 및 텐서 변환
        List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
        INDArray features = converter.toTrainingTensor(rawData);
        INDArray labels = createLabelTensor(rawData);

        // 모델 초기화 (입력 피처 10개, 출력 1개)
        initModel(10, 1);

        // 학습 수행
        log.info("LSTM 학습 시작 (총 " + rawData.size() + "건)");
        for (int i = 1; i <= 100; i++) {
            model.fit(features, labels);
            if (i % 10 == 0) log.info("Epoch " + i + " 완료...");
        }

        // 모델 저장
        File saveFile = new File(modelSavePath);
        model.save(saveFile, true);
        log.info("모델 저장 완료: " + saveFile.getAbsolutePath());
    }

    /**
     * LSTM 네트워크 구조 초기화.
     * @param inputSize 피처 수 (시계열 3 + 확장 피처 7 = 10)
     * @param outputSize 출력 수 (예측 결과 1)
     */
    public void initModel(int inputSize, int outputSize) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Adam(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(inputSize)
                        .nOut(64)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(64)
                        .nOut(outputSize)
                        .build())
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();
    }

    /**
     * DTO 리스트에서 정답(Label) 텐서 생성.
     */
    private INDArray createLabelTensor(List<LstmAdvancedRequestV1> list) {
        int batchSize = list.size();
        // [BatchSize, OutputSize, TimeStep] 구조로 마지막 시점에 정답 배치
        INDArray labels = Nd4j.zeros(batchSize, 1, 6);
        for (int i = 0; i < batchSize; i++) {
            labels.putScalar(new int[]{i, 0, 5}, list.get(i).getLabel());
        }
        return labels;
    }
}