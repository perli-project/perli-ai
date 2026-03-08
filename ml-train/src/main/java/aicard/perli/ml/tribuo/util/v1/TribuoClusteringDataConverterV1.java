package aicard.perli.ml.tribuo.util.v1;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.MutableDataset;
import org.tribuo.clustering.ClusterID;
import org.tribuo.clustering.ClusteringFactory;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.impl.EmptyDatasetProvenance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 카드 소비 집계 CSV를 Tribuo K-Means 학습용 데이터셋으로 변환합니다.
 */
@Slf4j
public class TribuoClusteringDataConverterV1 {

    private static final String[] FEATURE_NAMES = {
            "total_amount",
            "tx_count",
            "avg_installments",
            "max_amount",
            "avg_amount",
            "authorized_ratio"
    };

    /**
     * 학습 및 품질평가에 필요한 데이터 묶음입니다.
     */
    @Getter
    public static class PreparedClusteringData {
        private final MutableDataset<ClusterID> dataset;
        private final List<String> cardIds;
        private final List<double[]> normalizedVectors;
        private final List<double[]> rawVectors;

        public PreparedClusteringData(
                MutableDataset<ClusterID> dataset,
                List<String> cardIds,
                List<double[]> normalizedVectors,
                List<double[]> rawVectors
        ) {
            this.dataset = dataset;
            this.cardIds = cardIds;
            this.normalizedVectors = normalizedVectors;
            this.rawVectors = rawVectors;
        }
    }

    /**
     * CSV를 읽어 이상치 완화 + 스케일링 전처리 후 Tribuo 데이터셋으로 구성합니다.
     */
    public PreparedClusteringData loadDataset(String csvPath) throws IOException {
        List<String> cardIds = new ArrayList<>();
        List<double[]> rawVectors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine();
            if (header == null) {
                throw new IOException("CSV가 비어 있습니다: " + csvPath);
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 8) {
                    continue;
                }
                cardIds.add(v[0]);
                rawVectors.add(new double[]{
                        Double.parseDouble(v[1]),
                        Double.parseDouble(v[2]),
                        Double.parseDouble(v[3]),
                        Double.parseDouble(v[4]),
                        Double.parseDouble(v[5]),
                        Double.parseDouble(v[6])
                });
            }
        }

        List<double[]> normalized = preprocess(rawVectors);
        ClusteringFactory factory = new ClusteringFactory();
        MutableDataset<ClusterID> dataset = new MutableDataset<>(new EmptyDatasetProvenance(), factory);

        for (double[] vector : normalized) {
            dataset.add(new ArrayExample<>(ClusteringFactory.UNASSIGNED_CLUSTER_ID, FEATURE_NAMES, vector));
        }

        log.info("클러스터링 데이터셋 준비 완료 ({}건)", dataset.size());
        return new PreparedClusteringData(dataset, cardIds, normalized, rawVectors);
    }

    private List<double[]> preprocess(List<double[]> rows) {
        if (rows.isEmpty()) {
            return rows;
        }

        int dim = rows.get(0).length;
        List<double[]> transformed = new ArrayList<>(rows.size());
        for (double[] row : rows) {
            double[] out = new double[dim];
            for (int i = 0; i < dim; i++) {
                out[i] = signedLog1p(row[i]);
            }
            transformed.add(out);
        }

        double[] lower = new double[dim];
        double[] upper = new double[dim];
        for (int i = 0; i < dim; i++) {
            lower[i] = percentile(transformed, i, 1.0);
            upper[i] = percentile(transformed, i, 99.0);
        }

        for (double[] row : transformed) {
            for (int i = 0; i < dim; i++) {
                if (row[i] < lower[i]) {
                    row[i] = lower[i];
                } else if (row[i] > upper[i]) {
                    row[i] = upper[i];
                }
            }
        }

        return robustScale(transformed);
    }

    private List<double[]> robustScale(List<double[]> rows) {
        int dim = rows.get(0).length;
        double[] median = new double[dim];
        double[] q1 = new double[dim];
        double[] q3 = new double[dim];

        for (int i = 0; i < dim; i++) {
            median[i] = percentile(rows, i, 50.0);
            q1[i] = percentile(rows, i, 25.0);
            q3[i] = percentile(rows, i, 75.0);
        }

        List<double[]> normalized = new ArrayList<>(rows.size());
        for (double[] row : rows) {
            double[] out = new double[dim];
            for (int i = 0; i < dim; i++) {
                double iqr = q3[i] - q1[i];
                out[i] = iqr == 0.0 ? 0.0 : (row[i] - median[i]) / iqr;
            }
            normalized.add(out);
        }
        return normalized;
    }

    private double percentile(List<double[]> rows, int featureIdx, double p) {
        double[] values = new double[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            values[i] = rows.get(i)[featureIdx];
        }
        java.util.Arrays.sort(values);
        if (values.length == 1) {
            return values[0];
        }

        double rank = (p / 100.0) * (values.length - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);
        if (low == high) {
            return values[low];
        }
        double weight = rank - low;
        return values[low] * (1.0 - weight) + values[high] * weight;
    }

    private double signedLog1p(double value) {
        if (value == 0.0) {
            return 0.0;
        }
        return Math.signum(value) * Math.log1p(Math.abs(value));
    }
}
