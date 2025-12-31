package aicard.perli.dl.optimization.domain.fix.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 카테고리별 차등 혜택과 통합 할인 한도를 포함하는 고도화된 카드 정보 클래스입니다.
 * <p>
 * V2 최적화 모델의 핵심 Fact 데이터로 사용되며, 단순 실적 달성 여부뿐만 아니라
 * 개별 업종별 혜택 시뮬레이션 및 월간 총 혜택 한도 초과 여부를 계산하는 기준이 됩니다.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardV2 {

    /** 카드를 식별하기 위한 고유 ID */
    private String cardId;

    /** 카드의 공식 명칭 (예: 신한 Mr.Life, 삼성 iD ON) */
    private String cardName;

    /** 혜택 수령을 위해 반드시 충족해야 하는 전월 실적 목표 금액 */
    private double performanceTarget;

    /** 업종별(FOOD, MART, ONLINE 등) 차등 혜택 비율을 담은 맵 구조입니다.
     * 키값은 업종 코드를 의미하며, 밸류값은 해당 업종 결제 시 적용되는 할인율입니다.
     */
    private Map<String, Double> ccategoryBenefitRates = new HashMap<>();

    /** 해당 카드로 이번 달에 이미 결제하여 실적으로 잡힌 누적 금액입니다.
     * 엔진은 이 값과 새로 배정될 금액을 합산하여 최종 실적 달성 여부를 판단합니다.
     */
    private double currentPerformance;

    /** 해당 카드가 한 달 동안 제공할 수 있는 최대 할인/적립 금액의 총합입니다.
     * 카테고리별 혜택의 합이 이 한도를 초과할 경우, 엔진은 한도까지만 점수를 부여합니다.
     */
    private double maxBenefitLimit;
}