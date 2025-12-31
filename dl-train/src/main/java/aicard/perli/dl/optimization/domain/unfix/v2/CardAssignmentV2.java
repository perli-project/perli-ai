package aicard.perli.dl.optimization.domain.unfix.v2;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import aicard.perli.dl.optimization.domain.fix.v2.CreditCardV2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 특정 업종의 지출 항목에 최적의 카드를 배정하기 위한 고도화된 변동(Planning Entity) 클래스입니다.
 * <p>
 * Timefold 최적화 엔진은 이 엔티티의 업종 정보를 바탕으로 각 카드별 차등 혜택율을 계산하며,
 * 월간 통합 한도를 초과하지 않는 범위 내에서 가장 경제적인 카드 배정 시나리오를 탐색합니다.
 * </p>
 */
@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardAssignmentV2 {

    /** 개별 지출 내역을 식별하기 위한 고유 ID */
    private String spendingId;

    /** 지출이 발생한 업종 카테고리 정보입니다. (예: FOOD, ONLINE, MART)
     * 배정된 카드의 혜택 맵(Map)에서 해당 키에 매핑된 할인율을 찾는 기준점이 됩니다.
     */
    private String category;

    /** AI 모델(LSTM)이 예측한 구체적인 지출 예정 금액입니다.
     * 이 금액에 카테고리별 할인율을 곱하여 예상 혜택 점수를 산출합니다.
     */
    private double spendingAmount;

    /** 최적화 엔진이 'cardRange' 후보군 내에서 실시간으로 교체하며 결정하는 가변 필드입니다.
     * <p>
     * 업종별 혜택과 통합 한도, 현재 실적 상태를 종합적으로 고려하여
     * 전체 솔루션 점수를 극대화할 수 있는 최적의 카드가 할당됩니다.
     * </p>
     */
    @PlanningVariable(valueRangeProviderRefs = "cardRange")
    private CreditCardV2 creditCard;
}