package aicard.perli.dl.lstm.util.converter.v2;

import aicard.perli.dl.lstm.dto.request.advanced.v2.LstmAdvancedRequestV2;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;

/**
 * 고도화된 데이터 변환 유틸리티.
 * 16개의 피처(시계열 + 인구통계 + 파생 지표)를 3차원 텐서로 변환하며 정교한 스케일링을 수행함.
 */
public class LstmDataConverterV2 {

    /**
     * V2 DTO 리스트를 3차원 텐서 [Batch, Feature(16), Time(6)]로 변환.
     * @param dataList V2 통합 데이터셋 리스트
     * @return ND4J 3차원 배열
     */
    public INDArray toTrainingTensor(List<LstmAdvancedRequestV2> dataList) {
        int batchSize = dataList.size();
        int timeSteps = 6;     // 6개월 시계열
        int featureSize = 16;  // 피처 대폭 확장 (10 -> 16)

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        for (int i = 0; i < batchSize; i++) {
            LstmAdvancedRequestV2 d = dataList.get(i);

            for (int t = 0; t < timeSteps; t++) {
                // 스케일링 정교화
                tensor.putScalar(new int[]{i, 0, t}, d.getBillAmts()[t] / 500000.0);
                tensor.putScalar(new int[]{i, 1, t}, d.getPayAmts()[t] / 500000.0);
                tensor.putScalar(new int[]{i, 2, t}, (d.getPayStatus()[t] + 2) / 10.0); // -2~8 범위를 0~1로

                // 인구통계 4종
                tensor.putScalar(new int[]{i, 3, t}, (double)(d.getSex() - 1));         // 0:남, 1:여
                tensor.putScalar(new int[]{i, 4, t}, (double)d.getEducation() / 6.0);  // 학력 정규화
                tensor.putScalar(new int[]{i, 5, t}, (double)d.getMarriage() / 3.0);   // 결혼 정규화
                tensor.putScalar(new int[]{i, 6, t}, (double)d.getAge() / 100.0);      // 나이 정규화 (0.24, 0.35 등)

                // 파생 피처 2종
                // 지출 변화율 (Spend Trend)
                double trend = 0;
                if (t > 0) trend = (d.getBillAmts()[t] - d.getBillAmts()[t - 1]) / 100000.0;
                tensor.putScalar(new int[]{i, 7, t}, trend);

                // 한도 대비 사용률 (Utilization)
                double limitUsage = d.getBillAmts()[t] / (d.getLimitBal() + 1.0);
                tensor.putScalar(new int[]{i, 8, t}, limitUsage);

                // 확장 피처(살)
                tensor.putScalar(new int[]{i, 9, t}, d.getLimitBal() / 1000000.0);
                tensor.putScalar(new int[]{i, 10, t}, d.getTotalAmount() / 1000000.0);
                tensor.putScalar(new int[]{i, 11, t}, (double) d.getTxCount() / 1000.0);
                tensor.putScalar(new int[]{i, 12, t}, d.getAvgInstallments() / 10.0);
                tensor.putScalar(new int[]{i, 13, t}, d.getAuthorizedRatio());
                tensor.putScalar(new int[]{i, 14, t}, d.getBalance() / 1000000.0);
                tensor.putScalar(new int[]{i, 15, t}, d.getPurchases() / 1000000.0);
            }
        }
        return tensor;
    }
}