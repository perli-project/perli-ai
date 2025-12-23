package aicard.perli.ml.tribuo.service;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;
import aicard.perli.ml.tribuo.dto.request.CardRequest;
import aicard.perli.ml.tribuo.util.RankingDataConverter;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.slm.SLMTrainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Tribuo의 Sparse Linear Model(SLM)을 사용하여 카드 랭킹 예측 모델을 학습시키는 서비스 클래스입니다.</p>
 *
 * <p>이 서비스는 common 모듈의 {@link CsvDataLoader}를 통해 전처리된 데이터를 확보하고,
 * 이를 Tribuo 학습 규격으로 변환하여 최종적으로 .gdpc 모델 파일을 생성합니다.</p>
 *
 */
public class RankingTrainService {

    /** Tribuo 데이터셋 변환 유틸리티 */
    private final RankingDataConverter converter = new RankingDataConverter();

    /**
     * <p>카드 랭킹 모델 학습의 전 과정을 실행합니다.</p>
     * <pre>
     * 수행 단계:
     * 1. Raw 데이터 집계 및 중간 가공 파일(CSV) 생성
     * 2. 가공 파일 로드 및 CardRankingRequest DTO 변환
     * 3. Tribuo Dataset 생성
     * 4. SLMTrainer를 이용한 모델 학습
     * 5. 학습 완료된 모델 객체 파일 저장
     * </pre>
     *
     * @throws Exception 데이터 로딩 실패, 학습 오류 또는 파일 입출력 예외 발생 시
     */
    public void executeTrain() throws Exception {

        // FinEventParser를 통해 데이터 정합성을 체크하며 가공합니다.
        FinEventParser parser = new FinEventParser();
        CsvDataLoader loader = new CsvDataLoader(parser);

        String processedPath = "resources/processed/ranking_features.csv";
        loader.aggregateAndSave("resources/raw/train.csv", "resources/raw/historical_transactions.csv", processedPath);
        System.out.println("데이터 집계 및 가공 CSV 생성 완료");

        List<CardRequest> dtoList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(processedPath, StandardCharsets.UTF_8))) {
            br.readLine(); // CSV 헤더 행 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                // CsvDataLoader 저장 순서: 0:card_id, 1:total_sum, 2:count, 6:auth_ratio, 5:avg_amt, 7:target
                dtoList.add(new CardRequest(
                        s[0],
                        Double.parseDouble(s[1]),
                        Double.parseDouble(s[2]),
                        Double.parseDouble(s[6]),
                        Double.parseDouble(s[5]),
                        Double.parseDouble(s[7])
                ));
            }
        }
        System.out.println("가공 데이터 로드 완료: " + dtoList.size() + " 건");

        // Tribuo 전용 Dataset 객체로 변환
        Dataset<Regressor> dataset = converter.convertToDataset(dtoList);
        System.out.println("Tribuo 데이터셋 변환 완료");

        // 모델 학습 수행
        SLMTrainer trainer = new SLMTrainer(true, -1);

        System.out.println("SLMTrainer 모델 학습 시작...");
        Model<Regressor> model = trainer.train(dataset);

        // 학습 결과 모델 저장
        saveModel(model, "card_ranking_model.gdpc");
    }

    /**
     * 학습이 완료된 Tribuo 모델 객체를 지정된 경로에 직렬화하여 저장합니다.
     *
     * @param model 학습된 모델 객체
     * @param name  저장할 파일명 (확장자 포함)
     * @throws IOException 파일 쓰기 실패 시 발생
     */
    private void saveModel(Model<Regressor> model, String name) throws IOException {
        File f = new File("resources/output/models/" + name);
        // 디렉토리가 없을 경우 생성
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(model);
        }
        System.out.println("💾 [5/5] 모델 저장 완료: " + f.getAbsolutePath());
    }
}