package aicard.perli.dl.forecast.app.v1;

import aicard.perli.dl.forecast.service.v1.ExpenseForecastServiceV1;
import lombok.extern.slf4j.Slf4j;

/**
 * ARIMA/LSTM/Ensemble 예측 성능을 간단 비교하는 백테스트 앱.
 */
@Slf4j
public class ForecastComparisonAppV1 {

    public static void main(String[] args) {
        ExpenseForecastServiceV1 service = new ExpenseForecastServiceV1();

        double[][] histories = {
                {640000, 690000, 705000, 730000, 755000, 780000},
                {420000, 450000, 440000, 470000, 495000, 510000},
                {880000, 860000, 910000, 940000, 965000, 990000},
                {530000, 520000, 545000, 560000, 590000, 615000}
        };
        double[] actualNext = {802000, 525000, 1015000, 632000};

        double[] lstmPredictions = {810000, 515000, 1008000, 640000};

        double arimaMae = 0.0;
        double lstmMae = 0.0;
        double ensembleMae = 0.0;

        for (int i = 0; i < histories.length; i++) {
            double arima = service.forecastByArima(histories[i]);
            double lstm = lstmPredictions[i];
            double ensemble = service.forecastByEnsemble(arima, lstm);
            double actual = actualNext[i];

            arimaMae += service.absoluteError(arima, actual);
            lstmMae += service.absoluteError(lstm, actual);
            ensembleMae += service.absoluteError(ensemble, actual);

            log.info("[Case {}] actual={} arima={} lstm={} ensemble={}",
                    i + 1,
                    String.format("%,.0f", actual),
                    String.format("%,.0f", arima),
                    String.format("%,.0f", lstm),
                    String.format("%,.0f", ensemble));
        }

        arimaMae /= histories.length;
        lstmMae /= histories.length;
        ensembleMae /= histories.length;

        log.info("========== Forecast Comparison V1 ==========");
        log.info("ARIMA MAE: {}", String.format("%,.2f", arimaMae));
        log.info("LSTM MAE: {}", String.format("%,.2f", lstmMae));
        log.info("Ensemble MAE: {}", String.format("%,.2f", ensembleMae));

        String best = "ARIMA";
        double bestMae = arimaMae;
        if (lstmMae < bestMae) {
            best = "LSTM";
            bestMae = lstmMae;
        }
        if (ensembleMae < bestMae) {
            best = "Ensemble";
        }
        log.info("Best by MAE: {}", best);
    }
}
