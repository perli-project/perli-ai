package aicard.perli.ml.tribuo.service.v1;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;
import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
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
 * Tribuo Sparse Linear Model(SLM) 기반 카드 랭킹 학습 서비스 클래스입니다.
 * <p>
 * 원천 CSV 데이터를 로드하여 금융 도메인 지식에 기반한 전처리를 수행하고,
 * Tribuo 전용 데이터셋 규격으로 변환 후 고해상도 회귀 모델을 학습 및 저장하는 역할을 담당합니다.
 * </p>
 */
@Slf4j
public class TribuoTrainServiceV1 {

    /** Tribuo 데이터셋 변환 및 피처 맵 구축 유틸리티 */
    private final TribuoDataConverterV1 converter = new TribuoDataConverterV1();

    /**
     * V1 카드 랭킹 모델 학습 파이프라인을 실행합니다.
     * <p>
     * {@link FinEventParser}를 통한 결제 데이터 집계 <br>
     * 가공된 피처 기반의 DTO 리스트 생성 <br>
     * {@link SLMTrainer}를 활용한 선형 회귀 모델 빌드 <br>
     * 최종 모델 파일(.gdpc) 직렬화 저장 <br>
     * 순으로 공정이 진행됩니다.
     * </p>
     * @throws Exception 학습 공정 중 발생하는 I/O 및 Tribuo 내부 최적화 예외
     */
    public void executeTrain() throws Exception {
        log.info("카드 랭킹 학습 파이프라인 개시");

        // 데이터 집계 및 전처리 (Aggregation)
        FinEventParser parser = new FinEventParser();
        CsvDataLoader loader = new CsvDataLoader(parser);

        String processedPath = "resources/processed/ranking_features.csv";
        log.info("데이터 집계 및 가공 시작 (Path: {})", processedPath);

        // Raw 데이터(기본 정보 + 거래 내역)를 통합하여 분석용 피처셋 생성
        loader.aggregateAndSave("resources/raw/train.csv", "resources/raw/historical_transactions.csv", processedPath);
        log.info("데이터 집계 및 가공 CSV 생성 완료");

        // 가공 데이터 로드 및 DTO 매핑
        List<TribuoRequestV1> dtoList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(processedPath, StandardCharsets.UTF_8))) {
            br.readLine(); // 헤더 행 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                // 인덱스 맵핑: 0:card_id, 1:total_sum, 2:count, 6:auth_ratio, 5:avg_amt, 7:target(정답)
                dtoList.add(new TribuoRequestV1(
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

        // Tribuo 전용 Dataset 변환 (Feature Map 구성)
        log.info("Tribuo Dataset 변환 중");
        Dataset<Regressor> dataset = converter.convertToDataset(dtoList);
        log.info("Tribuo 데이터셋 변환 완료");

        // SLMTrainer: 희소 데이터를 효과적으로 처리하는 선형 모델 최적화 도구
        log.info("SLMTrainer 알고리즘 학습 시작");
        SLMTrainer trainer = new SLMTrainer(true, -1);
        Model<Regressor> model = trainer.train(dataset);
        log.info("모델 학습 완료");

        // 결과 저장 (Serialization)
        saveModel(model, "tribuo_ranking_v1.gdpc");
    }

    /**
     * 학습이 완료된 모델 객체를 지정된 경로에 바이너리 파일로 직렬화하여 저장합니다.
     * <p>
     * 저장된 파일은 추후 {@link ObjectInputStream}을 통해 로드되어 실시간 추천 서비스에서 재사용됩니다.
     * </p>
     * @param model 학습된 Tribuo 모델 객체
     * @param name  저장할 모델 파일명
     * @throws IOException 파일 쓰기 권한 또는 경로 미존재 시 예외
     */
    private void saveModel(Model<Regressor> model, String name) throws IOException {
        File f = new File("resources/output/models/tribuo/v1/" + name);

        // 모델 저장 폴더 구조가 없을 경우 자동 생성
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