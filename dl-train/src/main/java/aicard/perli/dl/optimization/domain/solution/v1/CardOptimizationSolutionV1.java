package aicard.perli.dl.optimization.domain.solution.v1;

import aicard.perli.dl.optimization.domain.fix.v1.CreditCardV1;
import aicard.perli.dl.optimization.domain.unfix.v1.CardAssignmentV1;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 혜택 최적화 결과 성적표 (Planning Solution)
 * 고정 데이터(카드 목록)와 변동 데이터(지출 할당)를 통합하여 관리합니다.
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 생성 (score 포함)
public class CardOptimizationSolutionV1 {

    /**
     * 엔진이 선택 가능한 카드 후보 리스트
     */
    @ValueRangeProvider(id = "cardRange")
    private List<CreditCardV1> cardList;

    /**
     * 지출 항목별 카드 할당 현황 리스트
     */
    @PlanningEntityCollectionProperty
    private List<CardAssignmentV1> assignmentList;

    /**
     * 계산된 최종 점수 (Hard: 제약 준수 여부, Soft: 혜택 총합)
     */
    @PlanningScore
    private HardSoftScore score;
}