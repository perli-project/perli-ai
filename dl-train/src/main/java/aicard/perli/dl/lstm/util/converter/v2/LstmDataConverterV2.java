package aicard.perli.dl.lstm.util.converter.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;

/**
 * 고도화된 V2 데이터를 LSTM 신경망 입력 규격에 맞게 가공하는 전처리 유틸리티 클래스입니다.
 * <p>
 * 금융 시퀀스, 인구통계 정보, 그리고 실시간 파생 피처(추세 및 이용률)를 결합하여
 * 16차원의 입력 벡터를 생성하며, 학습과 추론 단계에서 동일한 정규화(Normalization)를 수행합니다.
 * </p>
 */
public class LstmDataConverterV2 {

    /**
     * 다수의 사용자 데이터 리스트를 학습용 3차원 텐서로 일괄 변환합니다.
     * <p>
     * ND4J를 활용하여 [Batch, Features, TimeSteps] 구조의 행렬을 생성하며,
     * 신경망이 학습하기 최적인 0~1 사이의 값으로 모든 피처를 스케일링합니다.
     * </p>
     * @param dataList 16개 이상의 정보를 포함한 V2 데이터셋 리스트
     * @return ND4J 3차원 배열 [BatchSize, 16, 6]
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
     * 단일 사용자의 실시간 데이터를 추론용 텐서 포맷으로 변환합니다.
     * <p>
     * 실시간 지출 예측 서비스(Inference)를 수행하기 위해 배치 크기를 1로 고정하며,
     * 학습 시와 동일한 수치 보정 로직을 적용하여 추론의 일관성을 유지합니다.
     * </p>
     * @param userData 추론 대상이 되는 사용자의 고도화 데이터
     * @return ND4J 3차원 배열 [1, 16, 6]
     */
    public INDArray toLstmInferenceInput(LstmAdvancedRequestV2 userData) {
        int batchSize = 1;
        int timeSteps = 6;
        int featureSize = 16;

        INDArray tensor = Nd4j.create(new int[]{batchSize, featureSize, timeSteps});

        // 0번 배치 인덱스에 단일 사용자 데이터를 매핑
        fillTensor(tensor, 0, userData, timeSteps);

        return tensor;
    }

    /**
     * 입력 데이터를 텐서의 특정 위치에 매핑하고 정규화 및 파생 피처 생성을 수행합니다.
     * <p>
     * 시계열 흐름뿐만 아니라 전월 대비 지출 변화(Trend)와 한도 대비 이용률(Utilization)을
     * 동적으로 산출하여 모델의 예측력을 높입니다.
     * </p>
     * @param tensor 데이터를 주입할 대상 텐서 객체
     * @param batchIdx 대상 사용자의 배치 내 위치
     * @param d 원천 데이터 DTO
     * @param timeSteps 분석 대상 시계열 구간 (6개월)
     */
    private void fillTensor(INDArray tensor, int batchIdx, LstmAdvancedRequestV2 d, int timeSteps) {
        for (int t = 0; t < timeSteps; t++) {
            // 시계열 기본 3종: 청구/결제 금액 스케일링 및 상태값 보정
            tensor.putScalar(new int[]{batchIdx, 0, t}, d.getBillAmts()[t] / 500000.0);
            tensor.putScalar(new int[]{batchIdx, 1, t}, d.getPayAmts()[t] / 500000.0);
            tensor.putScalar(new int[]{batchIdx, 2, t}, (d.getPayStatus()[t] + 2) / 10.0);

            // 인구통계 4종: 범주형 데이터를 수치형 정규화 데이터로 변환
            tensor.putScalar(new int[]{batchIdx, 3, t}, (double)(d.getSex() - 1));
            tensor.putScalar(new int[]{batchIdx, 4, t}, (double)d.getEducation() / 6.0);
            tensor.putScalar(new int[]{batchIdx, 5, t}, (double)d.getMarriage() / 3.0);
            tensor.putScalar(new int[]{batchIdx, 6, t}, (double)d.getAge() / 100.0);

            // 파생 피처 2종: 지출 추세(Trend)와 카드 한도 소진율(Utilization) 동적 산출
            double trend = (t > 0) ? (d.getBillAmts()[t] - d.getBillAmts()[t - 1]) / 100000.0 : 0;
            tensor.putScalar(new int[]{batchIdx, 7, t}, trend);
            tensor.putScalar(new int[]{batchIdx, 8, t}, d.getBillAmts()[t] / (d.getLimitBal() + 1.0));

            // 확장 분석 지표 7종: 총액, 건수, 잔액 등 통계적 살(Features) 주입
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