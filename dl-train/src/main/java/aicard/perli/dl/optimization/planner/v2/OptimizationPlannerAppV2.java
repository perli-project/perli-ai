package aicard.perli.dl.optimization.planner.v2;

import aicard.perli.dl.forecast.service.v1.ExpenseForecastServiceV1;
import aicard.perli.dl.optimization.domain.fix.v2.CreditCardV2;
import aicard.perli.dl.optimization.domain.solution.v2.CardSolutionV2;
import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import aicard.perli.dl.optimization.solver.v2.CardSolverV2;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ARIMA/LSTM 예측값을 기반으로 월말 최적 결제 시나리오를 생성하는 실행 앱.
 */
@Slf4j
public class OptimizationPlannerAppV2 {

    public static void main(String[] args) {
        ExpenseForecastServiceV1 forecastService = new ExpenseForecastServiceV1();

        double[] monthlyHistory = {680000, 720000, 700000, 760000, 810000, 845000};
        Double lstmPrediction = null;
        double predictedMonthEnd = forecastService.forecastMonthEndExpense(monthlyHistory, lstmPrediction);

        List<CreditCardV2> cards = buildCards();
        List<CardAssignmentV2> spendingPlan = buildPredictedSpendings(predictedMonthEnd);

        CardSolutionV2 problem = new CardSolutionV2(spendingPlan, cards);
        CardSolverV2 solver = new CardSolverV2();
        CardSolutionV2 solved = solver.solve(problem);

        log.info("========== Optimization Planner V2 ==========");
        log.info("예상 월말 지출액: {}원", String.format("%,.0f", predictedMonthEnd));
        log.info("최종 점수: {}", solved.getScore());
        for (CardAssignmentV2 a : solved.getSpendingList()) {
            log.info("카테고리={} 금액={}원 -> 카드={}",
                    a.getCategory(),
                    String.format("%,.0f", a.getSpendingAmount()),
                    a.getCreditCard() == null ? "N/A" : a.getCreditCard().getCardName());
        }
    }

    private static List<CreditCardV2> buildCards() {
        List<CreditCardV2> cards = new ArrayList<>();

        Map<String, Double> cardA = new HashMap<>();
        cardA.put("FOOD", 0.10);
        cardA.put("MART", 0.08);
        cardA.put("ONLINE", 0.05);
        cards.add(new CreditCardV2("cardA", "NH 라이프", 300000, cardA, 180000, 15000));

        Map<String, Double> cardB = new HashMap<>();
        cardB.put("ONLINE", 0.12);
        cardB.put("SUBSCRIPTION", 0.15);
        cardB.put("TRANSPORT", 0.05);
        cards.add(new CreditCardV2("cardB", "NH 디지털", 400000, cardB, 240000, 20000));

        Map<String, Double> cardC = new HashMap<>();
        cardC.put("FUEL", 0.10);
        cardC.put("TRANSPORT", 0.08);
        cardC.put("FOOD", 0.03);
        cards.add(new CreditCardV2("cardC", "NH 드라이브", 250000, cardC, 110000, 12000));

        return cards;
    }

    private static List<CardAssignmentV2> buildPredictedSpendings(double total) {

        Map<String, Double> ratio = new HashMap<>();
        ratio.put("FOOD", 0.28);
        ratio.put("MART", 0.15);
        ratio.put("ONLINE", 0.22);
        ratio.put("TRANSPORT", 0.12);
        ratio.put("SUBSCRIPTION", 0.08);
        ratio.put("FUEL", 0.15);

        List<CardAssignmentV2> result = new ArrayList<>();
        int idx = 1;
        for (Map.Entry<String, Double> e : ratio.entrySet()) {
            double amt = total * e.getValue();
            result.add(new CardAssignmentV2("sp_" + idx++, e.getKey(), amt, null));
        }
        return result;
    }
}
