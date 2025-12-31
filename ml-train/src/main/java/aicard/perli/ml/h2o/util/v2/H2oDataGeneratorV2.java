package aicard.perli.ml.h2o.util.v2;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * H2O 업리프트 모델(Uplift Modeling) 전용 데이터 생성기 V2.
 * <p>
 * 본 클래스는 다음 3가지 핵심 원천 데이터를 결합하여 고도화된 업리프트 학습용 피처를 생성합니다:
 * <ul>
 * <li>train.csv: 카드별 기본 속성 및 결과값(target) 제공</li>
 * <li>merchants.csv: 가맹점 마스터 정보를 활용한 매출 등급(Sales Range) 속성 추출</li>
 * <li>new_merchant_transactions.csv: 최신 거래 내역 분석을 통한 실시간 행동 데이터 산출</li>
 * </ul>
 * </p>
 * <p>주요 고도화 사항:
 * <ul>
 * <li>가맹점 매출 규모를 반영한 '프리미엄 소비 비중(premium_ratio)' 피처 동적 생성</li>
 * <li>S-Learner 및 UpliftDRF 학습을 위한 가상 처치 변수(is_recommended) 무작위 할당</li>
 * </ul>
 * </p>
 */
@Slf4j
public class H2oDataGeneratorV2 {

    /**
     * 고도화된 데이터 통합 및 피처 엔지니어링 프로세스를 실행하는 메인 메서드입니다.
     * <p>
     * 가맹점 정보 인덱싱, 카드별 소비 패턴 집계, 최종 데이터셋 생성으로 이어지는 파이프라인을 관장하며,
     * 단순 통계를 넘어 사용자의 '소비 성향(Consumption Propensity)'이 투영된 학습 데이터를 구축합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {

        String inputDir = "C:/Coding/perli-ai/resources/raw/";
        String outputDir = "C:/Coding/perli-ai/resources/processed/uplift/";

        String trainFile = inputDir + "train.csv";
        String merchantFile = inputDir + "merchants.csv";
        String newTransFile = inputDir + "new_merchant_transactions.csv";
        String resultFile = outputDir + "train_uplift_v2.csv";

        try {
            Files.createDirectories(Paths.get(outputDir));

            // 가맹점의 매출 규모(sales_range)를 인덱싱하여 향후 프리미엄 소비 여부 판단의 기준으로 활용합니다.
            Map<String, String> merchantSalesMap = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(merchantFile))) {
                String header = br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    // CSV 데이터의 따옴표 제거 후 컬럼 분리
                    String[] cols = line.replace("\"", "").split(",");
                    if (cols.length > 7) {
                        merchantSalesMap.put(cols[0], cols[7]);
                    }
                }
            }

            log.info("가맹점 매출 데이터 인덱싱 완료");

            // 최신 거래 내역을 순회하며 각 카드별 총 거래액, 건수 및 프리미엄 가맹점 이용 빈도를 산출합니다.
            Map<String, CardUpliftStats> statsMap = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(newTransFile))) {
                String header = br.readLine().replace("\"", "");
                List<String> headerList = Arrays.asList(header.split(","));

                int cardIdx = headerList.indexOf("card_id");
                int mIdx = headerList.indexOf("merchant_id");
                int amtIdx = headerList.indexOf("purchase_amount");

                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.replace("\"", "").split(",");
                    try {
                        String cardId = cols[cardIdx];
                        String merchantId = cols[mIdx];
                        String amtStr = cols[amtIdx];

                        double amount = Double.parseDouble(amtStr);

                        CardUpliftStats s = statsMap.getOrDefault(cardId, new CardUpliftStats());
                        s.txCount++;
                        s.totalAmt += amount;

                        // 가맹점 매출 등급이 'A'(최상위)인 경우 프리미엄 소비로 카운트
                        if ("A".equals(merchantSalesMap.get(merchantId))) {
                            s.premiumCount++;
                        }
                        statsMap.put(cardId, s);
                    } catch (Exception e) {
                        // 수치 변환 실패 등 예외 데이터 발생 시 해당 라인 스킵
                        continue;
                    }
                }
            }

            log.info("신규 거래 기반 소비 패턴 집계 완료");

            // 기초 데이터(train)에 위에서 산출된 고도화 행동 피처와 처치 변수를 결합하여 최종 학습용 CSV를 작성합니다.
            try (BufferedReader br = new BufferedReader(new FileReader(trainFile));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile))) {

                String header = br.readLine();
                // 수혈된 신규 피처 헤더 추가 (건수, 금액, 프리미엄 비중, 처치 여부)
                bw.write(header + ",new_tx_count,new_total_amt,premium_ratio,is_recommended");
                bw.newLine();

                Random rand = new Random();
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split(",");
                    String cardId = cols[1];

                    CardUpliftStats s = statsMap.getOrDefault(cardId, new CardUpliftStats());
                    int isRecommended = rand.nextInt(2); // S-Learner 학습을 위한 랜덤 배정 (0 또는 1)
                    double premiumRatio = s.txCount > 0 ? (double) s.premiumCount / s.txCount : 0;

                    // 데이터 바인딩 및 파일 기록
                    bw.write(String.format("%s,%d,%.4f,%.4f,%d",
                            line, s.txCount, s.totalAmt, premiumRatio, isRecommended));
                    bw.newLine();
                    count++;
                }

                log.info("train_uplift_v2.csv 생성 완료 (총 " + count + "건)");
            }

        } catch (Exception e) {
            log.error("데이터 생성 중 오류 발생: " + e.getMessage());
        }
    }

    /** 카드별 업리프트 학습용 통계 데이터를 임시 보관하는 정적 내부 클래스입니다.
     */
    static class CardUpliftStats {
        /** 최근 총 거래 건수 */
        int txCount = 0;
        /** 최근 총 결제 금액 */
        double totalAmt = 0.0;
        /** 프리미엄 가맹점 이용 건수 */
        int premiumCount = 0;
    }
}