package aicard.perli.dl.optimization.domain.fix.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카드 정보 및 실적 구간 등 고정 데이터를 담는 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardV1 {
    private String cardId;
    private String cardName;
    private double performanceTarget; // 목표 실적 금액
    private double benefitRate;       // 혜택 비율 (예: 0.05)
}