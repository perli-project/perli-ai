package aicard.perli.ml.tribuo.service.v1;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;
import aicard.perli.ml.tribuo.dto.request.v1.CardRequestV1;
import aicard.perli.ml.tribuo.util.v1.TribuoDataConverterV1;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.slm.SLMTrainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Tribuo Sparse Linear Model(SLM) 기반 카드 랭킹 학습 서비스입니다.</p>
 * <p>CSV 데이터를 로드하여 전처리하고, Tribuo 규격으로 변환 후 모델을 학습/저장합니다.</p>
 */
@Slf4j
public class TribuoTrainServiceV1 {

    /** Tribuo 데이터셋 변환 유틸리티 */
    private final TribuoDataConverterV1 converter = new TribuoDataConverterV1();

    /**
     * <p>V1 카드 랭킹 모델 학습 파이프라인을 실행합니다.</p>
     * @throws Exception 학습 공정 중 발생하는 I/O 및 Tribuo 내부 예외
     */
    public void executeTrain() throws Exception {
        log.info("카드 랭킹 학습 파이프라인 개시");

        // 데이터 집계 및 전처리
        FinEventParser parser = new FinEventParser();
        CsvDataLoader loader = new CsvDataLoader(parser);

        String processedPath = "resources/processed/ranking_features.csv";
        log.info("데이터 집계 및 가공 시작 (Path: {})", processedPath);

        loader.aggregateAndSave("resources/raw/train.csv", "resources/raw/historical_transactions.csv", processedPath);
        log.info("데이터 집계 및 가공 CSV 생성 완료");

        // 가공 데이터 로드 (DTO 변환)
        List<CardRequestV1> dtoList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(processedPath, StandardCharsets.UTF_8))) {
            br.readLine(); // 헤더 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                // 0:card_id, 1:total_sum, 2:count, 6:auth_ratio, 5:avg_amt, 7:target
                dtoList.add(new CardRequestV1(
                        s[0],
                        Double.parseDouble(s[1]),
                        Double.parseDouble(s[2]),
                        Double.parseDouble(s[6]),
                        Double.parseDouble(s[5]),
                        Double.parseDouble(s[7])
                ));
            }
        }
        log.info("가공 데이터 DTO 변환 완료 (총 {} 건)", dtoList.size());

        // Tribuo Dataset 변환
        log.info("Tribuo Dataset 변환 중");
        Dataset<Regressor> dataset = converter.convertToDataset(dtoList);
        log.info("Tribuo 데이터셋 변환 완료");

        // 모델 학습 (SLM 알고리즘)
        log.info("SLMTrainer 알고리즘 학습 시작");
        SLMTrainer trainer = new SLMTrainer(true, -1);
        Model<Regressor> model = trainer.train(dataset);
        log.info("모델 학습 완료");

        // 결과 저장
        saveModel(model, "tribuo_ranking_v1.gdpc");
    }

    /**
     * 학습된 모델을 지정된 경로에 직렬화하여 저장합니다.
     *
     * @param model 학습된 모델 객체
     * @param name  파일명
     * @throws IOException 파일 쓰기 예외
     */
    private void saveModel(Model<Regressor> model, String name) throws IOException {
        File f = new File("resources/output/models/tribuo/v1/" + name);

        if (f.getParentFile() != null && !f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
            log.info("모델 저장 폴더 생성: {}", f.getParentFile().getAbsolutePath());
        }

        log.info("모델 파일 직렬화 중");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(model);
        }
        log.info("모델 저장 최종 성공: {}", f.getAbsolutePath());
    }
}