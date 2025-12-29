package aicard.perli.dl.lstm.dto.request.advanced.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 통합 데이터셋(UCI + Advanced + CC + Demographics) 매핑 DTO.
 * 기존 시계열 및 통계 데이터에 인구통계학적 특성을 추가함.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LstmAdvancedRequestV2 {

    private String id;       // 레코드 식별자
    private double limitBal; // 카드 한도

    /** 인구통계학적 피처 */
    private int sex;         // 성별 (1:남성, 2:여성)
    private int education;   // 교육 수준 (1:대학원, 2:대졸, 3:고졸, 4:기타)
    private int marriage;    // 결혼 상태 (1:기혼, 2:미혼, 3:기타)
    private int age;         // 나이

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

    private int label; // 정답지 (연체 여부 등 예측 목표)
}