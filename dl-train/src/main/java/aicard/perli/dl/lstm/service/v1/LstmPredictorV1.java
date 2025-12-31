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
 * 다차원 결제 데이터(10개 피처)를 기반으로 지출 패턴을 학습하는 LSTM 예측 서비스 클래스입니다.
 * <p>
 * 시계열 금융 데이터와 확장 분석 피처를 결합하여 모델을 생성하며,
 * 특정 소비 상태나 패턴 발생 여부를 분류하고 예측하는 신경망 환경을 제공합니다.
 * </p>
 */
@Getter
@Slf4j
public class LstmPredictorV1 {

    /** 실제 딥러닝 연산을 처리하는 멀티레이어 신경망 객체 */
    private MultiLayerNetwork model;

    /** CSV 데이터를 로드하기 위한 유틸리티 */
    private final LstmDataLoaderV1 dataLoader = new LstmDataLoaderV1();

    /** 데이터를 3D 텐서 형식으로 변환하기 위한 컨버터 */
    private final LstmDataConverterV1 converter = new LstmDataConverterV1();

    /**
     * 통합 데이터셋을 활용하여 전체 학습 프로세스를 실행하고 모델을 파일로 저장합니다.
     * <p>
     * 데이터 로딩부터 텐서 생성, 50회 반복 학습 및 물리 파일(.zip) 직렬화까지의
     * 일련의 과정을 관리하며 지출 패턴 가중치를 생성합니다.
     * </p>
     * @param csvPath 학습용 원천 데이터 CSV 파일 경로
     * @param modelSavePath 학습 완료 모델을 저장할 물리적 경로
     * @throws Exception 입출력 또는 신경망 학습 중 발생하는 예외
     */
    public void runTraining(String csvPath, String modelSavePath) throws Exception {
        // 데이터 로드 및 텐서 변환
        List<LstmAdvancedRequestV1> rawData = dataLoader.loadTrainingData(csvPath);
        INDArray features = converter.toTrainingTensor(rawData);
        INDArray labels = createLabelTensor(rawData);

        // 모델 초기화 (입력 피처 10개, 출력 1개)
        initModel(10, 1);

        // 학습 수행 (50 Epochs 반복)
        log.info("LSTM 학습 시작 (데이터 규모: {}건)", rawData.size());
        for (int i = 1; i <= 50; i++) {
            model.fit(features, labels);
            if (i % 10 == 0) log.info("Epoch {} 완료 - 패턴 가중치 갱신 중...", i);
        }

        // 모델 저장
        File saveFile = new File(modelSavePath);
        model.save(saveFile, true);
        log.info("모델 저장 완료: {}", saveFile.getAbsolutePath());
    }

    /**
     * LSTM 네트워크의 계층 구조 및 학습 알고리즘을 초기화합니다.
     * <p>
     * Xavier 가중치 초기화와 Adam 최적화(Learning Rate: 0.005)를 사용하며,
     * 출력층은 분류 모델에 적합한 Sigmoid 활성화 함수와 XENT(Binary Cross Entropy) 손실 함수를 채택합니다.
     * </p>
     * @param inputSize 입력 피처 차원 (시계열 3 + 확장 피처 7 = 10)
     * @param outputSize 최종 출력 노드 수 (1)
     */
    public void initModel(int inputSize, int outputSize) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Adam(0.005))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(inputSize)
                        .nOut(64)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .activation(Activation.SIGMOID)
                        .nIn(64)
                        .nOut(outputSize)
                        .build())
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();
    }

    /**
     * 입력을 바탕으로 학습용 정답(Label) 텐서를 3D 구조로 생성합니다.
     * <p>
     * [BatchSize, OutputSize, TimeStep] 형태를 가지며,
     * Many-to-One 학습 모델을 위해 시계열의 종착점(Index 5)에 실제 라벨 값을 배치합니다.
     * </p>
     * @param list 입력 데이터 DTO 리스트
     * @return ND4J INDArray 형태의 라벨 텐서
     */
    private INDArray createLabelTensor(List<LstmAdvancedRequestV1> list) {
        int batchSize = list.size();
        // [BatchSize, 1, 6] 구조로 초기화
        INDArray labels = Nd4j.zeros(batchSize, 1, 6);
        for (int i = 0; i < batchSize; i++) {
            // 마지막 시점에 정답 주입
            labels.putScalar(new int[]{i, 0, 5}, list.get(i).getLabel());
        }
        return labels;
    }
}