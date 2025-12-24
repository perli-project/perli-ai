package aicard.perli.ml.h2o.util.v2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * <p>H2O 업리프트 모델(Uplift Modeling) 전용 데이터 생성기 V2</p>
 * <p>본 클래스는 오직 다음 3가지 데이터만을 결합하여 업리프트 학습용 피처를 생성합니다:
 * 1. <b>train.csv</b>: 카드별 기본 속성(f1, f2, f3) 및 결과값(target) 제공
 * 2. <b>merchants.csv</b>: 가맹점 마스터 정보 (매출 등급 등 속성 추출용)
 * 3. <b>new_merchant_transactions.csv</b>: 최신 거래 내역 (행동 데이터 추출용)
 * </p>
 * <p>V2 고도화 내용:
 * - 가맹점 매출 규모(sales_range)를 반영한 '프리미엄 소비 비중' 피처 추가
 * - S-Learner 학습을 위한 가상 처치 변수(is_recommended) 생성
 * </p>
 */
public class EloUpliftDataGeneratorV2 {

    public static void main(String[] args) {

        String inputDir = "C:/Coding/perli-ai/resources/raw/";
        String outputDir = "C:/Coding/perli-ai/resources/processed/uplift/";

        String trainFile = inputDir + "train.csv";
        String merchantFile = inputDir + "merchants.csv";
        String newTransFile = inputDir + "new_merchant_transactions.csv";
        String resultFile = outputDir + "train_uplift_v2.csv";

        try {
            Files.createDirectories(Paths.get(outputDir));

            // 가맹점 정보 로드 (ID -> 매출 등급)
            Map<String, String> merchantSalesMap = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(merchantFile))) {
                String header = br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    // 따옴표 제거 후 분리
                    String[] cols = line.replace("\"", "").split(",");
                    if (cols.length > 7) {
                        merchantSalesMap.put(cols[0], cols[7]);
                    }
                }
            }
            System.out.println("가맹점 매출 데이터 인덱싱 완료");

            // 카드별 최신 소비 행태 집계
            Map<String, CardUpliftStats> statsMap = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(newTransFile))) {
                String header = br.readLine().replace("\"", "");
                List<String> headerList = Arrays.asList(header.split(","));

                int cardIdx = headerList.indexOf("card_id");
                int mIdx = headerList.indexOf("merchant_id");
                int amtIdx = headerList.indexOf("purchase_amount");

                String line;
                while ((line = br.readLine()) != null) {
                    // 데이터 행에서도 따옴표 제거
                    String[] cols = line.replace("\"", "").split(",");
                    try {
                        String cardId = cols[cardIdx];
                        String merchantId = cols[mIdx];
                        String amtStr = cols[amtIdx];

                        double amount = Double.parseDouble(amtStr);

                        CardUpliftStats s = statsMap.getOrDefault(cardId, new CardUpliftStats());
                        s.txCount++;
                        s.totalAmt += amount;

                        if ("A".equals(merchantSalesMap.get(merchantId))) {
                            s.premiumCount++;
                        }
                        statsMap.put(cardId, s);
                    } catch (Exception e) {
                        // 숫자 변환 실패 시 해당 라인만 스킵
                        continue;
                    }
                }
            }
            System.out.println("신규 거래 기반 소비 패턴 집계 완료");

            // 최종 업리프트 데이터셋 생성
            try (BufferedReader br = new BufferedReader(new FileReader(trainFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile))) {

                String header = br.readLine();
                bw.write(header + ",new_tx_count,new_total_amt,premium_ratio,is_recommended");
                bw.newLine();

                Random rand = new Random();
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split(",");
                    String cardId = cols[1];

                    CardUpliftStats s = statsMap.getOrDefault(cardId, new CardUpliftStats());
                    int isRecommended = rand.nextInt(2); // 0 또는 1 (S-Learner 필수 변수)
                    double premiumRatio = s.txCount > 0 ? (double) s.premiumCount / s.txCount : 0;

                    bw.write(String.format("%s,%d,%.4f,%.4f,%d",
                            line, s.txCount, s.totalAmt, premiumRatio, isRecommended));
                    bw.newLine();
                    count++;
                }
                System.out.println("train_uplift_v2.csv 생성 완료 (총 " + count + "건)");
            }

        } catch (Exception e) {
            System.err.println("데이터 생성 중 오류: " + e.getMessage());
        }
    }

    /** 업리프트 학습용 통계 객체 */
    static class CardUpliftStats {
        int txCount = 0;
        double totalAmt = 0.0;
        int premiumCount = 0;
    }
}
