package aicard.perli.dl.optimization.service.v2;

import aicard.perli.dl.optimization.domain.solution.v2.CardSolutionV2;
import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import aicard.perli.dl.optimization.score.v2.CardScoreV2;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

/**
 * 고도화된 V2 규격의 최적화 연산을 수행하는 독립형 엔진 클래스입니다.
 * <p>
 * 업종별 차등 할인율과 카드사 통합 할인 한도(Cap)를 고려하는 V2 채점 로직을 실행하며,
 * 외부 모듈로부터 전달받은 AI 예측 지출 데이터를 바탕으로 정밀한 카드 배정 가이드를 산출합니다.
 * </p>
 */
public class OptimizationServiceV2 {

    /**
     * V2 최적화 알고리즘을 가동하여 지출 항목별 최적 카드를 결정합니다.
     * <p>
     * Score 불러오기: 고도화된 CardScoreV2(한도/업종/실적) 규칙 적용 <br>
     * Solver 조립: V2 전용 Solution 및 Entity 구조 설정 <br>
     * Solution 실행: 다차원 제약 조건을 만족하는 최상의 조합 탐색
     * </p>
     * @param problem 고도화된 카드 정보와 예측 지출이 담긴 V2 솔루션 객체
     * @return 통합 한도 내에서 혜택이 극대화된 결과 객체
     */
    public CardSolutionV2 solve(CardSolutionV2 problem) {

        // V2의 핵심인 업종별 매칭 로직과 한도 방어 규칙(CardScoreV2)을 엔진의 뇌로 이식합니다.
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(CardSolutionV2.class)
                .withEntityClasses(CardAssignmentV2.class)
                .withConstraintProviderClass(CardScoreV2.class);

        // 최적화 연산을 수행할 솔버 객체를 생성하기 위한 팩토리를 빌드합니다.
        SolverFactory<CardSolutionV2> solverFactory = SolverFactory.create(solverConfig);

        // AI가 예측한 미래 지출 데이터를 입력받아 수학적으로 가장 유리한 카드 조합을 확정합니다.
        Solver<CardSolutionV2> solver = solverFactory.buildSolver();

        // 배정 결과와 최종 최적화 점수(Score)가 포함된 정답지를 반환합니다.
        return solver.solve(problem);
    }
}