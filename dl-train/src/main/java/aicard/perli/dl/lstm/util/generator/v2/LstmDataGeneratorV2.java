package aicard.perli.dl.lstm.util.generator.v2;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 고도화된 LSTM V2 학습용 통합 데이터셋 생성 유틸리티.
 * UCI, Advanced, CC 데이터를 결합하여 'train_lstm_v2.csv'를 생성함.
 */
@Slf4j
public class LstmDataGeneratorV2 {

    public static void main(String[] args) {
        String basePath = "C:/Coding/perli-ai/resources/processed";
        String advancedCsv = basePath + "/train_features_advanced.csv";
        String uciCsv = "C:/Coding/perli-ai/resources/raw/UCI_Credit_Card.csv";
        String ccCsv = "C:/Coding/perli-ai/resources/raw/CC GENERAL.csv";
        String targetPath = basePath + "/lstm/v2/train_lstm_v2.csv";

        try {
            Path targetDir = Paths.get(basePath + "/lstm/v2");
            if (Files.notExists(targetDir)) Files.createDirectories(targetDir);

            // 피처 리스트 로드 (Advanced: 통계 4종, CC: 성향 2종)
            List<String> advancedList = loadList(advancedCsv, 4);
            List<String> ccList = loadList(ccCsv, 2);

            try (BufferedReader brUci = new BufferedReader(new FileReader(uciCsv));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPath))) {

                // 헤더 작성 (UCI 기본 헤더 + 확장 피처 헤더)
                String uciHeader = brUci.readLine();
                // "total_amount,tx_count,avg_installments,authorized_ratio,BALANCE,PURCHASES"
                String extraHeader = ",total_amount,tx_count,avg_installments,authorized_ratio,BALANCE,PURCHASES";

                bw.write(uciHeader + extraHeader);
                bw.newLine();

                String line;
                int count = 0;
                while ((line = brUci.readLine()) != null) {
                    // 순차 결합 (Modulo 연산 사용)
                    String advPart = advancedList.get(count % advancedList.size());
                    String ccPart = ccList.get(count % ccList.size());

                    // 최종 라인 결합 (V2 데이터 라인 생성)
                    bw.write(line + "," + advPart + "," + ccPart);
                    bw.newLine();
                    count++;
                }

                log.info("==== V2 데이터 통합 완료 ====");
                log.info("저장 경로: " + targetPath);
                log.info("총 데이터 건수: " + count + "건");
            }
        } catch (IOException e) {
            log.error("V2 데이터 통합 중 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * CSV에서 필요한 필드 추출 로직
     */
    private static List<String> loadList(String path, int type) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                // CSV 파싱 시 쉼표 처리 및 안전한 인덱스 접근
                if (type == 4) {
                    // total_amount, tx_count, avg_installments, authorized_ratio
                    list.add(String.format("%s,%s,%s,%s", v[1], v[2], v[3], v[6]));
                } else {
                    // BALANCE, PURCHASES
                    list.add(String.format("%s,%s", v[1], v[3]));
                }
            }
        }
        return list;
    }
}