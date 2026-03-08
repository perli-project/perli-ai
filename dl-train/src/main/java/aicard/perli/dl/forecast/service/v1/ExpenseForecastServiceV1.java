package aicard.perli.dl.forecast.service.v1;

/**
 * 월말 지출 예측 서비스.
 * ARIMA 예측값과 (선택) LSTM 예측값을 앙상블합니다.
 */
public class ExpenseForecastServiceV1 {

    private final ArimaForecasterV1 arimaForecaster = new ArimaForecasterV1();

    /**
     * @param monthlySpentHistory 월별 누적 지출 이력 (최소 2개)
     * @param lstmPrediction LSTM 기반 월말 예측값 (없으면 null)
     * @return 최종 월말 예상 지출액
     */
    public double forecastMonthEndExpense(double[] monthlySpentHistory, Double lstmPrediction) {
        double arima = forecastByArima(monthlySpentHistory);
        if (lstmPrediction == null || lstmPrediction <= 0) return arima;
        return forecastByEnsemble(arima, lstmPrediction);
    }

    public double forecastByArima(double[] monthlySpentHistory) {
        return arimaForecaster.forecastNext(monthlySpentHistory, 1);
    }

    public double forecastByEnsemble(double arimaPrediction, double lstmPrediction) {
        return 0.6 * arimaPrediction + 0.4 * lstmPrediction;
    }

    public double absoluteError(double prediction, double actual) {
        return Math.abs(prediction - actual);
    }
}
