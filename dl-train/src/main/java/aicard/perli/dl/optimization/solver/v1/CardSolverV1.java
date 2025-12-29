package aicard.perli.dl.optimization.solver.v1;

import aicard.perli.dl.optimization.domain.solution.v1.CardOptimizationSolutionV1;
import aicard.perli.dl.optimization.domain.unfix.v1.CardAssignmentV1; // 추가
import aicard.perli.dl.optimization.score.v1.CardScoreV1;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import lombok.RequiredArgsConstructor;
import java.time.Duration;

/**
 * Timefold 엔진을 구동하여 최적의 카드 할당 플랜을 계산하는 서비스입니다.
 */
@RequiredArgsConstructor
public class CardSolverV1 {

    /**
     * 입력된 데이터를 바탕으로 최적화 계산을 실행하여 결과 플랜을 반환합니다.
     *
     * @param problem 카드 목록과 지출 항목이 담긴 초기 솔루션
     * @return 최적화(배정)가 완료된 결과 솔루션
     */
    public CardOptimizationSolutionV1 solve(CardOptimizationSolutionV1 problem) {
        // 엔진 설정
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(CardOptimizationSolutionV1.class)
                .withEntityClasses(CardAssignmentV1.class)
                .withConstraintProviderClass(CardScoreV1.class)
                // 연산 시간 제한 (5초)
                .withTerminationSpentLimit(Duration.ofSeconds(5));

        // 솔버 빌드 및 실행
        SolverFactory<CardOptimizationSolutionV1> solverFactory = SolverFactory.create(solverConfig);
        Solver<CardOptimizationSolutionV1> solver = solverFactory.buildSolver();

        // 최적의 해답 도출
        return solver.solve(problem);
    }
}