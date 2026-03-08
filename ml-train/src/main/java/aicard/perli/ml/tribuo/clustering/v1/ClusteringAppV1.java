package aicard.perli.ml.tribuo.clustering.v1;

import aicard.perli.ml.tribuo.service.v1.TribuoClusteringServiceV1;
import aicard.perli.ml.tribuo.util.v1.TribuoClusteringDataConverterV1;
import lombok.extern.slf4j.Slf4j;

/**
 * Tribuo K-Means 소비 패턴 클러스터링 실행 애플리케이션.
 */
@Slf4j
public class ClusteringAppV1 {

    public static void main(String[] args) {
        String csvPath = "resources/processed/ranking_features.csv";
        String modelPath = "resources/output/models/tribuo/v1/tribuo_kmeans_v1.gdpc";
        String assignmentPath = "resources/output/models/tribuo/v1/cluster_assignments_v1.csv";
        String evalPath = "resources/output/models/tribuo/v1/cluster_k_evaluation_v1.csv";
        String profilePath = "resources/output/models/tribuo/v1/cluster_profiles_v1.csv";

        int minK = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int maxK = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        int maxIterations = 100;
        long seed = 777L;
        double minClusterRatio = 0.01;

        TribuoClusteringDataConverterV1 converter = new TribuoClusteringDataConverterV1();
        TribuoClusteringServiceV1 service = new TribuoClusteringServiceV1(converter);

        try {
            log.info("Auto-K 클러스터링 시작 (kRange={}..{}, maxIterations={})", minK, maxK, maxIterations);
            TribuoClusteringServiceV1.ClusteringResult result = service.clusterAutoK(
                    csvPath, minK, maxK, maxIterations, seed, minClusterRatio, evalPath
            );
            service.saveModel(result.getModel(), modelPath);
            service.saveAssignments(result, assignmentPath);
            service.saveClusterProfileReport(result, converter.loadDataset(csvPath).getRawVectors(), profilePath);
            log.info("클러스터링 완료. chosenK={}, DBI={}, CH={}",
                    result.getK(),
                    String.format("%.4f", result.getDaviesBouldinIndex()),
                    String.format("%.2f", result.getCalinskiHarabaszIndex()));
        } catch (Exception e) {
            log.error("클러스터링 파이프라인 실패", e);
            System.exit(1);
        }
    }
}
