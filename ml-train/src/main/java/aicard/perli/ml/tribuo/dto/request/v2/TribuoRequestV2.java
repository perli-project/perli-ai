package aicard.perli.ml.tribuo.dto.request.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고도화 데이터 수혈 모델용 카드 추천 요청 DTO 클래스입니다.
 * <p>
 * 기존의 누적 통계 지표에 더해, 최근 행동 데이터(건수, 금액, 프리미엄 비중) 필드가 추가되어
 * 유저의 최신 소비 트렌드와 프리미엄 지향성을 모델에 주입합니다.
 * </p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TribuoRequestV2 {

    /** 추천 대상 카드 식별 ID */
    private String cardId;

    /** 과거 누적 총 소비 금액 (사용자의 장기적인 소비 규모) */
    private double totalAmount;

    /** 과거 누적 총 거래 발생 횟수 (장기 활동성 지표) */
    private int txCount;

    /** 카드 승인 성공 비율 (금융 거래의 안정성 지표) */
    private double authorizedRatio;

    /** 과거 거래당 평균 결제 금액 (기본 소비 단가) */
    private double avgAmount;

    /** 최근 1개월간 발생한 거래 건수 (사용자의 현재 활성화 수준 측정) */
    private double newTxCount;

    /** 최근 1개월간 발생한 총 거래 금액 (최근 지출 여력 및 모멘텀 파악) */
    private double newTotalAmt;

    /** 전체 거래 중 프리미엄 가맹점 결제가 차지하는 비중입니다.
     * <p>
     * 사용자의 소비 품격과 고부가가치 카드 상품에 대한 반응도를 예측하는 핵심 변수입니다.
     * </p>
     */
    private double premiumRatio;
}