package aicard.perli.dl.optimization.solver.v1;

import aicard.perli.dl.optimization.domain.fix.v1.CreditCardV1;
import aicard.perli.dl.optimization.domain.solution.v1.CardSolutionV1;
import aicard.perli.dl.optimization.domain.unfix.v1.CardAssignmentV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardSolverV1Test {

    @Test
    @DisplayName("카드 최적화 엔진 통합 테스트: 실적 달성 및 혜택 극대화 검증")
    void solveTest() {
        // Given: 카드 구성 (고혜택/높은실적 vs 저혜택/무실적)
        CreditCardV1 highBenefitCard = new CreditCardV1("CARD-01", "고혜택카드", 300000.0, 0.05); // 30만 실적 필요
        CreditCardV1 basicCard = new CreditCardV1("CARD-02", "기본카드", 0.0, 0.01);         // 실적 필요 없음

        List<CreditCardV1> cardList = Arrays.asList(highBenefitCard, basicCard);

        // Given: LSTM이 예측한 지출 목록 (총 40만원)
        List<CardAssignmentV1> assignmentList = Arrays.asList(
                new CardAssignmentV1("SPEND-01", 150000.0, null),
                new CardAssignmentV1("SPEND-02", 150000.0, null),
                new CardAssignmentV1("SPEND-03", 100000.0, null)
        );

        CardSolutionV1 problem = new CardSolutionV1(cardList, assignmentList, null);
        CardSolverV1 solver = new CardSolverV1();

        // When: 최적화 실행
        CardSolutionV1 result = solver.solve(problem);

        // Then: 결과 검증
        assertNotNull(result.getScore(), "결과 점수가 산출되어야 합니다.");

        System.out.println("=== 최적화 결과 ===");
        System.out.println("최종 점수: " + result.getScore());

        double highCardTotal = 0;
        for (CardAssignmentV1 a : result.getAssignmentList()) {
            System.out.printf("항목: %s | 금액: %.0f | 카드: %s\n",
                    a.getSpendingId(), a.getSpendingAmount(), a.getCreditCard().getCardName());

            if (a.getCreditCard().getCardId().equals("CARD-01")) {
                highCardTotal += a.getSpendingAmount();
            }
        }

        // 고혜택 카드의 실적(30만)을 채우는 것이 이득이므로, 30만 원 이상 할당되었는지 확인
        assertTrue(highCardTotal >= 300000.0, "엔진은 고혜택 카드의 실적을 우선적으로 채워야 합니다.");
    }
}