package aicard.perli.dl.lstm.dto.request.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 통합 데이터셋(UCI + Advanced + CC + Demographics) 매핑 DTO V2.
 * <p>
 * 기존의 시계열 금융 흐름과 통계 지표에 사용자의 인구통계학적 특성(성별, 학력, 결혼 여부, 연령)을 결합하였습니다.
 * 이를 통해 모델은 사용자의 생애 주기 및 사회적 지위에 따른 소비 패턴의 편차를 정밀하게 학습할 수 있습니다.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LstmAdvancedRequestV2 {

    /** 레코드 식별자 */
    private String id;

    /** 사용자의 카드 이용 한도 (소비 여력 및 신용 규모를 판단하는 핵심 지표) */
    private double limitBal;

    /** 인구통계학적 피처 (Demographics):
     * 소비 주체의 사회적 배경을 나타내는 정적 데이터군입니다.
     */

    /** 성별 (1: 남성, 2: 여성) */
    private int sex;

    /** 교육 수준 (1: 대학원, 2: 대졸, 3: 고졸, 4: 기타) */
    private int education;

    /** 결혼 상태 (1: 기혼, 2: 미혼, 3: 기타) */
    private int marriage;

    /** 연령 (사용자의 소비 생애 주기를 결정하는 핵심 변수) */
    private int age;

    /** 시계열 데이터 (Time-Series):
     * 최근 6개월간의 금융 상태 및 금액의 동적 변화를 추적합니다.
     */

    /** BILL_AMT1 ~ 6: 매월 청구 금액 리스트 (6개월 시퀀스) */
    private double[] billAmts;

    /** PAY_AMT1 ~ 6: 매월 실제 결제 금액 리스트 (6개월 시퀀스) */
    private double[] payAmts;

    /** PAY_0 ~ 6: 매월 결제 이행 상태 흐름 (연체 및 상환 패턴 시퀀스) */
    private int[] payStatus;

    /** 확장 분석 피처:
     * 데이터 융합 공정을 통해 산출된 심층 소비 분석 지표군입니다.
     */

    /** 분석 기간 내 총 지출액 합계 (소비 규모) */
    private double totalAmount;

    /** 총 거래 발생 횟수 (소비 빈도 및 활동성) */
    private int txCount;

    /** 평균 할부 개월 수 (자금 유동성 관리 성향 측정 지표) */
    private double avgInstallments;

    /** 승인 성공률 (사용자의 금융 신뢰도 및 패턴 안정성 측정) */
    private double authorizedRatio;

    /** 현재 계좌 잔액 상태 (지불 잠재력 지표) */
    private double balance;

    /** 총 구매 확정 금액 (실질적 소비 결과값) */
    private double purchases;

    /** 학습용 정답지 (Label):
     * <p>
     * 모델이 총 16개의 피처를 복합적으로 분석하여 예측해야 하는 최종 타겟값입니다.
     * </p>
     */
    private int label;
}