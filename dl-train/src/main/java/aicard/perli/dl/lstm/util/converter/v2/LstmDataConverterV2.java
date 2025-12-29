package aicard.perli.dl.lstm.util.converter.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;

/**
 * 데이터 변환 유틸리티.
 * 학습용 벌크 변환 및 실시간 추론용 단일 변환을 모두 지원.
 */
public class LstmDataConverterV2 {

    /**
     * DTO 리스트를 3차원 텐서 [Batch, 16, 6]로 변환
     */
    public INDArray toTrainingTensor(List<LstmAdvancedRequestV2> dataList) {
        int batchSize = dataList.size();
        int timeSteps = 6;
        int featureSize = 16;

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        for (int i = 0; i < batchSize; i++) {
            fillTensor(tensor, i, dataList.get(i), timeSteps);
        }
        return tensor;
    }

    /**
     * 단일 사용자 데이터를 모델 입력용 텐서 [1, 16, 6]로 변환
     */
    public INDArray toLstmInferenceInput(LstmAdvancedRequestV2 userData) {
        int batchSize = 1; // 단일 사용자
        int timeSteps = 6;
        int featureSize = 16;

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        // 0번 인덱스에 데이터 주입
        fillTensor(tensor, 0, userData, timeSteps);

        return tensor;
    }

    /**
     * 텐서에 데이터를 채우는 공통 로직 (학습/추론 공용)
     */
    private void fillTensor(INDArray tensor, int batchIdx, LstmAdvancedRequestV2 d, int timeSteps) {
        for (int t = 0; t < timeSteps; t++) {
            // 시계열 기본 3종
            tensor.putScalar(new int[]{batchIdx, 0, t}, d.getBillAmts()[t] / 500000.0);
            tensor.putScalar(new int[]{batchIdx, 1, t}, d.getPayAmts()[t] / 500000.0);
            tensor.putScalar(new int[]{batchIdx, 2, t}, (d.getPayStatus()[t] + 2) / 10.0);

            // 인구통계 4종
            tensor.putScalar(new int[]{batchIdx, 3, t}, (double)(d.getSex() - 1));
            tensor.putScalar(new int[]{batchIdx, 4, t}, (double)d.getEducation() / 6.0);
            tensor.putScalar(new int[]{batchIdx, 5, t}, (double)d.getMarriage() / 3.0);
            tensor.putScalar(new int[]{batchIdx, 6, t}, (double)d.getAge() / 100.0);

            // 파생 피처 2종 (Trend, Utilization)
            double trend = (t > 0) ? (d.getBillAmts()[t] - d.getBillAmts()[t - 1]) / 100000.0 : 0;
            tensor.putScalar(new int[]{batchIdx, 7, t}, trend);
            tensor.putScalar(new int[]{batchIdx, 8, t}, d.getBillAmts()[t] / (d.getLimitBal() + 1.0));

            // 확장 피처(살) 7종
            tensor.putScalar(new int[]{batchIdx, 9, t}, d.getLimitBal() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 10, t}, d.getTotalAmount() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 11, t}, (double) d.getTxCount() / 1000.0);
            tensor.putScalar(new int[]{batchIdx, 12, t}, d.getAvgInstallments() / 10.0);
            tensor.putScalar(new int[]{batchIdx, 13, t}, d.getAuthorizedRatio());
            tensor.putScalar(new int[]{batchIdx, 14, t}, d.getBalance() / 1000000.0);
            tensor.putScalar(new int[]{batchIdx, 15, t}, d.getPurchases() / 1000000.0);
        }
    }
}