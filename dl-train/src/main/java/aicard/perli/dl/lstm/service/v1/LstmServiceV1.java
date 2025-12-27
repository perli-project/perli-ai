package aicard.perli.dl.lstm.service.v1;

import aicard.perli.dl.lstm.dto.request.advanced.v1.LstmAdvancedRequestV1;
import aicard.perli.dl.lstm.util.converter.v1.LstmDataConverterV1;
import lombok.RequiredArgsConstructor;
import org.nd4j.linalg.api.ndarray.INDArray;
import java.util.List;

/**
 * 10개 피처를 사용하는 LSTM 기반 지출 예측 서비스.
 */
@RequiredArgsConstructor
public class LstmServiceV1 {

    private final LstmPredictorV1 predictorV1;
    private final LstmDataConverterV1 converterV1;

    /**
     * 통합 데이터를 기반으로 학습을 수행하고 예측 확률을 반환함.
     *
     * @param trainingData 통합 DTO 리스트 (10개 피처 포함)
     * @return 예측 결과 (연체/지출위험 확률)
     */
    public double predictFutureSpending(List<LstmAdvancedRequestV1> trainingData) {
        // 모델 초기화 (피처 10개로 고정)
        predictorV1.initModel(10, 1);

        // 통합 텐서 변환 (toTrainingTensor 메서드 사용)
        INDArray inputTensor = converterV1.toTrainingTensor(trainingData);

        // 모델 학습 (정답지 생략 시 자기 자신으로 fit하거나 별도 정답지 필요)
        predictorV1.getModel().fit(inputTensor, inputTensor);

        // 추론 (Inference)
        INDArray output = predictorV1.getModel().rnnTimeStep(inputTensor);

        // 최종 시점의 예측값 반환
        return output.getDouble(output.length() - 1);
    }
}