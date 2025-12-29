package aicard.perli.dl.optimization.domain.unfix.v1;

import aicard.perli.dl.optimization.domain.fix.v1.CreditCardV1;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지출 항목에 어떤 카드를 사용할지 결정하는 변동 엔티티
 */
@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardAssignmentV1 {

    private String spendingId;      // 지출 항목 ID
    private double spendingAmount;  // 예측된 지출 금액

    /**
     * Timefold 엔진이 최적의 카드를 찾아 할당하는 변수
     */
    @PlanningVariable(valueRangeProviderRefs = "cardRange")
    private CreditCardV1 creditCard;
}