package aicard.perli.dl.optimization.service.v1;

import aicard.perli.dl.optimization.domain.solution.v1.CardSolutionV1;
import aicard.perli.dl.optimization.domain.unfix.v1.CardAssignmentV1;
import aicard.perli.dl.optimization.score.v1.CardScoreV1;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

/**
 * 외부 모듈에서 호출하여 카드 혜택 최적화를 수행하는 V1 전용 엔진 클래스입니다.
 * <p>
 * 스프링 프레임워크에 의존하지 않는 순수 자바 구조로 설계되었으며,
 * V1 규격의 채점 규칙(Score)과 데이터 구조(Solution)를 결합하여 최적의 배정 안을 산출합니다.
 * </p>
 */
public class OptimizationServiceV1 {

    /**
     * V1 최적화 알고리즘을 실행하여 지출별 최적 카드를 할당합니다.
     * <p>
     * Score 불러오기: CardScoreV1 규칙 적용 <br>
     * Solver 조립: Solution 및 Entity 클래스 매핑 <br>
     * Solution 실행: 전달받은 문제(Problem)를 풀어 최적해 반환
     * </p>
     * @param problem 카드 목록과 지출 데이터가 포함된 초기 솔루션 객체
     * @return 최적의 카드 할당 결과와 최종 점수가 포함된 결과 객체
     */
    public CardSolutionV1 solve(CardSolutionV1 problem) {

        // 엔진이 참조할 솔루션 구조, 변동 엔티티, 그리고 형님이 만든 채점 로직을 조립합니다.
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(CardSolutionV1.class)
                .withEntityClasses(CardAssignmentV1.class)
                .withConstraintProviderClass(CardScoreV1.class);

        // 설정을 바탕으로 엔진을 생성할 준비를 합니다.
        SolverFactory<CardSolutionV1> solverFactory = SolverFactory.create(solverConfig);

        // 실제 최적화 연산을 수행하는 솔버를 빌드하고 문제를 해결합니다.
        Solver<CardSolutionV1> solver = solverFactory.buildSolver();

        // 계산된 최적의 배정 시나리오를 반환합니다.
        return solver.solve(problem);
    }
}