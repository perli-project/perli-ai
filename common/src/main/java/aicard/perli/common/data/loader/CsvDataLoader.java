package aicard.perli.common.data.loader;

import aicard.perli.common.data.parser.FinEventParser;
import aicard.perli.common.model.FinEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 대용량 금융 데이터를 로드하고 공용 모델을 거쳐 학습용 피처셋을 생성하는 로더 클래스입니다.
 * <p>스트리밍 방식을 사용하여 메모리 점유를 최소화하며, 카드별 소비 패턴(합계, 최대, 평균, 승인율)을 집계합니다.</p>
 */
@RequiredArgsConstructor
public class CsvDataLoader {

    private final FinEventParser eventParser;

    /**
     * 원본 데이터를 읽어 공용 객체로 정제한 후, 고도화된 피처셋을 파일로 저장합니다.
     *
     * @param trainPath   타겟 데이터 경로 (train.csv)
     * @param historyPath 원본 거래 내역 경로 (historical_transactions.csv)
     * @param outputPath  가공된 결과 저장 경로 (train_features_advanced.csv)
     */
    public void aggregateAndSave(String trainPath, String historyPath, String outputPath) {
        Map<String, Double> targetMap = loadTargetMap(trainPath);
        Map<String, AggregatedFeature> featureMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(historyPath, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(br);

            for (CSVRecord record : records) {
                // 공용 모델(FinEvent)을 거쳐 데이터의 규격과 정합성 확보
                FinEvent event = eventParser.parse(record, targetMap);

                if (event != null) {
                    AggregatedFeature feat = featureMap.computeIfAbsent(event.getCardId(), k -> new AggregatedFeature());
                    feat.addTransaction(event.getAmount(), event.getInstallments(), event.isAuthorized());
                }
            }
            saveToProcessedCsv(featureMap, targetMap, outputPath);
        } catch (Exception e) {
            throw new RuntimeException("피처 가공 프로세스 중 오류 발생", e);
        }
    }

    /**
     * 집계된 통계 맵을 최종 CSV 파일로 출력합니다.
     */
    private void saveToProcessedCsv(Map<String, AggregatedFeature> features, Map<String, Double> targets, String outputPath) throws IOException {
        File file = new File(outputPath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        String[] headers = {"card_id", "total_amount", "tx_count", "avg_installments", "max_amount", "avg_amount", "authorized_ratio", "target"};

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {

            for (Map.Entry<String, AggregatedFeature> entry : features.entrySet()) {
                String cardId = entry.getKey();
                AggregatedFeature f = entry.getValue();
                csvPrinter.printRecord(cardId, f.totalSum, f.count, (double)f.totalInst/f.count, f.maxVal, f.totalSum/f.count, (double)f.authCount/f.count, targets.getOrDefault(cardId, 0.0));
            }
        }
    }

    /**
     * 학습용 정답지 데이터를 로드합니다.
     */
    private Map<String, Double> loadTargetMap(String trainPath) {
        Map<String, Double> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(trainPath, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(br);
            for (CSVRecord record : records) { map.put(record.get("card_id"), Double.parseDouble(record.get("target"))); }
        } catch (Exception e) { throw new RuntimeException("타겟 맵 로드 실패", e); }
        return map;
    }

    /**
     * 카드별 소비 패턴을 집계하기 위한 내부 통계 컨테이너 클래스입니다.
     */
    private static class AggregatedFeature {
        double totalSum = 0; double maxVal = -Double.MAX_VALUE; int count = 0; int totalInst = 0; int authCount = 0;
        void addTransaction(double amt, int inst, boolean auth) {
            totalSum += amt; if (amt > maxVal) maxVal = amt; count++; totalInst += inst; if (auth) authCount++;
        }
    }
}