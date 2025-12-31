package aicard.perli.dl.lstm.service.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
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
 * 다차원 결제 데이터(10개 피처)를 학습하는 LSTM 지출 예측 모델 서비스 클래스입니다.
 * <p>
 * 시계열 금융 데이터와 확장 분석 피처를 통합 로드하여 신경망 모델을 생성하고,
 * 100회 반복 학습(Epochs)을 통해 예측 모델을 최적화한 뒤 파일로 저장하는 기능을 수행합니다.
 * </p>
 */
@Getter
@Slf4j
public class LstmPredictorEvaluationV1 {

    /** 실제 예측 및 학습 연산을 담당하는 DeepLearning4J 네트워크 객체 */
    private MultiLayerNetwork model;

    /** CSV 형식의 학습 데이터를 로드하는 유틸리티 */
    private final LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();

    /** DTO 데이터를 신경망 입력용 3D 텐서로 변환하는 유틸리티 */
    private final LstmDataConverterV1 converter = new LstmDataConverterV1();

    /**
     * 통합 데이터셋을 활용하여 전체 학습 프로세스를 실행하고 모델을 저장합니다.
     * <p>
     * 데이터 로딩, 텐서 변환, 모델 초기화, 학습 진행 및 물리 파일 저장의
     * 전체 파이프라인을 제어하며 시계열 패턴 최적화를 수행합니다.
     * </p>
     * @param csvPath 학습용 원천 데이터 CSV 경로
     * @param modelSavePath 학습 완료 모델(.zip)을 저장할 경로
     * @throws Exception 데이터 처리 또는 모델 저장 중 발생하는 예외
     */
    public void runTraining(String csvPath, String modelSavePath) throws Exception {
        // 데이터 로드 및 텐서 변환
        List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
        INDArray features = converter.toTrainingTensor(rawData);
        INDArray labels = createLabelTensor(rawData);

        // 모델 초기화 (입력 피처 10개, 출력 1개)
        initModel(10, 1);

        // 학습 수행
        log.info("LSTM 학습 시작 (총 {}건)", rawData.size());
        for (int i = 1; i <= 100; i++) {
            model.fit(features, labels);
            if (i % 10 == 0) log.info("Epoch {} 완료 - 최적화 진행 중...", i);
        }

        // 모델 저장
        File saveFile = new File(modelSavePath);
        model.save(saveFile, true);
        log.info("모델 저장 완료: {}", saveFile.getAbsolutePath());
    }

    /**
     * LSTM 네트워크의 물리적 구조 및 하이퍼파라미터를 초기화합니다.
     * <p>
     * Adam 옵티마이저와 Xavier 가중치 초기화를 사용하며,
     * 은닉층(LSTM)과 출력층(RnnOutputLayer)으로 구성된 순환 신경망(RNN)을 빌드합니다.
     * </p>
     * @param inputSize 입력 피처 차원 수 (시계열 3 + 확장 피처 7 = 10)
     * @param outputSize 출력 결과 노드 수 (1: 지출 예측액)
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
     * 학습용 정답지(Label)를 3D 텐서 형식으로 생성합니다.
     * <p>
     * [BatchSize, OutputSize, TimeStep] 구조를 형성하며,
     * 시계열 데이터의 특성을 고려하여 마지막 시점(t=5)에 실제 정답 데이터를 주입합니다.
     * </p>
     * @param list 입력 DTO 리스트
     * @return ND4J INDArray 형태의 라벨 텐서
     */
    private INDArray createLabelTensor(List<LstmAdvancedRequestV1> list) {
        int batchSize = list.size();
        // [BatchSize, 1, 6] 구조로 초기화
        INDArray labels = Nd4j.zeros(batchSize, 1, 6);
        for (int i = 0; i < batchSize; i++) {
            // 시계열의 종착점 인덱스(5)에 정답값 설정
            labels.putScalar(new int[]{i, 0, 5}, list.get(i).getLabel());
        }
        return labels;
    }
}