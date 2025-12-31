package aicard.perli.dl.optimization.domain.solution.v2;

import aicard.perli.dl.optimization.domain.fix.v2.CreditCardV2;
import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 고도화된 카드 혜택 최적화 문제의 전체 데이터를 담는 Planning Solution 클래스입니다.
 * <p>
 * 업종별 차등 할인과 통합 한도가 적용된 V2 모델의 모든 요소를 통합하며,
 * AI 예측 지출(Spending)과 보유 카드(Fact)를 결합하여 최상의 경제적 조합을 산출하는 그릇 역할을 합니다.
 * </p>
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardSolutionV2 {

    /** 최적화 엔진이 카드를 배정해야 하는 개별 지출 항목(Spending) 리스트입니다.
     * <p>
     * PlanningEntityCollectionProperty 설정을 통해 엔진은 이 리스트의 각 항목에
     * 가장 적합한 카드를 끼워 넣어 보며 실시간으로 점수를 계산합니다.
     * </p>
     */
    @PlanningEntityCollectionProperty
    private List<CardAssignmentV2> spendingList;

    /** 엔진이 지출 항목에 할당할 수 있는 카드 자산 리스트입니다.
     * <p>
     * ProblemFactCollectionProperty는 이 데이터가 연산 중 변하지 않는 고정 정보임을 명시하며,
     * ValueRangeProvider는 지출 엔티티가 카드를 선택할 수 있는 '선택지 범위'임을 선언합니다.
     * </p>
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "cardRange")
    private List<CreditCardV2> cardList;

    /** 최적화 알고리즘에 의해 도출된 배정 시나리오의 수리적 평가 지표입니다.
     * <p>
     * Hard 점수는 실적 미달 등 필수 제약 위반 여부를 감지하고,
     * Soft 점수는 통합 한도 내에서 극대화된 총 혜택 금액을 나타냅니다.
     * </p>
     */
    @PlanningScore
    private HardSoftScore score;

    /**
     * 초기 최적화 문제를 생성하기 위한 생성자입니다.
     * @param spendingList AI로부터 예측된 차월 지출 항목 리스트
     * @param cardList 유저가 보유한 카드 및 혜택 상세 정보 리스트
     */
    public CardSolutionV2(List<CardAssignmentV2> spendingList, List<CreditCardV2> cardList) {
        this.spendingList = spendingList;
        this.cardList = cardList;
    }
}