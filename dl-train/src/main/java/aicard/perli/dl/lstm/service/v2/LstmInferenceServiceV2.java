package aicard.perli.dl.lstm.service.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import aicard.perli.dl.lstm.util.converter.v2.LstmDataConverterV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;

/**
 * 학습된 모델을 사용하여 지출을 예측하는 서비스
 * */
@Slf4j
public class LstmInferenceServiceV2 {

    private MultiLayerNetwork model;
    private final LstmDataConverterV2 converter = new LstmDataConverterV2();

    /**
     * 저장된 모델 로드
     * @param modelPath 모델 파일 경로 (.zip)
     * */
    public void loadModel(String modelPath) throws IOException {
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new IOException("모델 파일을 찾을 수 없습니다: " + modelPath);
        }
        this.model = MultiLayerNetwork.load(file, true);
        log.info("LSTM 모델 로드 완료: {}", modelPath);
    }

    /**
     * 특정 사용자의 지출액 예측
     * @param userData 16개 피처가 포함된 사용자 데이터
     * @return 예측 지출 금액 (스케일 복원 전 값)
     */
    public double predictExpense(LstmAdvancedRequestV2 userData) {
        if (model == null) {
            throw new IllegalStateException("모델이 로드되지 않았습니다.");
        }

        // 입력을 텐서로 변환 [1, 16, 6]
        INDArray inputTensor = converter.toLstmInferenceInput(userData);

        // 모델 추론 실행
        INDArray output = model.output(inputTensor);

        // 마지막 시점(t=5)의 결과값 추출
        // 결과 텐서 구조: [Batch(1), Output(1), TimeStep(6)]
        return output.getDouble(0, 0, 5);
    }
}
