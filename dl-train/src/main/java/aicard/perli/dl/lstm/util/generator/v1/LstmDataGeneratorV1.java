package aicard.perli.dl.lstm.util.generator.v1;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * LSTM 학습을 위해 UCI(시계열), Advanced(통계), CC(성향) 데이터를 통합하는 유틸리티.
 * ID 불일치 문제를 해결하기 위해 인덱스 기반의 순차적 매칭(Sequential Matching) 방식을 사용함.
 */
@Slf4j
public class LstmDataGeneratorV1 {

    /**
     * 메인 실행 메서드. 3개 소스 데이터를 결합하여 'train_lstm_v1.csv'를 생성함.
     */
    public static void main(String[] args) {
        String basePath = "C:/Coding/perli-ai/resources/processed";
        String advancedCsv = basePath + "/train_features_advanced.csv";
        String uciCsv = "C:/Coding/perli-ai/resources/raw/UCI_Credit_Card.csv";
        String ccCsv = "C:/Coding/perli-ai/resources/raw/CC GENERAL.csv";

        String targetPath = basePath + "/lstm/v1/train_lstm_v1.csv";

        try {
            Path targetDir = Paths.get(basePath + "/lstm/v1");
            if (Files.notExists(targetDir)) Files.createDirectories(targetDir);

            // 피처 리스트 로드 (순차 결합용)
            List<String> advancedList = loadList(advancedCsv, 4);
            List<String> ccList = loadList(ccCsv, 2);

            try (BufferedReader brUci = new BufferedReader(new FileReader(uciCsv));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPath))) {

                // 헤더 정의
                String uciHeader = brUci.readLine();
                String extraHeader = ",total_amount,tx_count,avg_installments,authorized_ratio,BALANCE,PURCHASES";
                bw.write(uciHeader + extraHeader);
                bw.newLine();

                String line;
                int count = 0;
                while ((line = brUci.readLine()) != null) {
                    // 데이터 개수 차이를 보완하기 위해 Modulo 연산으로 순환 매칭
                    String advPart = advancedList.get(count % advancedList.size());
                    String ccPart = ccList.get(count % ccList.size());

                    bw.write(line + "," + advPart + "," + ccPart);
                    bw.newLine();
                    count++;
                }

                log.info("통합 완료: " + targetPath + " (총 " + count + "건)");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * CSV에서 필요한 컬럼만 추출하여 문자열 리스트로 반환.
     * @param type 4: Advanced(통계 4종), 2: CC General(잔액/구매액 2종)
     */
    private static List<String> loadList(String path, int type) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
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