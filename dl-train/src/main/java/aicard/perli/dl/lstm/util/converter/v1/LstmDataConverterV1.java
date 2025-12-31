package aicard.perli.dl.lstm.util.converter.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;

/**
 * DL4J LSTM 모델 V1 입력용 데이터 변환 유틸리티.
 * 학습(Bulk) 및 서비스(Inference) LstmAdvancedRequestV1(10 Features)을 사용함.
 */
public class LstmDataConverterV1 {

    /**
     * 통합 데이터 리스트를 3차원 텐서로 변환 (Batch용)
     * @param dataList V1 통합 데이터셋 리스트
     * @return ND4J 3차원 배열 [Batch, 10, 6]
     */
    public INDArray toTrainingTensor(List<LstmAdvancedRequestV1> dataList) {
        int batchSize = dataList.size();
        int timeSteps = 6;
        int featureSize = 10; // 시계열 3종 + 확장 피처 7종

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        for (int i = 0; i < batchSize; i++) {
            fillTensorWithV1Data(tensor, i, dataList.get(i), timeSteps);
        }
        return tensor;
    }

    /**
     * 단일 사용자의 데이터를 모델 입력용 텐서로 변환 (Inference용)
     * @param userData 고도화된 V1 데이터 (10개 피처 포함)
     * @return ND4J 3차원 배열 [1, 10, 6]
     */
    public INDArray toLstmInput(LstmAdvancedRequestV1 userData) {
        int featureSize = 10;
        int timeSteps = 6;

        // 배치 사이즈가 1인 단일 추론용 텐서 생성
        INDArray tensor = Nd4j.create(new int[]{1, featureSize, timeSteps});

        // 0번 인덱스에 데이터 채움
        fillTensorWithV1Data(tensor, 0, userData, timeSteps);

        return tensor;
    }

    /**
     * V1 데이터 매핑 공통 로직
     * 학습과 추론에서 동일한 스케일링(나누기 100만 등)을 보장함
     */
    private void fillTensorWithV1Data(INDArray tensor, int batchIdx, LstmAdvancedRequestV1 d, int timeSteps) {
        for (int t = 0; t < timeSteps; t++) {
            // 시계열 데이터 매핑 및 Scaling (V1 기준)
            tensor.putScalar(new int[]{batchIdx, 0, t}, d.getBillAmts()[t] / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 1, t}, d.getPayAmts()[t] / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 2, t}, (double) d.getPayStatus()[t] / 10.0);

            // 확장 피처(살) 매핑 (V1 기준)
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