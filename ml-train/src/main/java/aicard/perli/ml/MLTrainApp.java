package aicard.perli.ml;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;

/**
 * 학습용 피처 생성 프로세스를 관리하고 실행하는 메인 엔트리 포인트입니다.
 * <p>Raw 데이터를 읽어 정제된 피처셋(Advanced Features)을 생성하고 이를 물리 파일로 저장합니다.</p>
 */
public class MLTrainApp {

    /**
     * 데이터 가공 파이프라인을 실행합니다.
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        FinEventParser parser = new FinEventParser();
        CsvDataLoader loader = new CsvDataLoader(parser);

        // 로컬 환경 절대 경로
        String root = "C:/Coding/perli-ai/resources/";
        String train = root + "raw/train.csv";
        String history = root + "raw/historical_transactions.csv";
        String output = root + "processed/train_features_advanced.csv";

        System.out.println("========== 엔지니어링 시작 ==========");

        try {
            long start = System.currentTimeMillis();

            // 공용 모델 기반의 집계 및 저장 실행
            loader.aggregateAndSave(train, history, output);

            long end = System.currentTimeMillis();
            System.out.println("가공 완료 (소요 시간: " + (end - start) / 1000.0 + "초)");
            System.out.println("생성된 파일: " + output);

        } catch (Exception e) {
            System.err.println("실행 도중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}