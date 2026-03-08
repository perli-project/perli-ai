package aicard.perli.dl.optimization.backtest.v2;

import aicard.perli.dl.optimization.domain.fix.v2.CreditCardV2;
import aicard.perli.dl.optimization.domain.solution.v2.CardSolutionV2;
import aicard.perli.dl.optimization.domain.unfix.v2.CardAssignmentV2;
import aicard.perli.dl.optimization.solver.v2.CardSolverV2;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 최적화 플래너 백테스트 앱.
 * Baseline(단순 최고 혜택률 선택) 대비 최적화 전략의 월별 절감 효과를 비교합니다.
 */
@Slf4j
public class OptimizationBacktestAppV2 {

    public static void main(String[] args) {
        List<Map<String, Double>> monthlyActualSpendingScenarios = buildScenarios();
        double baselineTotal = 0.0;
        double optimizedTotal = 0.0;

        for (int i = 0; i < monthlyActualSpendingScenarios.size(); i++) {
            List<CreditCardV2> cardsForBaseline = buildCards();
            List<CreditCardV2> cardsForOptimized = buildCards();

            List<CardAssignmentV2> monthSpendForBaseline = toAssignments(monthlyActualSpendingScenarios.get(i));
            List<CardAssignmentV2> monthSpendForOptimized = toAssignments(monthlyActualSpendingScenarios.get(i));

            allocateBaseline(monthSpendForBaseline, cardsForBaseline);
            double baselineBenefit = evaluateBenefit(monthSpendForBaseline);

            CardSolutionV2 solved = new CardSolverV2().solve(new CardSolutionV2(monthSpendForOptimized, cardsForOptimized));
            double optimizedBenefit = evaluateBenefit(solved.getSpendingList());

            baselineTotal += baselineBenefit;
            optimizedTotal += optimizedBenefit;

            log.info("[Month {}] Baseline={}원 | Optimized={}원 | Delta={}원 | Score={}",
                    i + 1,
                    String.format("%,.0f", baselineBenefit),
                    String.format("%,.0f", optimizedBenefit),
                    String.format("%,.0f", optimizedBenefit - baselineBenefit),
                    solved.getScore());
        }

        double totalDelta = optimizedTotal - baselineTotal;
        double liftPct = baselineTotal == 0 ? 0 : (totalDelta / baselineTotal) * 100.0;

        log.info("========== Optimization Backtest V2 ==========");
        log.info("Baseline total benefit: {}원", String.format("%,.0f", baselineTotal));
        log.info("Optimized total benefit: {}원", String.format("%,.0f", optimizedTotal));
        log.info("Total incremental saving: {}원", String.format("%,.0f", totalDelta));
        log.info("Lift: {}%", String.format("%.2f", liftPct));
    }

    private static List<CardAssignmentV2> toAssignments(Map<String, Double> scenario) {
        List<CardAssignmentV2> list = new ArrayList<>();
        int id = 1;
        for (Map.Entry<String, Double> e : scenario.entrySet()) {
            list.add(new CardAssignmentV2("bt_" + id++, e.getKey(), e.getValue(), null));
        }
        return list;
    }

    /**
     * 단순 기준선 전략으로 모든 지출을 주력 카드 1장에 배정합니다.
     */
    private static void allocateBaseline(List<CardAssignmentV2> assignments, List<CreditCardV2> cards) {
        CreditCardV2 primary = cards.stream()
                .max((c1, c2) -> Double.compare(
                        c1.getCcategoryBenefitRates().values().stream().mapToDouble(Double::doubleValue).sum(),
                        c2.getCcategoryBenefitRates().values().stream().mapToDouble(Double::doubleValue).sum()))
                .orElse(cards.get(0));

        for (CardAssignmentV2 a : assignments) {
            a.setCreditCard(primary);
        }
    }

