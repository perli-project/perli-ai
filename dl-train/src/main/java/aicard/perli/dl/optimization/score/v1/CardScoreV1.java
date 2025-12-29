package aicard.perli.dl.optimization.score.v1;

import aicard.perli.dl.optimization.domain.fix.v1.CreditCardV1;
import aicard.perli.dl.optimization.domain.unfix.v1.CardAssignmentV1;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;

/**
 * 카드 혜택 최적화를 위한 채점 규칙 정의 클래스.
 * <p>
 * Timefold 엔진이 각 지출 항목을 카드에 할당할 때
 * 점수(Hard/Soft Score)를 계산하는 기준을 제공합니다.
 * </p>
 */
public class CardScoreV1 implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                maxBenefit(factory),
                checkTarget(factory)
        };
    }

    /**
     * [Soft Score] 지출액 대비 카드별 혜택 비율을 계산하여 점수를 부여합니다.
     * 혜택 금액이 클수록 높은 점수를 획득하여 혜택 중심의 할당을 유도합니다.
     */
    private Constraint maxBenefit(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV1.class)
                .reward(HardSoftScore.ONE_SOFT, (a) ->
                        (int) (a.getSpendingAmount() * a.getCreditCard().getBenefitRate()))
                .asConstraint("MaxBenefit");
    }

    /**
     * [Soft Score] 카드별 배정 금액 총합이 목표 실적에 도달했는지 확인합니다.
     * 목표 실적 달성 시 보너스 점수(1000점)를 부여하여 효율적인 실적 채우기를 유도합니다.
     */
    private Constraint checkTarget(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV1.class)
                .groupBy(CardAssignmentV1::getCreditCard,
                        ConstraintCollectors.sum(a -> (int) a.getSpendingAmount()))
                .filter((card, sum) -> sum >= card.getPerformanceTarget())
                .reward(HardSoftScore.ofSoft(1000))
                .asConstraint("CheckTarget");
    }
}