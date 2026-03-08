package aicard.perli.dl.forecast.service.v1;

/**
 * 단변량 시계열용 경량 ARIMA(1,1,1) 근사 예측기.
 */
public class ArimaForecasterV1 {

    /**
     * 최근 값 시퀀스를 기반으로 향후 steps 시점 값을 예측합니다.
     * 데이터가 짧을 경우 drift 기반으로 fallback 합니다.
     */
    public double forecastNext(double[] series, int steps) {
        if (series == null || series.length < 2 || steps <= 0) {
            return 0.0;
        }

        double[] diff = new double[series.length - 1];
        for (int i = 1; i < series.length; i++) {
            diff[i - 1] = series[i] - series[i - 1];
        }

        if (diff.length < 3) {
            return simpleDriftForecast(series, steps);
        }

        double meanPrev = mean(slice(diff, 0, diff.length - 1));
        double meanCurr = mean(slice(diff, 1, diff.length));

        double num = 0.0;
        double den = 0.0;
        for (int i = 1; i < diff.length; i++) {
            double x = diff[i - 1] - meanPrev;
            double y = diff[i] - meanCurr;
            num += x * y;
            den += x * x;
        }
        double phi = den == 0.0 ? 0.0 : clamp(num / den, -0.99, 0.99);
        double c = meanCurr - phi * meanPrev;

        double[] residual = new double[diff.length - 1];
        for (int i = 1; i < diff.length; i++) {
            residual[i - 1] = diff[i] - (c + phi * diff[i - 1]);
        }
        double theta = estimateLag1Autocorr(residual);

        double lastLevel = series[series.length - 1];
        double lastDiff = diff[diff.length - 1];
        double lastResidual = residual.length > 0 ? residual[residual.length - 1] : 0.0;

        for (int h = 0; h < steps; h++) {
            double nextDiff = c + phi * lastDiff + theta * lastResidual;
            lastLevel += nextDiff;
            lastResidual = 0.0;
            lastDiff = nextDiff;
        }
        return Math.max(lastLevel, 0.0);
    }

    private double simpleDriftForecast(double[] series, int steps) {
        double slope = (series[series.length - 1] - series[0]) / Math.max(series.length - 1, 1);
        double forecast = series[series.length - 1] + slope * steps;
        return Math.max(forecast, 0.0);
    }

    private double estimateLag1Autocorr(double[] v) {
        if (v.length < 2) {
            return 0.0;
        }
        double m1 = mean(slice(v, 0, v.length - 1));
        double m2 = mean(slice(v, 1, v.length));
        double num = 0.0;
        double den = 0.0;
        for (int i = 1; i < v.length; i++) {
            double x = v[i - 1] - m1;
            double y = v[i] - m2;
            num += x * y;
            den += x * x;
        }
        if (den == 0.0) {
            return 0.0;
        }
        return clamp(num / den, -0.99, 0.99);
    }

    private double[] slice(double[] v, int start, int endExclusive) {
        double[] out = new double[endExclusive - start];
        System.arraycopy(v, start, out, 0, out.length);
        return out;
    }

    private double mean(double[] v) {
        double s = 0.0;
        for (double x : v) {
            s += x;
        }
        return v.length == 0 ? 0.0 : s / v.length;
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
