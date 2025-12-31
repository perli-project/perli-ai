package aicard.perli.dl.optimization.domain.unfix.v1;

import aicard.perli.dl.optimization.domain.fix.v1.CreditCardV1;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 특정 지출 항목에 최적의 카드를 배정하기 위한 변동(Planning Entity) 클래스입니다.
 * <p>
 * Timefold 최적화 엔진은 이 클래스의 인스턴스들을 모아 다양한 카드 조합을 시뮬레이션하며,
 * 각 지출 금액에 어떤 카드를 매칭했을 때 유저의 혜택이 극대화되는지 계산합니다.
 * </p>
 */
@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardAssignmentV1 {

    /** 개별 지출 내역을 식별하기 위한 고유 ID */
    private String spendingId;

    /** AI 모델(LSTM)에 의해 예측된 차월 지출 예정 금액입니다.
     * 이 금액을 어떤 카드에 배정하느냐에 따라 해당 카드의 실적 달성 여부가 결정됩니다.
     */
    private double spendingAmount;

    /** 최적화 엔진이 탐색을 통해 결정하는 가변적 카드 할당 필드입니다.
     * <p>
     * PlanningVariable 설정을 통해 'cardRange'라는 후보군 내에서 엔진이 자유롭게
     * 카드를 교체하며, 전체 혜택 점수(Score)를 높이는 최적의 대상을 확정합니다.
     * </p>
     */
    @PlanningVariable(valueRangeProviderRefs = "cardRange")
    private CreditCardV1 creditCard;
}