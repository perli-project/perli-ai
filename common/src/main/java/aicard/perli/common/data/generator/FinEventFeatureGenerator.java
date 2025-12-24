package aicard.perli.common.data.generator;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;

/**
 * <p>FinEvent 표준 모델 기반 기본 피처 생성기</p>
 * <p>FinEventParser와 CsvDataLoader를 사용하여 원본 데이터를 분석하고,
 * 시스템의 가장 기초가 되는 'train_features_advanced.csv'를 생성합니다.</p>
 */
public class FinEventFeatureGenerator {

    public static void main(String[] args) {

        String rawDir = "C:/Coding/perli-ai/resources/raw/";
        String processedDir = "C:/Coding/perli-ai/resources/processed/";

        String trainPath = rawDir + "train.csv";
        String historyPath = rawDir + "historical_transactions.csv";
        String outputPath = processedDir + "train_features_advanced.csv";

        System.out.println("FinEvent 기반 표준 피처 추출 시작");

        try {
            // FinEvent 모델 규격을 따르는 파서 및 로더 준비
            FinEventParser eventParser = new FinEventParser();
            CsvDataLoader dataLoader = new CsvDataLoader(eventParser);

            // 집계 및 파일 쓰기 실행
            long startTime = System.currentTimeMillis();

            dataLoader.aggregateAndSave(trainPath, historyPath, outputPath);

            long endTime = System.currentTimeMillis();

            // 3. 결과 보고
            System.out.println("표준 피처 생성 완료!");
            System.out.println("파일 경로: " + outputPath);
            System.out.println("소요 시간: " + (endTime - startTime) / 1000.0 + "초");

        } catch (Exception e) {
            System.err.println("피처 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}