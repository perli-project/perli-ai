package aicard.perli.common.data.uplift;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * <p>업리프트 모델링(Uplift Modeling)을 위한 학습 데이터 확장 유틸리티 클래스입니다.</p>
 *
 * <p>기본 피처 데이터셋에 처치 변수(Treatment Variable)인 {@code is_recommended} 컬럼을 추가합니다.
 * 이 과정은 S-Learner 알고리즘 구현을 위해 필수적이며, 모델이 추천 여부에 따른 고객 로열티의 변화를
 * 학습할 수 있는 기반을 마련합니다.</p>
 */
public class DataFeatureExpander {

    /**
     * 메인 실행 메서드입니다. 기존의 전처리된 CSV 파일을 읽어 'is_recommended' 피처가 추가된
     * 새로운 업리프트 전용 CSV 파일을 생성합니다.
     *
     * @param args 실행 인자 (사용되지 않음)
     */
    public static void main(String[] args) {
        // 경로 설정 (원본 데이터 및 타겟 디렉토리)
        String sourcePathStr = "C:/Coding/perli-ai/resources/processed/train_features_advanced.csv";
        String targetDirStr = "C:/Coding/perli-ai/resources/processed/uplift";
        String targetPathStr = targetDirStr + "/train_features_uplift.csv";

        try {
            // 디렉토리가 존재하지 않을 경우 자동으로 생성 (NIO 활용)
            Path targetDir = Paths.get(targetDirStr);
            if (Files.notExists(targetDir)) {
                Files.createDirectories(targetDir);
                System.out.println("업리프트 전용 폴더 생성 완료: " + targetDirStr);
            }

            // 파일 스트림 처리 (Try-with-resources로 리소스 자동 해제)
            try (BufferedReader br = new BufferedReader(new FileReader(sourcePathStr));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(targetPathStr))) {

                // 헤더 처리: 기존 헤더 끝에 'is_recommended' 피처명 추가
                String line = br.readLine();
                if (line != null) {
                    bw.write(line + ",is_recommended");
                    bw.newLine();
                }

                // 데이터 처리: 각 로우마다 랜덤하게 0(Control) 또는 1(Treatment) 값을 부여
                Random rand = new Random();
                int count = 0;
                while ((line = br.readLine()) != null) {
                    // S-Learner 학습의 기초가 되는 이진 노출(Binary Exposure) 변수 생성
                    int recommended = rand.nextInt(2);
                    bw.write(line + "," + recommended);
                    bw.newLine();
                    count++;
                }
                System.out.println("업리프트 학습용 데이터 생성 완료!");
                System.out.println("생성 경로: " + targetPathStr);
                System.out.println("총 데이터 수: " + count + " 건");
            }

        } catch (IOException e) {
            System.err.println("데이터 생성 중 입출력 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}