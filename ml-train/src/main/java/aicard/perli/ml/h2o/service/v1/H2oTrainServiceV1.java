package aicard.perli.ml.h2o.service.v1;

import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import hex.tree.gbm.GBMModel.GBMParameters;
import lombok.extern.slf4j.Slf4j;
import water.H2O;
import water.Key;
import water.Scope;
import water.fvec.Frame;
import water.fvec.NFSFileVec;
import water.parser.ParseDataset;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipOutputStream;

/**
 * V1용 Gradient Boosting Machine(GBM) 모델 학습 및 MOJO 생성 서비스입니다.
 * <p>
 * 과거 거래 내역 기반의 뼈대 피처셋을 학습하여 업리프트 시뮬레이션을 위한 기초 모델을 생성합니다.
 * H2O 엔진의 내장 MOJO 내보내기 기능(SchemaServer 연결) 결함을 우회하기 위해
 * Reflection을 이용한 수동 ZIP 아카이브 조립 방식을 채택하였습니다.
 * </p>
 */
@Slf4j
public class H2oTrainServiceV1 {

    /**
     * 지정된 경로의 CSV 데이터를 로드하여 V1 모델 학습을 수행합니다.
     * <p>
     * H2O 클러스터를 초기화하고, NFS(Network File System) 방식으로 데이터를 파싱한 뒤
     * GBM 알고리즘을 가동합니다. 학습 완료 후 즉시 MOJO 직렬화 프로세스를 호출합니다.
     * </p>
     * @param dataPath 학습용 CSV 파일의 물리적 경로 (train_features_advanced.csv)
     */
    public void train(String dataPath) {
        // JDK 버전 체크 우회 및 로그 환경 설정 (H2O의 환경 제약 극복)
        System.setProperty("h2o.ignore.jdk.version", "true");
        String logDirPath = new File("C:/Coding/perli-ai/resources/output/logs").getAbsolutePath();
        H2O.main(new String[]{"-log_dir", logDirPath});

        try {
            Scope.enter(); // H2O 메모리 자원 관리 시작
            File f = new File(dataPath);
            NFSFileVec nfs = NFSFileVec.make(f);
            Frame fr = ParseDataset.parse(Key.make("train_frame_v1"), nfs._key);

            // 뼈대 학습용 하이퍼파라미터 정의
            GBMParameters params = new GBMParameters();
            params._train = fr._key;
            params._response_column = "target"; // 타겟 변수 설정 (로열티 또는 지출액)
            params._ignored_columns = new String[]{"card_id"}; // 학습 제외 식별자
            params._ntrees = 100;    // 결정 트리 개수
            params._max_depth = 10;  // 트리의 최대 깊이
            params._seed = 1234L;    // 결과 재현을 위한 시드값
            params._learn_rate = 0.01; // 학습률

            log.info("AI 모델 학습 프로세스 시작...");
            GBM job = new GBM(params);
            GBMModel model = job.trainModel().get(); // 동기 방식으로 학습 완료 대기
            log.info("학습 완료. MOJO 수동 조립을 시작합니다.");

            // 학습된 모델을 물리 파일로 변환
            forceManualMojoAssembly(model);

        } catch (Exception e) {
            log.error("학습 중 예외 발생: {}", e.getMessage());
        } finally {
            Scope.exit(); // 사용된 메모리 자원 해제
        }
    }

    /**
     * H2O SchemaServer 연결 오류를 우회하여 MOJO ZIP 파일을 수동으로 생성합니다.
     * <p>
     * Reflection 기술을 사용하여 MojoWriter 내부의 'targetdir'과 'zos(ZipOutputStream)' 필드에 직접 접근합니다.
     * 이는 서버-클라이언트 통신 없이 JVM 내부 메모리에서 직접 모델 데이터를 ZIP으로 패키징하는 저수준 작업입니다.
     * </p>
     * @param model 학습이 완료된 GBM 모델 객체
     */
    private void forceManualMojoAssembly(GBMModel model) {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/h2o/v1/uplift_gbm_model_v1.zip";
        try {
            File modelFile = new File(modelPath);
            if (modelFile.getParentFile() != null) modelFile.getParentFile().mkdirs();

            Object mojoWriter = model.getMojo();
            log.info("리플렉션 기반 MOJO 조립 중");

            try (FileOutputStream fos = new FileOutputStream(modelFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 파일 경로 접두사 null 방지 처리를 위한 targetdir 수정
                Field dirField = findField(mojoWriter.getClass(), "targetdir");
                dirField.setAccessible(true);
                dirField.set(mojoWriter, "");

                // 내부 스트림에 현재 생성한 ZipOutputStream을 강제로 주입
                Field zosField = findField(mojoWriter.getClass(), "zos");
                zosField.setAccessible(true);
                zosField.set(mojoWriter, zos);

                // 내부 비공개 메서드를 순차 실행
                invokeMethod(mojoWriter, "addCommonModelInfo"); // 모델 공통 정보
                invokeMethod(mojoWriter, "writeModelData");      // 트리 구조 데이터
                invokeMethod(mojoWriter, "writeModelInfo");      // 모델 메타데이터
                invokeMethod(mojoWriter, "writeDomains");        // 범주형 데이터 도메인

                zos.finish();
            }
            log.info("MOJO 파일 생성 성공: {}", modelPath);

        } catch (Exception e) {
            log.error("MOJO 조립 실패: {}", e.getMessage());
        }
    }

    /**
     * 클래스 상속 계층을 순회하며 비공개 필드를 탐색합니다.
     * <p>H2O 모델 객체의 복잡한 상속 구조에서 특정 필드를 안전하게 확보합니다.</p>
     */
    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * 비공개 또는 보호된 내부 메서드를 강제 실행합니다.
     * <p>접근 제어자를 무력화하여 MOJO 기록 프로세스를 수동으로 제어합니다.</p>
     */
    private void invokeMethod(Object obj, String methodName) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Method m = clazz.getDeclaredMethod(methodName);
                m.setAccessible(true);
                m.invoke(obj);
                return;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}