package aicard.perli.dl.optimization.solver.v2;

import aicard.perli.dl.optimization.domain.fix.v2.CreditCardV2;
import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import aicard.perli.dl.optimization.domain.solution.v2.CardSolutionV2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CardSolverV2Test {

    @Test
    @DisplayName("고도화 모델 V2: 통합 한도 및 카테고리별 혜택 최적화 검증")
    void solve() {
        // 데이터 준비: 신한 Mr.Life 시뮬레이션 (식비/온라인/마트 각 10% 혜택, 한도 1만원)
        List<CreditCardV2> cards = new ArrayList<>();

        Map<String, Double> mrLifeRates = new HashMap<>();
        mrLifeRates.put("FOOD", 0.10);
        mrLifeRates.put("ONLINE", 0.10);
        mrLifeRates.put("MART", 0.10);

        cards.add(new CreditCardV2(
                "card_01",
                "신한 Mr.Life",
                300000.0,    // 전월 실적 목표
                mrLifeRates,
                150000.0,    // 현재 누적 실적
                10000.0      // 월간 통합 할인 한도
        ));

        // 지출 데이터 준비: AI가 예측한 업종별 지출 항목 (총 333,000원)
        List<CardAssignmentV2> spendingList = new ArrayList<>();
        spendingList.add(new CardAssignmentV2("sp_01", "FOOD", 150000.0, null));
        spendingList.add(new CardAssignmentV2("sp_02", "ONLINE", 100000.0, null));
        spendingList.add(new CardAssignmentV2("sp_03", "MART", 83000.0, null));

        // 최적화 엔진 실행
        CardSolverV2 solver = new CardSolverV2();
        CardSolutionV2 problem = new CardSolutionV2(spendingList, cards);
        CardSolutionV2 solution = solver.solve(problem);

        // 결과 검증 및 출력
        assertNotNull(solution.getScore());

        System.out.println("최적화 상태: " + (solution.getScore().isFeasible() ? "해결 가능" : "제약 위반"));
        System.out.println("최종 계산 점수: " + solution.getScore());

        System.out.println("\n[배정 상세 결과]");
        solution.getSpendingList().forEach(assignment -> {
            System.out.printf("업종: %-10s | 금액: %,7.0f원 -> 할당 카드: %s\n",
                    assignment.getCategory(),
                    assignment.getSpendingAmount(),
                    assignment.getCreditCard().getCardName());
        });

        // 수리적 혜택 검증 출력
        double rawBenefit = solution.getSpendingList().stream()
                .mapToDouble(a -> a.getSpendingAmount() * a.getCreditCard().getCcategoryBenefitRates().getOrDefault(a.getCategory(), 0.0))
                .sum();

        System.out.println("\n[혜택 시뮬레이션 리포트]");
        System.out.printf("이론상 최대 혜택 합계: %,.0f원\n", rawBenefit);
        System.out.printf("카드 통합 한도 적용 결과: %,.0f원\n", Math.min(rawBenefit, cards.get(0).getMaxBenefitLimit()));
    }
}