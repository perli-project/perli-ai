package aicard.perli.dl.optimization.score.v2;

import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;

/**
 * 카드 혜택 최적화를 위한 V2 채점 규칙 클래스.
 * AI가 예측한 지출 카테고리와 실제 카드의 혜택 한도(Cap), 실적 목표(Target)를 고려하여
 * 점수를 계산합니다.
 */
public class CardScoreV2 implements ConstraintProvider {

    /**
     * 엔진에 적용될 제약 조건 리스트를 정의합니다.
     * @param factory 제약 조건 생성 팩토리
     * @return 정의된 제약 조건 배열
     */
    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                penalizeRestrictedCategoryUsage(factory),
                penalizeMinimumSpendShortfall(factory),
                penalizePerformanceShortfall(factory),
                maximizeBenefitWithinLimit(factory),
                reachPerformanceTarget(factory)
        };
    }

    /**
     * 카드 정책상 혜택 제외 업종에 배정된 경우 강한 하드 패널티를 부여합니다.
     */
    private Constraint penalizeRestrictedCategoryUsage(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV2.class)
                .filter(a -> a.getCreditCard() != null)
                .filter(a -> a.getCreditCard().getRestrictedCategories() != null
                        && a.getCreditCard().getRestrictedCategories().contains(a.getCategory()))
                .penalize(HardSoftScore.ONE_HARD, a -> 100)
                .asConstraint("PenalizeRestrictedCategoryUsage");
    }

    /**
     * 카드별 최소 실적 요건(minimumSpendRequired) 미달 시 하드 패널티를 부여합니다.
     */
    private Constraint penalizeMinimumSpendShortfall(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV2.class)
                .filter(a -> a.getCreditCard() != null)
                .groupBy(CardAssignmentV2::getCreditCard,
                        ConstraintCollectors.sum(a -> (int) a.getSpendingAmount()))
                .filter((card, sumAmount) ->
                        sumAmount > 0 && (card.getCurrentPerformance() + sumAmount) < card.getMinimumSpendRequired())
                .penalize(HardSoftScore.ONE_HARD, (card, sumAmount) -> {
                    double shortfall = card.getMinimumSpendRequired() - (card.getCurrentPerformance() + sumAmount);
                    return (int) Math.ceil(shortfall / 1000.0);
                })
                .asConstraint("PenalizeMinimumSpendShortfall");
    }

    /**
     * 카드별 실적 목표 미달 금액을 하드 패널티로 반영합니다.
     * 목표를 채우지 못한 해는 우선순위에서 탈락하도록 강제합니다.
     */
    private Constraint penalizePerformanceShortfall(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV2.class)
                .filter(a -> a.getCreditCard() != null)
                .groupBy(CardAssignmentV2::getCreditCard,
                        ConstraintCollectors.sum(a -> (int) a.getSpendingAmount()))
                .penalize(HardSoftScore.ONE_HARD, (card, sumAmount) -> {
                    double shortfall = card.getPerformanceTarget() - (card.getCurrentPerformance() + sumAmount);
                    return shortfall > 0 ? (int) Math.ceil(shortfall / 1000.0) : 0;
                })
                .asConstraint("PenalizePerformanceShortfall");
    }

    /**
     * 카드별 카테고리 할인율을 적용하여 전체 혜택 금액을 최대화합니다.
     * 단, 카드의 '월 통합 할인 한도(maxBenefitLimit)'를 초과하는 혜택은 점수에 반영하지 않음으로써,
     * 한도가 남은 다른 카드로의 지출 분산을 유도합니다.
     * @param factory 제약 조건 생성 팩토리
     * @return 카드별 실질 혜택 점수 부여 제약
     */
    private Constraint maximizeBenefitWithinLimit(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV2.class)
                .filter(a -> a.getCreditCard() != null)
                .groupBy(CardAssignmentV2::getCreditCard,
                        ConstraintCollectors.sum(a -> (int) a.getSpendingAmount()),
                        ConstraintCollectors.sum(a -> {
                            if (a.getCreditCard().getRestrictedCategories() != null
                                    && a.getCreditCard().getRestrictedCategories().contains(a.getCategory())) {
                                return 0;
                            }
                            if (a.getSpendingAmount() < a.getCreditCard().getMinimumTransactionAmountForBenefit()) {
                                return 0;
                            }

                            double rate = a.getCreditCard().getCcategoryBenefitRates()
                                    .getOrDefault(a.getCategory(), 0.0);
                            return (int) (a.getSpendingAmount() * rate);
                        }))
                .reward(HardSoftScore.ONE_SOFT, (card, totalSpend, totalBenefit) -> {
                    if ((card.getCurrentPerformance() + totalSpend) < card.getMinimumSpendRequired()) {
                        return 0;
                    }
                    double multiplier = card.resolvePerformanceBandMultiplier(card.getCurrentPerformance() + totalSpend);
                    double adjustedBenefit = totalBenefit * multiplier;
                    return (int) Math.min(adjustedBenefit, card.getMaxBenefitLimit());
                })
                .asConstraint("MaximizeBenefitWithinLimit");
    }

    /**
     * 카드별 목표 실적(performanceTarget) 달성을 유도합니다.
     * '기존 실적(currentPerformance) + 새로 할당된 지출액'이 목표 실적에 도달할 경우
     * 강력한 가산점을 부여하여, 사용자가 다음 달에도 혜택을 받을 수 있도록 실적 관리를 최우선합니다.
     * @param factory 제약 조건 생성 팩토리
     * @return 목표 실적 달성 시 가산점 부여 제약
     */
    private Constraint reachPerformanceTarget(ConstraintFactory factory) {
        return factory.forEach(CardAssignmentV2.class)
                .filter(a -> a.getCreditCard() != null)
                .groupBy(CardAssignmentV2::getCreditCard,
                        ConstraintCollectors.sum(a -> (int) a.getSpendingAmount()))
                .filter((card, sumAmount) ->
                        (card.getCurrentPerformance() + sumAmount) >= card.getPerformanceTarget())
                .reward(HardSoftScore.ofSoft(5000))
                .asConstraint("ReachPerformanceTarget");
    }
}