    private static double evaluateBenefit(List<CardAssignmentV2> assignments) {
        Map<CreditCardV2, Double> spendByCard = new LinkedHashMap<>();
        Map<CreditCardV2, Double> rawBenefitByCard = new LinkedHashMap<>();

        for (CardAssignmentV2 a : assignments) {
            CreditCardV2 card = a.getCreditCard();
            if (card == null) continue;

            spendByCard.merge(card, a.getSpendingAmount(), Double::sum);

            if (card.getRestrictedCategories().contains(a.getCategory())) continue;
            if (a.getSpendingAmount() < card.getMinimumTransactionAmountForBenefit()) continue;

            double rate = card.getCcategoryBenefitRates().getOrDefault(a.getCategory(), 0.0);
            rawBenefitByCard.merge(card, a.getSpendingAmount() * rate, Double::sum);
        }

        double total = 0.0;
        for (Map.Entry<CreditCardV2, Double> e : rawBenefitByCard.entrySet()) {
            CreditCardV2 card = e.getKey();
            double spend = spendByCard.getOrDefault(card, 0.0);
            double projectedPerformance = card.getCurrentPerformance() + spend;

            if (projectedPerformance < card.getMinimumSpendRequired()) continue;

            double multiplier = card.resolvePerformanceBandMultiplier(projectedPerformance);
            double adjusted = e.getValue() * multiplier;
            total += Math.min(adjusted, card.getMaxBenefitLimit());
        }
        return total;
    }

    private static List<CreditCardV2> buildCards() {
        List<CreditCardV2> cards = new ArrayList<>();

        CreditCardV2 life = new CreditCardV2(
                "cardA", "NH 라이프", 300000, mapOf("FOOD", 0.10, "MART", 0.08, "ONLINE", 0.05), 180000, 15000
        );
        life.setMinimumSpendRequired(200000);
        life.setMinimumTransactionAmountForBenefit(10000);
        life.setRestrictedCategories(new java.util.HashSet<>(List.of("FUEL")));
        life.setPerformanceBandMultipliers(new TreeMap<>(Map.of(0.0, 1.0, 300000.0, 1.05, 600000.0, 1.12)));

        CreditCardV2 digital = new CreditCardV2(
                "cardB", "NH 디지털", 400000, mapOf("ONLINE", 0.12, "SUBSCRIPTION", 0.15, "TRANSPORT", 0.05), 240000, 20000
        );
        digital.setMinimumSpendRequired(250000);
        digital.setMinimumTransactionAmountForBenefit(7000);
        digital.setRestrictedCategories(new java.util.HashSet<>(List.of("MART")));
        digital.setPerformanceBandMultipliers(new TreeMap<>(Map.of(0.0, 1.0, 400000.0, 1.08, 700000.0, 1.15)));

        CreditCardV2 drive = new CreditCardV2(
                "cardC", "NH 드라이브", 250000, mapOf("FUEL", 0.10, "TRANSPORT", 0.08, "FOOD", 0.03), 110000, 12000
        );
        drive.setMinimumSpendRequired(150000);
        drive.setMinimumTransactionAmountForBenefit(5000);
        drive.setRestrictedCategories(new java.util.HashSet<>(List.of("SUBSCRIPTION")));
        drive.setPerformanceBandMultipliers(new TreeMap<>(Map.of(0.0, 1.0, 250000.0, 1.06, 500000.0, 1.1)));

        cards.add(life);
        cards.add(digital);
        cards.add(drive);
        return cards;
    }

    private static List<Map<String, Double>> buildScenarios() {
        List<Map<String, Double>> scenarios = new ArrayList<>();

        scenarios.add(mapOf("FOOD", 210000.0, "MART", 130000.0, "ONLINE", 180000.0, "TRANSPORT", 90000.0, "SUBSCRIPTION", 65000.0, "FUEL", 120000.0));
        scenarios.add(mapOf("FOOD", 240000.0, "MART", 110000.0, "ONLINE", 200000.0, "TRANSPORT", 100000.0, "SUBSCRIPTION", 80000.0, "FUEL", 90000.0));
        scenarios.add(mapOf("FOOD", 190000.0, "MART", 160000.0, "ONLINE", 230000.0, "TRANSPORT", 120000.0, "SUBSCRIPTION", 70000.0, "FUEL", 140000.0));

        return scenarios;
    }

    private static Map<String, Double> mapOf(Object... kv) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (Double) kv[i + 1]);
        }
        return map;
    }
}
