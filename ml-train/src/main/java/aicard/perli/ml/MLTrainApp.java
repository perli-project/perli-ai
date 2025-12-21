package aicard.perli.ml;

import aicard.perli.common.data.loader.CsvDataLoader;
import aicard.perli.common.data.parser.FinEventParser;
import aicard.perli.ml.service.MLTrainService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * AI 카드 로열티 예측 시스템의 통합 학습 파이프라인 엔트리 포인트입니다.
 * <p>
 * 본 클래스는 크게 두 단계의 프로세스를 관리합니다:
 * 1. 데이터 엔지니어링: Raw 데이터를 학습 가능한 형태의 피처셋으로 가공 (중복 가공 방지 로직 포함)
 * 2. AI 모델링: H2O 엔진을 활용한 GBM(Gradient Boosting Machine) 모델 학습 및 저장
 * </p>
 *
 * @version 1.1
 */
@Slf4j
public class MLTrainApp {

    /**
     * 메인 실행 메서드로 전처리 및 학습 파이프라인을 구동합니다.
     * * @param args 실행 인자 (현재 사용되지 않음)
     */
    public static void main(String[] args) {
        // 1. 핵심 서비스 인스턴스 초기화
        FinEventParser parser = new FinEventParser();
        CsvDataLoader loader = new CsvDataLoader(parser);
        MLTrainService trainService = new MLTrainService();

        // 프로젝트 물리 경로 정의
        String root = "C:/Coding/perli-ai/resources/";
        String trainRaw = root + "raw/train.csv";
        String historyRaw = root + "raw/historical_transactions.csv";
        String processedCsv = root + "processed/train_features_advanced.csv";

        log.info("===========================================================");
        log.info("예측 모델 통합 파이프라인을 가동합니다.");
        log.info("===========================================================");

        try {
            long startTime = System.currentTimeMillis();

            /*
             * [Step 1] 데이터 엔지니어링 및 가공 데이터 존재 여부 확인
             * 대용량 원본 데이터를 매번 집계하는 리소스를 절약하기 위해
             * 기존에 생성된 가공 파일(processedCsv)이 존재할 경우 전처리 단계를 생략합니다.
             */
            File processedFile = new File(processedCsv);
            if (processedFile.exists() && processedFile.length() > 0) {
                log.info("기존 가공 데이터를 발견했습니다. (Path: {})", processedCsv);
                log.info("데이터 최신화를 원할 경우 해당 파일을 삭제 후 재실행하십시오. 전처리를 건너뜁니다.");
            } else {
                log.info("공 데이터가 존재하지 않습니다. 대용량 전처리를 시작합니다");
                loader.aggregateAndSave(trainRaw, historyRaw, processedCsv);
                log.info("전처리 및 피처 추출 완료: {}", processedCsv);
            }

            /*
             * [Step 2] AI 모델 학습 단계
             * 가공된 피처셋을 H2O 엔진에 로드하여 GBM 알고리즘을 통한 학습을 수행합니다.
             * 학습 완료 시 모델은 'resources/output/models' 경로에 바이너리 형태로 저장됩니다.
             */
            log.info("머신러닝 모델 학습 및 최적화 단계를 시작합니다.");
            trainService.train(processedCsv);

            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            log.info("===========================================================");
            log.info("전체 파이프라인이 성공적으로 종료되었습니다. (총 소요 시간: {}s)", duration);
            log.info("===========================================================");

        } catch (Exception e) {
            log.error("파이프라인 실행 중 치명적인 시스템 오류가 발생했습니다.");
            log.error("상세 에러 메시지: {}", e.getMessage(), e);
        }
    }
}