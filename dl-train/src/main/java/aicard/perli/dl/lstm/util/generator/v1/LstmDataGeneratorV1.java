package aicard.perli.dl.lstm.util.generator.v1;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * LSTM 학습에 필요한 시계열, 통계, 소비 성향 데이터를 하나의 데이터셋으로 결합하는 데이터 제너레이터입니다.
 * <p>
 * 서로 다른 데이터 소스 간의 ID 불일치를 해결하기 위해 인덱스 기반의 순차적 매칭 방식을 채택하였으며,
 * 시계열 흐름(UCI)에 분석 지표(Advanced)와 소비 패턴(CC General)을 융합하여 최종 학습용 CSV를 생성합니다.
 * </p>
 */
@Slf4j
public class LstmDataGeneratorV1 {

    /**
     * 데이터 통합 프로세스를 실행하는 메인 메서드입니다.
     * <p>
     * 각 원천 CSV 파일로부터 필요한 컬럼을 추출하여 메모리에 로드한 뒤,
     * UCI 신용 데이터의 행을 기준으로 다른 데이터셋의 피처를 순환 매칭하여 결합합니다.
     * 이 과정을 통해 모델은 단순 지출액 외에도 신용 상태와 소비 성향이 결합된 고차원 데이터를 학습하게 됩니다.
     * </p>
     * @param args 실행 인자
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

            // 분석 피처 리스트 로드 (통계 4종 및 소비성향 2종 추출)
            List<String> advancedList = loadList(advancedCsv, 4);
            List<String> ccList = loadList(ccCsv, 2);

            try (BufferedReader brUci = new BufferedReader(new FileReader(uciCsv));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPath))) {

                // 헤더 통합 정의
                String uciHeader = brUci.readLine();
                String extraHeader = ",total_amount,tx_count,avg_installments,authorized_ratio,BALANCE,PURCHASES";
                bw.write(uciHeader + extraHeader);
                bw.newLine();

                String line;
                int count = 0;
                while ((line = brUci.readLine()) != null) {
                    // 순차적 매칭(Sequential Matching): 모듈로 연산을 통해 데이터셋 간 크기 차이를 극복하고 피처 주입
                    String advPart = advancedList.get(count % advancedList.size());
                    String ccPart = ccList.get(count % ccList.size());

                    bw.write(line + "," + advPart + "," + ccPart);
                    bw.newLine();
                    count++;
                }

                log.info("데이터셋 통합 완료: {} (총 {}건 생성)", targetPath, count);
            }
        } catch (IOException e) {
            log.error("데이터 통합 중 파일 입출력 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    /**
     * CSV 파일에서 분석에 필요한 특정 컬럼들만 선별적으로 추출하여 리스트로 반환합니다.
     * <p>
     * 타입 4: Advanced 데이터셋에서 총액, 거래건수, 할부, 승인율 추출 <br>
     * 타입 2: CC General 데이터셋에서 잔액, 구매액 추출
     * </p>
     * @param path 읽어올 CSV 파일 경로
     * @param type 데이터 추출 규격 타입 (4 또는 2)
     * @return 추출된 피처 문자열 리스트
     * @throws IOException 파일 읽기 실패 시 발생
     */
    private static List<String> loadList(String path, int type) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 데이터 건너뛰기
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (type == 4) {
                    // Advanced Features: total_amount, tx_count, avg_installments, authorized_ratio
                    list.add(String.format("%s,%s,%s,%s", v[1], v[2], v[3], v[6]));
                } else {
                    // CC General Features: BALANCE, PURCHASES
                    list.add(String.format("%s,%s", v[1], v[3]));
                }
            }
        }
        return list;
    }
}