package aicard.perli.dl.lstm.util.converter.v1;

import aicard.perli.dl.lstm.dto.request.advanced.v1.LstmAdvancedRequestV1;
import aicard.perli.dl.lstm.dto.request.basic.v1.LstmRequestV1;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DL4J LSTM 모델 입력용 데이터 변환 유틸리티.
 * 서비스용 단일 피처 변환 및 학습용 다차원 피처 변환을 지원함.
 */
public class LstmDataConverterV1 {

    /**
     * [학습용] 통합 CSV 데이터를 3차원 텐서[Batch, Feature, Time]로 변환.
     * 시계열 데이터(6개월)와 추가 피처(4종 이상의 살)를 결합하여 정규화 수행.
     * * @param dataList 통합 데이터셋 리스트
     * @return ND4J 3차원 배열
     */
    public INDArray toTrainingTensor(List<LstmAdvancedRequestV1> dataList) {
        int batchSize = dataList.size();
        int timeSteps = 6;    // 시계열 길이 (6개월)
        int featureSize = 10; // 시계열 3종 + 확장 피처 7종

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        for (int i = 0; i < batchSize; i++) {
            LstmAdvancedRequestV1 d = dataList.get(i);

            for (int t = 0; t < timeSteps; t++) {
                // 시계열 데이터 매핑 및 Scaling
                tensor.putScalar(new int[]{i, 0, t}, d.getBillAmts()[t] / 1000000.0);
                tensor.putScalar(new int[]{i, 1, t}, d.getPayAmts()[t] / 1000000.0);
                tensor.putScalar(new int[]{i, 2, t}, (double)d.getPayStatus()[t] / 10.0);

                // 확장 피처(살) 매핑
                tensor.putScalar(new int[]{i, 3, t}, d.getLimitBal() / 1000000.0);
                tensor.putScalar(new int[]{i, 4, t}, d.getTotalAmount() / 1000000.0);
                tensor.putScalar(new int[]{i, 5, t}, (double)d.getTxCount() / 1000.0);
                tensor.putScalar(new int[]{i, 6, t}, d.getAvgInstallments() / 10.0);
                tensor.putScalar(new int[]{i, 7, t}, d.getAuthorizedRatio());
                tensor.putScalar(new int[]{i, 8, t}, d.getBalance() / 1000000.0);
                tensor.putScalar(new int[]{i, 9, t}, d.getPurchases() / 1000000.0);
            }
        }
        return tensor;
    }

    /**
     * [서비스용] 사용자별 결제 이력 리스트를 단일 피처 시계열 텐서로 변환.
     * * @param rawData 사용자별 결제 이력
     * @param timeSteps 과거 시퀀스 길이
     * @return ND4J 3차원 배열
     */
    public INDArray toLstmInput(List<LstmRequestV1> rawData, int timeSteps) {
        Map<String, List<LstmRequestV1>> userGroups = rawData.stream()
                .collect(Collectors.groupingBy(LstmRequestV1::getUserId));

        int batchSize = userGroups.size();
        int featureSize = 1;

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        int row = 0;
        for (String userId : userGroups.keySet()) {
            List<LstmRequestV1> sortedData = userGroups.get(userId).stream()
                    .sorted(Comparator.comparing(LstmRequestV1::getDate))
                    .collect(Collectors.toList());

            for (int t = 0; t < timeSteps && t < sortedData.size(); t++) {
                tensor.putScalar(new int[]{row, 0, t}, sortedData.get(t).getAmount());
            }
            row++;
        }
        return tensor;
    }
}