package aicard.perli.dl.lstm.service.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import aicard.perli.dl.lstm.util.converter.v2.LstmDataConverterV2;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;

/**
 * 학습이 완료된 V2 LSTM 모델을 사용하여 실시간 지출 예측을 수행하는 서비스 클래스입니다.
 * <p>
 * 저장된 신경망 가중치를 로드하고, 사용자의 16개 다차원 피처를
 * 시계열 텐서로 변환하여 차월 예상 지출액을 산출하는 역할을 담당합니다.
 * </p>
 */
@Slf4j
public class LstmInferenceServiceV2 {

    /** 실제 추론 연산을 수행하는 DeepLearning4J 멀티레이어 신경망 모델 */
    private MultiLayerNetwork model;

    /** 실시간 입력 데이터를 신경망용 3D 텐서로 가공하는 컨버터 */
    private final LstmDataConverterV2 converter = new LstmDataConverterV2();

    /**
     * 물리적 경로에 저장된 학습 모델 파일(.zip)을 시스템 메모리에 로드합니다.
     * <p>
     * 모델 로드 시 가중치와 네트워크 구성 정보가 함께 복원되어 연산 가능한 상태로 초기화됩니다.
     * </p>
     * @param modelPath 로드할 모델 파일의 절대 또는 상대 경로
     * @throws IOException 모델 파일을 찾을 수 없거나 파일 읽기 중 오류 발생 시
     */
    public void loadModel(String modelPath) throws IOException {
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new IOException("모델 파일을 찾을 수 없습니다: " + modelPath);
        }
        this.model = MultiLayerNetwork.load(file, true);
        log.info("LSTM 모델 로드 완료: {}", modelPath);
    }

    /**
     * 특정 사용자의 다차원 데이터를 기반으로 차월 지출액을 예측합니다.
     * <p>
     * 16개의 피처를 [1, 16, 6] 형태의 텐서로 변환하여 모델에 입력하며,
     * 시계열 흐름의 가장 마지막 타임스텝에서 도출된 수치를 최종 결과로 반환합니다.
     * </p>
     * @param userData 인구통계학적 특성 및 6개월 시계열 정보가 포함된 V2 데이터
     * @return 신경망 모델이 도출한 예측값 (스케일 복원 전 수치)
     * @throws IllegalStateException 모델 파일이 정상적으로 로드되지 않은 상태에서 호출 시
     */
    public double predictExpense(LstmAdvancedRequestV2 userData) {
        if (model == null) {
            throw new IllegalStateException("모델이 로드되지 않았습니다. loadModel()을 먼저 호출하십시오.");
        }

        // 입력 데이터를 3D 텐서로 가공 [BatchSize: 1, Features: 16, TimeStep: 6]
        INDArray inputTensor = converter.toLstmInferenceInput(userData);

        // 신경망 연산(Forward Propagation) 수행
        INDArray output = model.output(inputTensor);

        // Many-to-One 구조에 따라 시퀀스의 마지막 인덱스(5) 결과값 추출
        // 결과 텐서 구조: [Batch(1), Output(1), TimeStep(6)]
        return output.getDouble(0, 0, 5);
    }
}