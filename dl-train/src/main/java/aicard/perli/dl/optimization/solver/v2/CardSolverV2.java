package aicard.perli.dl.optimization.solver.v2;

import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import aicard.perli.dl.optimization.score.v2.CardScoreV2;
import aicard.perli.dl.optimization.domain.solution.v2.CardSolutionV2;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Timefold 엔진을 구동하여 V2 모델 기준 최적의 카드 할당 플랜을 계산하는 서비스입니다.
 * 카테고리별 할인율과 통합 한도를 고려하여 연산을 수행합니다.
 */
@NoArgsConstructor
public class CardSolverV2 {

    /**
     * 입력된 데이터(지출 목록, 카드 목록)를 바탕으로 최적화 계산을 실행합니다.
     *
     * @param problem AI 예측 지출과 카드 혜택 정보가 담긴 초기 솔루션
     * @return 혜택이 극대화된 배정 결과가 포함된 결과 솔루션
     */
    public CardSolutionV2 solve(CardSolutionV2 problem) {
        // 엔진 설정
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(CardSolutionV2.class)      // 보따리 클래스
                .withEntityClasses(CardAssignmentV2.class)    // 변동 데이터(빈칸)
                .withConstraintProviderClass(CardScoreV2.class) // 채점 두뇌
                // 연산 시간 제한
                .withTerminationSpentLimit(Duration.ofSeconds(5));

        // 솔버 팩토리 생성 및 솔버 빌드
        SolverFactory<CardSolutionV2> solverFactory = SolverFactory.create(solverConfig);
        Solver<CardSolutionV2> solver = solverFactory.buildSolver();

        // 최적의 해답 도출 (동기적 실행)
        return solver.solve(problem);
    }
}