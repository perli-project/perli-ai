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
 * <p>V1용 Gradient Boosting Machine(GBM) 모델 학습 및 MOJO 생성 서비스입니다.</p>
 * <p>과거 거래 내역 기반의 뼈대 피처셋을 학습하여 업리프트 시뮬레이션을 위한 기초 모델을 생성합니다.
 * H2O 엔진의 내장 MOJO 내보내기 기능 결함을 우회하기 위해 수동 ZIP 아카이브 조립 방식을 사용합니다.</p>
 *
 */
@Slf4j
public class H2oTrainServiceV1 {

    /**
     * <p>지정된 경로의 CSV 데이터를 로드하여 V1 모델 학습을 수행합니다.</p>
     *
     * @param dataPath 학습용 CSV 파일의 물리적 경로 (train_features_advanced.csv)
     */
    public void train(String dataPath) {
        // JDK 버전 체크 우회 및 로그 환경 설정
        System.setProperty("h2o.ignore.jdk.version", "true");
        String logDirPath = new File("C:/Coding/perli-ai/resources/output/logs").getAbsolutePath();
        H2O.main(new String[]{"-log_dir", logDirPath});

        try {
            Scope.enter();
            File f = new File(dataPath);
            NFSFileVec nfs = NFSFileVec.make(f);
            Frame fr = ParseDataset.parse(Key.make("train_frame_v1"), nfs._key);

            // GBM 알고리즘 설정 (뼈대 학습용)
            GBMParameters params = new GBMParameters();
            params._train = fr._key;
            params._response_column = "target";
            params._ignored_columns = new String[]{"card_id"}; // 학습 제외 식별자
            params._ntrees = 100;
            params._max_depth = 10;
            params._seed = 1234L;
            params._learn_rate = 0.01;

            log.info("AI 모델 학습 프로세스 시작...");
            GBM job = new GBM(params);
            GBMModel model = job.trainModel().get();
            log.info("학습 완료. MOJO 수동 조립을 시작합니다.");

            forceManualMojoAssembly(model);

        } catch (Exception e) {
            log.error("학습 중 예외 발생: {}", e.getMessage());
        } finally {
            Scope.exit();
        }
    }

    /**
     * <p>H2O SchemaServer 연결 오류를 우회하여 MOJO ZIP 파일을 수동으로 생성합니다.</p>
     * <p>Reflection을 통해 내부 writer의 스트림과 경로를 강제 제어합니다.</p>
     *
     * @param model 학습이 완료된 GBM 모델 객체
     */
    private void forceManualMojoAssembly(GBMModel model) {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/uplift/v1/uplift_gbm_model_v1.zip";
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

                // 내부 스트림에 현재 ZipOutputStream 주입
                Field zosField = findField(mojoWriter.getClass(), "zos");
                zosField.setAccessible(true);
                zosField.set(mojoWriter, zos);

                // MOJO 구성 요소 순차 기록
                invokeMethod(mojoWriter, "addCommonModelInfo");
                invokeMethod(mojoWriter, "writeModelData");
                invokeMethod(mojoWriter, "writeModelInfo");
                invokeMethod(mojoWriter, "writeDomains");

                zos.finish();
            }
            log.info("MOJO 파일 생성 성공: {}", modelPath);

        } catch (Exception e) {
            log.error("MOJO 조립 실패: {}", e.getMessage());
        }
    }

    /**
     * 클래스 상속 계층을 순회하며 필드를 탐색합니다.
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
     * 비공개 또는 보호된 메서드를 강제 실행합니다.
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