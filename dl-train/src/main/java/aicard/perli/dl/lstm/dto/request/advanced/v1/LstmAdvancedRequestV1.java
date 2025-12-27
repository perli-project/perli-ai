package aicard.perli.dl.lstm.dto.request.advanced.v1;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 통합 데이터셋(UCI + Advanced + CC) 매핑 DTO.
 * 시계열 흐름 데이터와 통계/소비성향 확장 피처를 동시에 보유함.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LstmAdvancedRequestV1 {

    private String id;       // 레코드 식별자
    private double limitBal; // 카드 한도

    /** 시계열 데이터: 최근 6개월간의 상태 및 금액 흐름 */
    private double[] billAmts;   // BILL_AMT1 ~ 6 (청구 금액)
    private double[] payAmts;    // PAY_AMT1 ~ 6 (결제 금액)
    private int[] payStatus;     // PAY_0 ~ 6 (결제 상태)

    /** 확장 피처: 데이터 융합을 통해 추가된 분석 지표 (살) */
    private double totalAmount;      // 총 지출액
    private int txCount;             // 거래 횟수
    private double avgInstallments;  // 평균 할부 개월
    private double authorizedRatio;  // 승인율
    private double balance;          // 계좌 잔액
    private double purchases;        // 총 구매액

    private int label; // 정답지
}