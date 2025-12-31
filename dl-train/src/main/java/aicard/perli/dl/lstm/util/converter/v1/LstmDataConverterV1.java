package aicard.perli.dl.lstm.util.converter.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;

/**
 * DeepLearning4J LSTM 모델 V1 입력용 데이터 변환 유틸리티 클래스입니다.
 * <p>
 * 10개의 피처(시계열 3종 + 확장 분석 지표 7종)를 3차원 텐서 포맷으로 변환하며,
 * 학습(Bulk)과 추론(Inference) 단계에서 일관된 데이터 스케일링을 보장합니다.
 * </p>
 */
public class LstmDataConverterV1 {

    /**
     * 통합 데이터 리스트를 학습용 3차원 텐서로 변환합니다.
     * <p>
     * ND4J 라이브러리를 사용하여 [Batch, Features, TimeSteps] 구조의 배열을 생성하며,
     * 대용량 학습 데이터를 일괄 처리하는 데 최적화되어 있습니다.
     * </p>
     * @param dataList 10개의 피처를 포함한 V1 통합 데이터셋 리스트
     * @return ND4J 3차원 배열 [BatchSize, 10, 6]
     */
    public INDArray toTrainingTensor(List<LstmAdvancedRequestV1> dataList) {
        int batchSize = dataList.size();
        int timeSteps = 6;
        int featureSize = 10;

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        for (int i = 0; i < batchSize; i++) {
            fillTensorWithV1Data(tensor, i, dataList.get(i), timeSteps);
        }
        return tensor;
    }

    /**
     * 단일 사용자의 데이터를 모델 추론용 텐서로 변환합니다.
     * <p>
     * 실시간 예측 서비스(Inference)를 위해 배치 사이즈를 1로 고정하여
     * 모델 입력 규격에 맞는 3D 텐서를 생성합니다.
     * </p>
     * @param userData 실시간 예측 대상인 단일 사용자 데이터
     * @return ND4J 3차원 배열 [1, 10, 6]
     */
    public INDArray toLstmInput(LstmAdvancedRequestV1 userData) {
        int featureSize = 10;
        int timeSteps = 6;

        INDArray tensor = Nd4j.create(new int[]{1, featureSize, timeSteps});

        // 0번 배치 인덱스에 단일 사용자 데이터를 매핑
        fillTensorWithV1Data(tensor, 0, userData, timeSteps);

        return tensor;
    }

    /**
     * 데이터 텐서 매핑 및 수치 정규화(Scaling)를 수행하는 내부 공통 로직입니다.
     * <p>
     * 학습 안정성을 위해 청구/결제 금액은 1,000,000으로 나누어 스케일링하며,
     * 상태 코드 및 거래 횟수 등도 각 특성에 맞는 분모를 적용하여 모델의 가중치 수렴을 돕습니다.
     * </p>
     * @param tensor 데이터를 주입할 타겟 ND4J 텐서 객체
     * @param batchIdx 데이터를 주입할 배치 내 행 인덱스
     * @param d 원본 데이터 DTO
     * @param timeSteps 분석 대상 시계열 길이 (6개월)
     */
    private void fillTensorWithV1Data(INDArray tensor, int batchIdx, LstmAdvancedRequestV1 d, int timeSteps) {
        for (int t = 0; t < timeSteps; t++) {
            // 시계열 데이터 매핑 및 Scaling: 금액 단위의 편차를 줄이기 위해 정규화 수행
            tensor.putScalar(new int[]{batchIdx, 0, t}, d.getBillAmts()[t] / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 1, t}, d.getPayAmts()[t] / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 2, t}, (double) d.getPayStatus()[t] / 10.0);

            // 확장 피처 매핑: 정적 정보와 통계 지표를 각 타임스텝에 배경 정보로 주입
            tensor.putScalar(new int[]{batchIdx, 3, t}, d.getLimitBal() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 4, t}, d.getTotalAmount() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 5, t}, (double) d.getTxCount() / 1000.0);
            tensor.putScalar(new int[]{batchIdx, 6, t}, d.getAvgInstallments() / 10.0);
            tensor.putScalar(new int[]{batchIdx, 7, t}, d.getAuthorizedRatio());
            tensor.putScalar(new int[]{batchIdx, 8, t}, d.getBalance() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 9, t}, d.getPurchases() / 1000000.0);
        }
    }
}