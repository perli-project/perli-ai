package aicard.perli.ml.tribuo.service.v1;

import aicard.perli.ml.tribuo.util.v1.TribuoClusteringDataConverterV1;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.clustering.ClusterID;
import org.tribuo.clustering.kmeans.KMeansTrainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tribuo K-Means 기반 소비 패턴 그룹화를 수행합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoClusteringServiceV1 {

    private final TribuoClusteringDataConverterV1 converter;

    @Getter
    public static class ClusteringResult {
        private final Model<ClusterID> model;
        private final List<String> cardIds;
        private final List<Integer> clusterIds;
        private final double daviesBouldinIndex;
        private final double calinskiHarabaszIndex;
        private final int k;

        public ClusteringResult(
                Model<ClusterID> model,
                List<String> cardIds,
                List<Integer> clusterIds,
                double daviesBouldinIndex,
                double calinskiHarabaszIndex,
                int k
        ) {
            this.model = model;
            this.cardIds = cardIds;
            this.clusterIds = clusterIds;
            this.daviesBouldinIndex = daviesBouldinIndex;
            this.calinskiHarabaszIndex = calinskiHarabaszIndex;
            this.k = k;
        }
    }

    public ClusteringResult cluster(String csvPath, int k, int iterations, long seed) throws Exception {
        TribuoClusteringDataConverterV1.PreparedClusteringData prepared = converter.loadDataset(csvPath);
        return runKMeans(prepared, k, iterations, seed, true);
    }

    public ClusteringResult clusterAutoK(
            String csvPath,
            int minK,
            int maxK,
            int iterations,
            long seed,
            double minClusterRatio,
            String evalReportPath
    ) throws Exception {
        TribuoClusteringDataConverterV1.PreparedClusteringData prepared = converter.loadDataset(csvPath);

        List<KEvaluation> evaluations = new ArrayList<>();
        for (int k = minK; k <= maxK; k++) {
            ClusteringResult result = runKMeans(prepared, k, iterations, seed + k, false);
            double smallestRatio = smallestClusterRatio(result.getClusterIds());
            double penalty = smallestRatio < minClusterRatio ? (minClusterRatio - smallestRatio) * 10.0 : 0.0;
            double score = result.getDaviesBouldinIndex() + penalty;

            evaluations.add(new KEvaluation(
                    k,
                    result,
                    smallestRatio,
                    penalty,
                    score
            ));
        }

        evaluations.sort(Comparator.comparingDouble(KEvaluation::getScore));
        KEvaluation best = evaluations.get(0);
        saveKEvaluationReport(evaluations, evalReportPath);

        log.info("Auto-K 선택 완료: k={}, DBI={}, CH={}, smallestRatio={}%",
                best.getK(),
                String.format("%.4f", best.getResult().getDaviesBouldinIndex()),
                String.format("%.2f", best.getResult().getCalinskiHarabaszIndex()),
                String.format("%.3f", best.getSmallestRatio() * 100.0));

        logClusterStats(best.getResult().getClusterIds(), prepared.getRawVectors(), best.getResult().getDaviesBouldinIndex());
        return best.getResult();
    }

    @Getter
    private static class KEvaluation {
        private final int k;
        private final ClusteringResult result;
        private final double smallestRatio;
        private final double penalty;
        private final double score;

        private KEvaluation(int k, ClusteringResult result, double smallestRatio, double penalty, double score) {
            this.k = k;
            this.result = result;
            this.smallestRatio = smallestRatio;
            this.penalty = penalty;
            this.score = score;
        }
    }

    private ClusteringResult runKMeans(
            TribuoClusteringDataConverterV1.PreparedClusteringData prepared,
            int k,
            int iterations,
            long seed,
            boolean withLogs
    ) {
        KMeansTrainer trainer = new KMeansTrainer(
                k,
                iterations,
                KMeansTrainer.Distance.EUCLIDEAN,
                KMeansTrainer.Initialisation.PLUSPLUS,
                1,
                seed
        );

        Model<ClusterID> model = trainer.train(prepared.getDataset());
        List<Integer> clusterIds = new ArrayList<>(prepared.getDataset().size());

        for (int i = 0; i < prepared.getDataset().size(); i++) {
            Prediction<ClusterID> prediction = model.predict(prepared.getDataset().getExample(i));
            clusterIds.add(prediction.getOutput().getID());
        }

        double dbi = calculateDaviesBouldinIndex(prepared.getNormalizedVectors(), clusterIds);
        double chi = calculateCalinskiHarabaszIndex(prepared.getNormalizedVectors(), clusterIds, k);

        if (withLogs) {
            logClusterStats(clusterIds, prepared.getRawVectors(), dbi);
        }

        return new ClusteringResult(model, prepared.getCardIds(), clusterIds, dbi, chi, k);
    }

    public void saveModel(Model<ClusterID> model, String modelPath) throws Exception {
        File modelFile = new File(modelPath);
        if (modelFile.getParentFile() != null && !modelFile.getParentFile().exists()) {
            modelFile.getParentFile().mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            oos.writeObject(model);
        }
        log.info("클러스터링 모델 저장 완료: {}", modelFile.getAbsolutePath());
    }

    public void saveAssignments(ClusteringResult result, String assignmentPath) throws Exception {
        Path path = Path.of(assignmentPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();
        lines.add("card_id,cluster_id");
        for (int i = 0; i < result.getCardIds().size(); i++) {
            lines.add(result.getCardIds().get(i) + "," + result.getClusterIds().get(i));
        }

        Files.write(path, lines);
        log.info("클러스터 할당 결과 저장 완료: {}", path);
    }

    public void saveClusterProfileReport(ClusteringResult result, List<double[]> rawVectors, String reportPath) throws Exception {
        Path path = Path.of(reportPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Map<Integer, double[]> sums = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < result.getClusterIds().size(); i++) {
            int clusterId = result.getClusterIds().get(i);
            double[] vector = rawVectors.get(i);
            sums.computeIfAbsent(clusterId, key -> new double[vector.length]);
            double[] acc = sums.get(clusterId);
            for (int j = 0; j < vector.length; j++) {
                acc[j] += vector[j];
            }
            counts.merge(clusterId, 1, Integer::sum);
        }

        List<String> lines = new ArrayList<>();
        lines.add("k,cluster_id,size,ratio,total_amount,tx_count,avg_installments,max_amount,avg_amount,authorized_ratio");
        for (Map.Entry<Integer, double[]> entry : sums.entrySet()) {
            int clusterId = entry.getKey();
            int size = counts.get(clusterId);
            double ratio = (double) size / result.getClusterIds().size();
            double[] mean = new double[entry.getValue().length];
            for (int i = 0; i < mean.length; i++) {
                mean[i] = entry.getValue()[i] / size;
            }
            lines.add(String.join(",",
                    Integer.toString(result.getK()),
                    Integer.toString(clusterId),
                    Integer.toString(size),
                    String.format("%.6f", ratio),
                    String.format("%.6f", mean[0]),
                    String.format("%.6f", mean[1]),
                    String.format("%.6f", mean[2]),
                    String.format("%.6f", mean[3]),
                    String.format("%.6f", mean[4]),
                    String.format("%.6f", mean[5])
            ));
        }
        Files.write(path, lines);
        log.info("클러스터 프로파일 리포트 저장 완료: {}", path);
    }

    private void logClusterStats(List<Integer> clusterIds, List<double[]> rawVectors, double dbi) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int clusterId : clusterIds) {
            counts.merge(clusterId, 1, Integer::sum);
        }
        log.info("클러스터 분포: {}", counts);

        double total = clusterIds.size();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            double ratio = entry.getValue() / total;
            if (ratio < 0.01) {
                log.warn("소형 클러스터 감지 - clusterId={}, size={}, ratio={}%",
                        entry.getKey(), entry.getValue(), String.format("%.3f", ratio * 100.0));
            }
        }

        logClusterProfiles(clusterIds, rawVectors);

        if (Double.isNaN(dbi)) {
            log.info("Davies-Bouldin Index 계산 불가 (활성 클러스터 수 부족)");
        } else {
            log.info("Davies-Bouldin Index: {}", String.format("%.4f", dbi));
        }
    }

    private void logClusterProfiles(List<Integer> clusterIds, List<double[]> rawVectors) {
        Map<Integer, double[]> sums = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();

        for (int i = 0; i < clusterIds.size(); i++) {
            int clusterId = clusterIds.get(i);
            double[] vector = rawVectors.get(i);
            sums.computeIfAbsent(clusterId, key -> new double[vector.length]);
            double[] acc = sums.get(clusterId);
            for (int j = 0; j < vector.length; j++) {
                acc[j] += vector[j];
            }
            counts.merge(clusterId, 1, Integer::sum);
        }

        for (Map.Entry<Integer, double[]> entry : sums.entrySet()) {
            int clusterId = entry.getKey();
            double[] mean = new double[entry.getValue().length];
            for (int i = 0; i < mean.length; i++) {
                mean[i] = entry.getValue()[i] / counts.get(clusterId);
            }
            log.info("Cluster {} 평균 프로파일 => total_amount={}, tx_count={}, avg_installments={}, max_amount={}, avg_amount={}, authorized_ratio={}",
                    clusterId,
                    String.format("%.2f", mean[0]),
                    String.format("%.2f", mean[1]),
                    String.format("%.2f", mean[2]),
                    String.format("%.2f", mean[3]),
                    String.format("%.2f", mean[4]),
                    String.format("%.4f", mean[5]));
        }
    }

    private double calculateDaviesBouldinIndex(List<double[]> vectors, List<Integer> clusterIds) {
        Map<Integer, List<double[]>> grouped = new HashMap<>();
        for (int i = 0; i < vectors.size(); i++) {
            grouped.computeIfAbsent(clusterIds.get(i), key -> new ArrayList<>()).add(vectors.get(i));
        }

        List<Integer> activeClusters = new ArrayList<>(grouped.keySet());
        if (activeClusters.size() < 2) {
            return Double.NaN;
        }

        Map<Integer, double[]> centroids = new HashMap<>();
        Map<Integer, Double> scatters = new HashMap<>();

        for (int clusterId : activeClusters) {
            List<double[]> rows = grouped.get(clusterId);
            double[] centroid = mean(rows);
            centroids.put(clusterId, centroid);

            double scatter = 0.0;
            for (double[] row : rows) {
                scatter += euclidean(row, centroid);
            }
            scatters.put(clusterId, scatter / rows.size());
        }

        double dbiSum = 0.0;
        for (int i : activeClusters) {
            double maxR = Double.NEGATIVE_INFINITY;
            for (int j : activeClusters) {
                if (i == j) {
                    continue;
                }
                double centroidDistance = euclidean(centroids.get(i), centroids.get(j));
                if (centroidDistance == 0.0) {
                    continue;
                }
                double rij = (scatters.get(i) + scatters.get(j)) / centroidDistance;
                if (rij > maxR) {
                    maxR = rij;
                }
            }
            if (maxR > Double.NEGATIVE_INFINITY) {
                dbiSum += maxR;
            }
        }
        return dbiSum / activeClusters.size();
    }

    private double calculateCalinskiHarabaszIndex(List<double[]> vectors, List<Integer> clusterIds, int k) {
        int n = vectors.size();
        if (n <= k || k <= 1) {
            return Double.NaN;
        }

        double[] globalCentroid = mean(vectors);
        Map<Integer, List<double[]>> grouped = new HashMap<>();
        for (int i = 0; i < n; i++) {
            grouped.computeIfAbsent(clusterIds.get(i), key -> new ArrayList<>()).add(vectors.get(i));
        }
        if (grouped.size() <= 1) {
            return Double.NaN;
        }

        double between = 0.0;
        double within = 0.0;

        for (Map.Entry<Integer, List<double[]>> entry : grouped.entrySet()) {
            List<double[]> cluster = entry.getValue();
            double[] centroid = mean(cluster);
            between += cluster.size() * squaredDistance(centroid, globalCentroid);
            for (double[] row : cluster) {
                within += squaredDistance(row, centroid);
            }
        }

        if (within == 0.0) {
            return Double.NaN;
        }
        return (between / (grouped.size() - 1)) / (within / (n - grouped.size()));
    }

    private double[] mean(List<double[]> vectors) {
        int dim = vectors.get(0).length;
        double[] mean = new double[dim];
        for (double[] vector : vectors) {
            for (int i = 0; i < dim; i++) {
                mean[i] += vector[i];
            }
        }
        for (int i = 0; i < dim; i++) {
            mean[i] /= vectors.size();
        }
        return mean;
    }

    private double euclidean(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double squaredDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    private double smallestClusterRatio(List<Integer> clusterIds) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int clusterId : clusterIds) {
            counts.merge(clusterId, 1, Integer::sum);
        }
        int min = Integer.MAX_VALUE;
        for (int count : counts.values()) {
            min = Math.min(min, count);
        }
        return (double) min / clusterIds.size();
    }

    private void saveKEvaluationReport(List<KEvaluation> evaluations, String pathStr) throws Exception {
        Path path = Path.of(pathStr);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<String> lines = new ArrayList<>();
        lines.add("k,dbi,chi,smallest_cluster_ratio,penalty,score");
        for (KEvaluation e : evaluations) {
            lines.add(String.join(",",
                    Integer.toString(e.getK()),
                    String.format("%.6f", e.getResult().getDaviesBouldinIndex()),
                    String.format("%.6f", e.getResult().getCalinskiHarabaszIndex()),
                    String.format("%.6f", e.getSmallestRatio()),
                    String.format("%.6f", e.getPenalty()),
                    String.format("%.6f", e.getScore())
            ));
        }
        Files.write(path, lines);
        log.info("k 후보 평가 리포트 저장 완료: {}", path);
    }
}
