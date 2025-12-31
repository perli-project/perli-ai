package aicard.perli.ml.h2o.util.v1;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * 업리프트 모델링(Uplift Modeling)을 위한 학습 데이터 확장 유틸리티 클래스입니다.
 * <p>
 * 기본 피처 데이터셋에 처치 변수(Treatment Variable)인 {@code is_recommended} 컬럼을 추가합니다.
 * 이 과정은 S-Learner 알고리즘 구현을 위해 필수적이며, 모델이 추천 여부에 따른 고객 로열티의 변화를
 * 학습할 수 있는 기반을 마련합니다.
 * </p>
 */
@Slf4j
public class H2oDataGeneratorV1 {

    /**
     * 데이터 통합 및 확장 프로세스를 실행하는 메인 메서드입니다.
     * <p>
     * 기존의 전처리된 CSV 파일을 읽어 'is_recommended' 피처가 추가된 새로운 업리프트 전용 CSV 파일을 생성합니다.
     * 각 레코드에 대해 무작위로 처치군(1)과 대조군(0)을 할당함으로써 무작위 대조군 실험(RCT)과 유사한 데이터 구조를 형성합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        // 경로 설정 (원본 데이터 및 타겟 디렉토리)
        String sourcePathStr = "C:/Coding/perli-ai/resources/processed/train_features_advanced.csv";
        String targetDirStr = "C:/Coding/perli-ai/resources/processed/uplift";
        String targetPathStr = targetDirStr + "/train_uplift_v1.csv";

        try {
            // 디렉토리가 존재하지 않을 경우 자동으로 생성 (NIO 활용)
            Path targetDir = Paths.get(targetDirStr);
            if (Files.notExists(targetDir)) {
                Files.createDirectories(targetDir);
                log.info("업리프트 전용 폴더 생성 완료: " + targetDirStr);
            }

            // 파일 스트림 처리 (Try-with-resources로 리소스 자동 해제)
            try (BufferedReader br = new BufferedReader(new FileReader(sourcePathStr));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPathStr))) {

                // 기존 피처 명세 끝에 'is_recommended' 처치 변수명 추가
                String line = br.readLine();
                if (line != null) {
                    bw.write(line + ",is_recommended");
                    bw.newLine();
                }

                // 무작위 할당(Random Assignment) 로직 수행
                Random rand = new Random();
                int count = 0;
                while ((line = br.readLine()) != null) {
                    // S-Learner 학습의 기초가 되는 이진 노출(Binary Exposure) 변수 생성
                    // 0: Control Group (추천 미노출), 1: Treatment Group (추천 노출)
                    int recommended = rand.nextInt(2);
                    bw.write(line + "," + recommended);
                    bw.newLine();
                    count++;
                }
                log.info("업리프트 학습용 데이터 생성 완료");
                log.info("생성 경로: " + targetPathStr);
                log.info("총 데이터 수: " + count + " 건");
            }

        } catch (IOException e) {
            log.error("데이터 생성 중 입출력 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}