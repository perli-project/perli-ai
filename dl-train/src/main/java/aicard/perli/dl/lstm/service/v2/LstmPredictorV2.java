package aicard.perli.dl.lstm.service.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import aicard.perli.dl.lstm.util.converter.v2.LstmDataConverterV2;
import aicard.perli.dl.lstm.util.loader.v2.LstmDataLoaderV2;
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
 * 다차원 결제 데이터와 인구통계 정보를 학습하는 고도화된 Stacked LSTM 지출 예측 모델 서비스입니다.
 * <p>
 * 두 개의 LSTM 계층을 수직으로 쌓아(Stacked) 데이터의 복합적인 상관관계를 학습하며,
 * L2 규제와 최적화된 학습률을 통해 과적합을 방지하고 예측 정밀도를 극대화합니다.
 * </p>
 */
@Getter
@Slf4j
public class LstmPredictorV2 {

    /** 실제 딥러닝 연산을 수행하는 멀티레이어 신경망 모델 */
    private MultiLayerNetwork model;

    /** V2 규격의 학습 데이터를 로드하는 유틸리티 */
    private final LstmDataLoaderV2 dataLoader = new LstmDataLoaderV2();

    /** 데이터를 3D 텐서 형식으로 변환하는 V2 전용 컨버터 */
    private final LstmDataConverterV2 converter = new LstmDataConverterV2();

    /**
     * V2 통합 데이터셋을 활용하여 전체 학습 프로세스를 실행하고 모델을 저장합니다.
     * <p>
     * 데이터 로딩부터 텐서 변환, 모델 초기화, 50회 에포크 학습 및
     * 물리 파일(.zip) 직렬화까지의 과정을 총괄하며 모델의 완성도를 관리합니다.
     * </p>
     * @param csvPath 16개 피처가 포함된 학습용 CSV 데이터 경로
     * @param modelSavePath 학습이 완료된 모델을 저장할 경로
     * @throws Exception 데이터 로드 또는 모델 저장 중 발생하는 예외
     */
    public void runTraining(String csvPath, String modelSavePath) throws Exception {
        // 데이터 로드 및 텐서 변환
        List<LstmAdvancedRequestV2> rawData = dataLoader.loadTrainingData(csvPath);
        INDArray features = converter.toTrainingTensor(rawData);
        INDArray labels = createLabelTensor(rawData);

        // 모델 초기화 (입력 피처 10개 기준 초기화 로직 유지)
        initModel(10, 1);

        // 학습 수행 (50 Epochs 반복 학습)
        log.info("V2 Stacked LSTM 학습 시작 (총 {}건)", rawData.size());
        for (int i = 1; i <= 50; i++) {
            model.fit(features, labels);
            if (i % 10 == 0) log.info("Epoch {} 완료 - 가중치 정밀 최적화 중...", i);
        }

        // 모델 저장
        File saveFile = new File(modelSavePath);
        model.save(saveFile, true);
        log.info("V2 모델 저장 완료: {}", saveFile.getAbsolutePath());
    }

    /**
     * 고도화된 Stacked LSTM 네트워크 구조 및 하이퍼파라미터를 초기화합니다.
     * <p>
     * 두 층의 LSTM 레이어를 배치하여 깊은 특징(Deep Features)을 추출하며,
     * L2 규제(0.0001)와 낮은 학습률의 Adam 옵티마이저를 통해 학습 안정성을 확보합니다.
     * </p>
     * @param inputSize 입력 피처 차원 수
     * @param outputSize 최종 출력 노드 수 (1: 지출액 예측)
     */
    public void initModel(int inputSize, int outputSize) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .updater(new Adam(0.0005)) // 정교한 수렴을 위한 낮은 학습률 설정
                .weightInit(WeightInit.XAVIER)
                .l2(0.0001) // 과적합 방지를 위한 L2 정규화 적용
                .list()
                // 첫 번째 LSTM 계층: 고차원 특징 추출 (128 노드)
                .layer(0, new LSTM.Builder()
                        .nIn(inputSize)
                        .nOut(128)
                        .activation(Activation.TANH)
                        .build())
                // 두 번째 LSTM 계층: 특징 요약 및 시계열 압축 (64 노드)
                .layer(1, new LSTM.Builder()
                        .nIn(128)
                        .nOut(64)
                        .activation(Activation.TANH)
                        .build())
                // 출력 계층: 회귀 분석 수행 (Loss: MSE, Activation: RELU)
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.RELU) // 음수 지출 방지 및 비선형성 확보
                        .nIn(64)
                        .nOut(outputSize)
                        .build())
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();
    }

    /**
     * DTO 리스트를 바탕으로 Many-to-One 학습용 정답 텐서를 생성합니다.
     * <p>
     * [BatchSize, 1, 6] 구조에서 시계열의 종착점(Index 5)에 실제 라벨 값을 배치하여
     * 과거 6개월 흐름을 기반으로 미래를 예측하도록 구성합니다.
     * </p>
     * @param list V2 요청 데이터 리스트
     * @return ND4J INDArray 형태의 라벨 텐서
     */
    private INDArray createLabelTensor(List<LstmAdvancedRequestV2> list) {
        int batchSize = list.size();
        INDArray labels = Nd4j.zeros(batchSize, 1, 6);
        for (int i = 0; i < batchSize; i++) {
            labels.putScalar(new int[]{i, 0, 5}, list.get(i).getLabel());
        }
        return labels;
    }
}