package aicard.perli.dl.lstm.util.generator.v2;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 고도화된 LSTM V2 학습용 통합 데이터셋 생성 유틸리티 클래스입니다.
 * <p>
 * UCI 신용 데이터(인구통계 및 시계열), Advanced 분석 데이터(통계 지표), CC General 데이터(소비 성향)를
 * 하나로 결합하여 16개 이상의 피처를 보유한 'train_lstm_v2.csv' 결과물을 생성합니다.
 * </p>
 */
@Slf4j
public class LstmDataGeneratorV2 {

    /**
     * V2 통합 데이터셋 생성을 위한 메인 실행 프로세스입니다.
     * <p>
     * 각 원천 CSV 파일에서 필요한 속성들을 추출하여 메모리에 적재한 뒤,
     * Modulo 연산을 활용한 순차적 매칭(Sequential Matching) 기법을 통해
     * 대규모 통합 학습 데이터를 물리 파일로 출력합니다.
     * 이를 통해 인구통계와 금융 시퀀스가 결합된 고해상도 학습 환경을 구축합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        String basePath = "C:/Coding/perli-ai/resources/processed";
        String advancedCsv = basePath + "/train_features_advanced.csv";
        String uciCsv = "C:/Coding/perli-ai/resources/raw/UCI_Credit_Card.csv";
        String ccCsv = "C:/Coding/perli-ai/resources/raw/CC GENERAL.csv";
        String targetPath = basePath + "/lstm/v2/train_lstm_v2.csv";

        try {
            // 결과 파일 저장을 위한 디렉토리 자동 생성
            Path targetDir = Paths.get(basePath + "/lstm/v2");
            if (Files.notExists(targetDir)) Files.createDirectories(targetDir);

            // 분석 피처 리스트 로드 (Advanced: 통계 4종, CC: 성향 2종 추출)
            List<String> advancedList = loadList(advancedCsv, 4);
            List<String> ccList = loadList(ccCsv, 2);

            try (BufferedReader brUci = new BufferedReader(new FileReader(uciCsv));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPath))) {

                // 헤더 정의: UCI 원천 속성에 6개의 확장 분석 컬럼을 결합
                String uciHeader = brUci.readLine();
                String extraHeader = ",total_amount,tx_count,avg_installments,authorized_ratio,BALANCE,PURCHASES";

                bw.write(uciHeader + extraHeader);
                bw.newLine();

                String line;
                int count = 0;
                while ((line = brUci.readLine()) != null) {
                    // 데이터셋 간의 크기 차이를 극복하기 위한 순차 순환 매칭 (Modulo Matching)
                    String advPart = advancedList.get(count % advancedList.size());
                    String ccPart = ccList.get(count % ccList.size());

                    // V2 규격에 맞춘 최종 라인 결합 및 쓰기
                    bw.write(line + "," + advPart + "," + ccPart);
                    bw.newLine();
                    count++;
                }

                log.info("==== V2 데이터 통합 프로세스 완료 ====");
                log.info("최종 결과물 저장 경로: {}", targetPath);
                log.info("생성된 총 데이터 레코드 건수: {}건", count);
            }
        } catch (IOException e) {
            log.error("V2 데이터 통합 작업 중 치명적인 파일 입출력 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    /**
     * CSV 파일로부터 분석 목적에 부합하는 특정 필드들을 추출하여 리스트로 반환합니다.
     * <p>
     * 타입 4: Advanced 통계 지표 (총액, 거래수, 할부, 승인율) 추출 <br>
     * 타입 2: CC General 성향 지표 (잔액, 구매액) 추출
     * </p>
     * @param path 분석 대상 CSV 파일 경로
     * @param type 피처 추출 규격 구분값
     * @return 쉼표로 구분된 피처 문자열 리스트
     * @throws IOException 파일 읽기 중 오류 발생 시
     */
    private static List<String> loadList(String path, int type) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 데이터 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                // 데이터 정합성을 위한 인덱스 기반 추출 로직
                if (type == 4) {
                    // total_amount, tx_count, avg_installments, authorized_ratio 필드 매핑
                    list.add(String.format("%s,%s,%s,%s", v[1], v[2], v[3], v[6]));
                } else {
                    // BALANCE, PURCHASES 필드 매핑
                    list.add(String.format("%s,%s", v[1], v[3]));
                }
            }
        }
        return list;
    }
}