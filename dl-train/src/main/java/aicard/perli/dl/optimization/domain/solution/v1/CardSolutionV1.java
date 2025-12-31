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
 * 카드 혜택 최적화 연산의 전체 데이터와 결과 점수를 관리하는 Planning Solution 클래스입니다.
 * <p>
 * 최적화 엔진이 참조할 수 있는 고정 자산(카드 목록)과 엔진이 직접 결정해야 할 가변 요소(지출 할당 목록)를
 * 하나의 컨텍스트로 통합하여, 최종적인 혜택 배정 시나리오를 완성합니다.
 * </p>
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardSolutionV1 {

    /** 최적화 엔진이 각 지출 항목에 배정할 수 있는 신용카드 후보군 리스트입니다.
     * <p>
     * ValueRangeProvider 설정을 통해 엔진은 이 리스트 내부의 카드 객체들을
     * 개별 지출 항목에 할당할 수 있는 '유효한 값의 범위'로 인식합니다.
     * </p>
     */
    @ValueRangeProvider(id = "cardRange")
    private List<CreditCardV1> cardList;

    /** 개별 지출 항목과 해당 항목에 할당된 카드 정보가 담긴 엔티티 리스트입니다.
     * <p>
     * PlanningEntityCollectionProperty 어노테이션은 엔진에게 이 리스트가
     * 실시간으로 최적화되어야 하는 대상임을 알려주며, 엔진은 탐색 과정에서
     * 리스트 내의 각 엔티티에 적합한 카드를 배치합니다.
     * </p>
     */
    @PlanningEntityCollectionProperty
    private List<CardAssignmentV1> assignmentList;

    /** 최적화 엔진에 의해 산출된 시나리오의 최종 수리적 품질 점수입니다.
     * <p>
     * Hard Score: 카드별 목표 실적 달성 여부 등 위반하면 안 되는 절대 제약 조건을 체크합니다. <br>
     * Soft Score: 실적 충족 후 사용자가 받게 되는 총 혜택 금액을 최대화하는 지표로 사용됩니다.
     * </p>
     */
    @PlanningScore
    private HardSoftScore score;
}