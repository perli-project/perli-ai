package aicard.perli.dl.lstm.dto.request.v1;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 통합 데이터셋(UCI + Advanced + CC) 매핑 DTO V1.
 * <p>
 * 시계열 흐름 데이터(6개월 청구/결제 내역)와 통계적 소비성향 확장 피처를 동시에 보유하여,
 * LSTM 모델이 단순 지출액 예측을 넘어 다차원적인 소비 문맥을 학습할 수 있도록 설계되었습니다.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LstmAdvancedRequestV1 {

    /** 레코드 식별자 */
    private String id;

    /** 사용자의 카드 이용 한도 (소비 여력을 판단하는 기준 피처) */
    private double limitBal;

    /**
     * 시계열 데이터: 최근 6개월간의 금융 상태 및 금액 흐름.
     * <p>
     * LSTM의 Time-Step(6)으로 변환되어 과거로부터 현재까지의 소비 패턴 변화를 학습하는 핵심 입력값이 됩니다.
     * </p>
     */

    /** BILL_AMT1 ~ 6: 매월 청구된 금액 리스트 (시퀀셜 데이터) */
    private double[] billAmts;

    /** PAY_AMT1 ~ 6: 매월 실제 결제한 금액 리스트 (시퀀셜 데이터) */
    private double[] payAmts;

    /** PAY_0 ~ 6: 매월 결제 이행 상태 (연체 여부 등 시퀀셜 데이터) */
    private int[] payStatus;

    /** 분석 기간 내 총 지출액 합계 (통계 피처) */
    private double totalAmount;

    /** 분석 기간 내 총 거래 횟수 (활동성 지표) */
    private int txCount;

    /** 평균 할부 이용 개월수 (소비의 탄력성 측정 지표) */
    private double avgInstallments;

    /** 승인 요청 대비 승인 완료 비율 (금융 신뢰도 및 패턴 안정성 지표) */
    private double authorizedRatio;

    /** 사용자의 현재 계좌 잔액 (지불 능력 지표) */
    private double balance;

    /** 총 구매 확정 금액 (실질 소비 규모 지표) */
    private double purchases;

    /** 학습용 정답지 (Label).
     * <p>
     * 모델이 과거 6개월간의 데이터를 학습한 후 최종적으로 도출해야 하는 타겟 지출 예측값입니다.
     * </p>
     */
    private int label;
}