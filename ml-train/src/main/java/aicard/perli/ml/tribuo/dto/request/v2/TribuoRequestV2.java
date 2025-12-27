package aicard.perli.ml.tribuo.dto.request.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고도화 데이터 수혈 모델용 카드 요청 DTO
 * 최근 행동 데이터(건수, 금액, 프리미엄 비중) 필드가 추가.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TribuoRequestV2 {
    private String cardId;
    private double totalAmount; // 총 소비액
    private int txCount; // 거래 횟수
    private double authorizedRatio; // 승인율
    private double avgAmount; // 평균 결제액

    // 수혈 피처
    private double newTxCount;   // 최근 1개월 거래 건수
    private double newTotalAmt;  // 최근 1개월 거래 금액
    private double premiumRatio; // 프리미엄 가맹점 결제 비중
}